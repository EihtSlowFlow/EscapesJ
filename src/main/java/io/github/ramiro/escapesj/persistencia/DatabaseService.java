package io.github.ramiro.escapesj.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private static final String DB_FILENAME = "escapesj.db";
    private static boolean inicializado = false;

    private static String customDbUrl = null;

    public interface MigrationAction {
        void execute(Connection conn) throws Exception;
    }

    public record Migration(int version, String descripcion, MigrationAction action) {}

    public static final List<Migration> MIGRACIONES = List.of(
        new Migration(1, "Conversión a centavos", conn -> {
            int legacyVersion = obtenerLegacyDbVersion(conn);
            if (legacyVersion < 1) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("UPDATE boletas SET total = CAST(ROUND(total * 100) AS INTEGER)");
                    stmt.execute("UPDATE boleta_items SET precio_unitario = CAST(ROUND(precio_unitario * 100) AS INTEGER)");
                    stmt.execute("UPDATE boleta_items SET subtotal = CAST(ROUND(subtotal * 100) AS INTEGER)");
                    stmt.execute("UPDATE productos SET precio = CAST(ROUND(precio * 100) AS INTEGER)");
                    stmt.execute("UPDATE presupuestos SET monto_estimado = CAST(ROUND(monto_estimado * 100) AS INTEGER)");
                }
            }
        }),
        new Migration(2, "Emisores", conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS emisores (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "cuit TEXT NOT NULL, " +
                        "calle TEXT, " +
                        "telefono TEXT)");
            }
        }),
        new Migration(3, "Columnas seguridad en usuarios", conn -> {
            try (Statement stmt = conn.createStatement()) {
                if (!existeColumna(conn, "usuarios", "pregunta_seguridad")) {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN pregunta_seguridad VARCHAR(255)");
                }
                if (!existeColumna(conn, "usuarios", "respuesta_seguridad")) {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN respuesta_seguridad VARCHAR(100)");
                }
                if (!existeColumna(conn, "usuarios", "debe_cambiar_password")) {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN debe_cambiar_password INTEGER DEFAULT 1");
                }
            }
        }),
        new Migration(4, "Rentabilidad y operaciones historicas", conn -> {
            try (Statement stmt = conn.createStatement()) {
                if (!existeColumna(conn, "productos", "costo_unitario_centavos")) {
                    stmt.execute("ALTER TABLE productos ADD COLUMN costo_unitario_centavos INTEGER DEFAULT NULL CHECK (costo_unitario_centavos IS NULL OR costo_unitario_centavos >= 0)");
                }
                if (!existeColumna(conn, "boleta_items", "costo_unitario_historico_centavos")) {
                    stmt.execute("ALTER TABLE boleta_items ADD COLUMN costo_unitario_historico_centavos INTEGER DEFAULT NULL CHECK (costo_unitario_historico_centavos IS NULL OR costo_unitario_historico_centavos >= 0)");
                }
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS operaciones_historicas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        fecha TEXT NOT NULL,
                        referencia_papel TEXT,
                        cliente TEXT,
                        descripcion TEXT NOT NULL,
                        importe_total_centavos INTEGER NOT NULL CHECK (importe_total_centavos >= 0),
                        costo_materiales_centavos INTEGER CHECK (costo_materiales_centavos IS NULL OR costo_materiales_centavos >= 0),
                        observaciones TEXT,
                        estado TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'DIGITALIZADO')),
                        boleta_digital_id INTEGER,
                        creado_en TEXT NOT NULL,
                        actualizado_en TEXT NOT NULL,
                        FOREIGN KEY (boleta_digital_id) REFERENCES boletas(id) ON DELETE SET NULL
                    )
                """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_boletas_fecha ON boletas(fecha)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_operaciones_historicas_fecha_estado ON operaciones_historicas(fecha, estado)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_operacion_boleta_digital ON operaciones_historicas(boleta_digital_id) WHERE boleta_digital_id IS NOT NULL");
            }
        }),
        new Migration(5, "Costos en cero para servicios legacy", conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE boleta_items SET costo_unitario_historico_centavos = 0 WHERE tipo != 'PRODUCTO' AND costo_unitario_historico_centavos IS NULL");
            }
        })
    );

    /**
     * Permite inyectar una URL JDBC custom para tests (ej: jdbc:sqlite::memory:).
     */
    public static void setCustomDbUrl(String url) {
        customDbUrl = url;
    }

    /**
     * Inicializa la base de datos y crea las tablas si no existen.
     * Debe llamarse una sola vez al arrancar la aplicación.
     */
    public static synchronized void inicializar() throws Exception {
        if (inicializado) return;
        try (Connection conn = getConnection()) {
            inicializarTablas(conn);
        }
        inicializado = true;
    }

    // Para forzar el re-inicio en tests
    public static synchronized void reiniciarTest() {
        inicializado = false;
    }

    /**
     * Obtiene una nueva conexión a SQLite.
     * El archivo .db se crea junto al JAR ejecutable.
     */
    public static Connection getConnection() {
        try {
            Connection conn;
            if (customDbUrl != null) {
                conn = DriverManager.getConnection(customDbUrl);
            } else {
                String dbPath = obtenerRutaDB();
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        } catch (Exception e) {
            logger.error("Error al conectar con SQLite: " + e.getMessage());
            throw new PersistenceException("Error al conectar con la base de datos", e);
        }
    }

    /**
     * Calcula la ruta del archivo .db relativa al directorio de ejecución.
     */
    private static String obtenerRutaDB() {
        // Intentar ubicar el .db junto al JAR
        try {
            File jarDir = new File(DatabaseService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            return new File(jarDir, DB_FILENAME).getAbsolutePath();
        } catch (Exception e) {
            // Fallback: directorio de trabajo actual
            return DB_FILENAME;
        }
    }

    public static boolean existeColumna(Connection conn, String tabla, String columna) throws Exception {
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int obtenerLegacyDbVersion(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT valor FROM configuracion WHERE clave = ?")) {
            stmt.setString(1, "db_version");
            try (java.sql.ResultSet rsVer = stmt.executeQuery()) {
                if (!rsVer.next()) {
                    return 0;
                }

                String valor = rsVer.getString("valor");
                try {
                    return Integer.parseInt(valor);
                } catch (NumberFormatException e) {
                    throw new PersistenceException("Valor inválido para db_version: " + valor, e);
                }
            }
        } catch (PersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenceException("Error al consultar db_version legacy", e);
        }
    }

    /**
     * Crea las tablas si no existen.
     * Esquema completo con todas las columnas que usan los repositorios.
     */
    private static void inicializarTablas(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {

            // Tabla Control de Migraciones
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INTEGER PRIMARY KEY,
                    descripcion TEXT,
                    ejecutado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    ms_transcurridos INTEGER
                )
            """);

            // Tabla Productos (Inventario)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS productos (
                    codigo VARCHAR(50) PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    descripcion TEXT NOT NULL,
                    precio INTEGER NOT NULL,
                    stock INTEGER NOT NULL DEFAULT 0
                )
            """);

            // Tabla Historial de Servicios
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS servicios_historial (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dni VARCHAR(20) NOT NULL,
                    nombre VARCHAR(100),
                    trabajo TEXT NOT NULL,
                    fecha VARCHAR(20) NOT NULL
                )
            """);

            // Tabla Usuarios (Autenticación)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    pregunta_seguridad VARCHAR(255),
                    respuesta_seguridad VARCHAR(100),
                    debe_cambiar_password INTEGER DEFAULT 1
                )
            """);

            // Tabla Configuración (AFIP token, URL, etc.)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS configuracion (
                    clave VARCHAR(100) PRIMARY KEY,
                    valor TEXT NOT NULL
                )
            """);

            // Tabla Cache AFIP
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cache_afip (
                    dni VARCHAR(20) PRIMARY KEY,
                    nombre VARCHAR(200) NOT NULL,
                    cuit VARCHAR(20) NOT NULL,
                    prefijo_cuit VARCHAR(2) NOT NULL,
                    fecha_cache TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS boletas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero INTEGER NOT NULL UNIQUE,
                    dni VARCHAR(20) NOT NULL,
                    nombre_cliente VARCHAR(100),
                    fecha VARCHAR(20) NOT NULL,
                    total INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS boleta_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    boleta_id INTEGER NOT NULL,
                    tipo VARCHAR(10) NOT NULL,
                    descripcion TEXT NOT NULL,
                    codigo_producto VARCHAR(50),
                    cantidad INTEGER NOT NULL DEFAULT 1,
                    precio_unitario INTEGER NOT NULL DEFAULT 0,
                    subtotal INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (boleta_id) REFERENCES boletas(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS presupuestos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo_unico VARCHAR(20) NOT NULL UNIQUE,
                    dni_cliente VARCHAR(20) NOT NULL,
                    nombre_cliente VARCHAR(200),
                    descripcion_trabajo TEXT NOT NULL,
                    monto_estimado INTEGER NOT NULL DEFAULT 0,
                    fecha_emision VARCHAR(20) NOT NULL,
                    fecha_limite VARCHAR(20) NOT NULL
                )
            """);



            ejecutarMigraciones(conn, MIGRACIONES);

            // Seed: configuración AFIP por defecto si no existe
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM configuracion");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO configuracion (clave, valor) VALUES ('afip.access_token', '')");
                stmt.execute("INSERT INTO configuracion (clave, valor) VALUES ('afip.cuit', '20409378472')");
                stmt.execute("INSERT INTO configuracion (clave, valor) VALUES ('afip.production', 'false')");
            }

            // Seed: productos de ejemplo (taller de escapes) si la tabla está vacía
            rs = stmt.executeQuery("SELECT COUNT(*) FROM productos");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO productos (codigo, nombre, descripcion, precio, stock) VALUES
                    ('ESC-001', 'Silenciador Universal', 'Silenciador de acero inoxidable 2" universal', 4500000, 12),
                    ('ESC-002', 'Caño de Escape 2.5"', 'Caño recto acero aluminizado 2.5 pulgadas x metro', 1850000, 20),
                    ('ESC-003', 'Catalizador Universal', 'Catalizador deportivo 200 celdas 2"', 6200000, 5),
                    ('ESC-004', 'Abrazadera Escape 2"', 'Abrazadera acero inox. para unión de caños 2"', 350000, 50),
                    ('ESC-005', 'Flexible Escape', 'Flexible corrugado acero inox. 2" x 20cm', 1200000, 15),
                    ('ESC-006', 'Silenciador Deportivo', 'Silenciador deportivo doble salida acero inox.', 7800000, 4),
                    ('ESC-007', 'Junta de Escape', 'Junta grafitada para múltiple de escape', 280000, 30),
                    ('ESC-008', 'Sensor de O2 Universal', 'Sensor de oxígeno 4 cables universal', 2200000, 8),
                    ('ESC-009', 'Múltiple de Escape 4-1', 'Header 4 en 1 acero inoxidable', 9500000, 3),
                    ('ESC-010', 'Cola de Escape Cromada', 'Terminal de escape cromada ovalada', 850000, 18)
                """);
                logger.info("DB Seed: 10 productos de escape cargados.");
            }

            // Seed: historial de servicios de ejemplo si la tabla está vacía
            rs = stmt.executeQuery("SELECT COUNT(*) FROM servicios_historial");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO servicios_historial (dni, nombre, trabajo, fecha) VALUES
                    ('46000698', 'ARGEL RAMIRO LUIS YAEL', 'SERVICIO: Cambio de silenciador trasero', '2026-06-15'),
                    ('46000698', 'ARGEL RAMIRO LUIS YAEL', 'VENTA: Silenciador Universal (x1)', '2026-06-15'),
                    ('46000698', 'ARGEL RAMIRO LUIS YAEL', 'SERVICIO: Soldadura de flexible roto', '2026-07-01'),
                    ('47048195', 'GONZALEZ MARTIN', 'SERVICIO: Instalación escape completo', '2026-06-20'),
                    ('47048195', 'GONZALEZ MARTIN', 'VENTA: Caño de Escape 2.5" (x3)', '2026-06-20'),
                    ('30111222', 'LOPEZ CARLOS ALBERTO', 'SERVICIO: Reparación catalizador', '2026-07-10')
                """);
                logger.info("DB Seed: 6 registros de historial cargados.");
            }

        } catch (Exception e) {
            logger.error("Error crítico inicializando base de datos:", e);
            throw e;
        }
    }

    public static void ejecutarMigraciones(Connection conn, List<Migration> migraciones) throws Exception {
        List<Migration> migracionesOrdenadas = new ArrayList<>(migraciones);
        Set<Integer> versiones = new HashSet<>();
        for (Migration migracion : migracionesOrdenadas) {
            if (!versiones.add(migracion.version())) {
                throw new PersistenceException("Versión de migración duplicada: " + migracion.version());
            }
        }
        migracionesOrdenadas.sort(Comparator.comparingInt(Migration::version));

        for (Migration m : migracionesOrdenadas) {
            boolean alreadyApplied;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                ps.setInt(1, m.version());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    alreadyApplied = rs.next();
                }
            } catch (Exception e) {
                throw new PersistenceException(
                        "Error al consultar la migración aplicada versión " + m.version(), e);
            }

            if (alreadyApplied) continue;

            long start = System.currentTimeMillis();
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                m.action().execute(conn);

                long elapsed = System.currentTimeMillis() - start;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO schema_migrations (version, descripcion, ms_transcurridos) VALUES (?, ?, ?)")) {
                    ps.setInt(1, m.version());
                    ps.setString(2, m.descripcion());
                    ps.setLong(3, elapsed);
                    ps.executeUpdate();
                }
                conn.commit();
                logger.info("Migración a versión {} exitosa: {}", m.version(), m.descripcion());
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                logger.error("Error crítico durante la migración a versión {}. Rollback ejecutado.", m.version(), e);
                throw new PersistenceException("Error durante la migración a versión " + m.version(), e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (Exception ex) {
                    logger.error("Error restaurando autocommit tras migración.", ex);
                }
            }
        }
    }

}
