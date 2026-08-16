package io.github.ramiro.escapesj.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final int LATEST_DB_VERSION = 2;


    private static final String DB_FILENAME = "escapesj.db";
    private static boolean inicializado = false;

    private static String customDbUrl = null;

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

    /**
     * Crea las tablas si no existen.
     * Esquema completo con todas las columnas que usan los repositorios.
     */
    private static void inicializarTablas(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {

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
            // Migración para bases de datos existentes
            try {
                stmt.execute("ALTER TABLE usuarios ADD COLUMN pregunta_seguridad VARCHAR(255)");
                stmt.execute("ALTER TABLE usuarios ADD COLUMN respuesta_seguridad VARCHAR(100)");
            } catch (Exception e) {
                // Si la columna ya existe, SQLite tira un error que podemos ignorar en esta migración
            }
            try {
                stmt.execute("ALTER TABLE usuarios ADD COLUMN debe_cambiar_password INTEGER DEFAULT 1");
            } catch (Exception e) {
                // Ignorar si la columna ya existe
            }

            // Tabla Configuración (AFIP token, URL, etc.)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS configuracion (
                    clave VARCHAR(100) PRIMARY KEY,
                    valor TEXT NOT NULL
                )
            """);

            // Tabla Cache AFIP (proxy de cache para evitar consultas repetidas)
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

            // --- Migración de versión de base de datos ---
            int currentVersion = 0;
            try (java.sql.ResultSet rsVer = stmt.executeQuery("SELECT valor FROM configuracion WHERE clave = 'db_version'")) {
                if (rsVer.next()) {
                    currentVersion = Integer.parseInt(rsVer.getString("valor"));
                }
            } catch (Exception e) {
                // Si falla (ej. tabla configuración no estaba creada antes), la versión queda en 0
            }

            // Crear un backup solamente cuando realmente hay migraciones pendientes.
            if (currentVersion < LATEST_DB_VERSION && customDbUrl == null) {
                String backupPath = obtenerRutaDB() + ".backup-" + System.currentTimeMillis() + ".db";
                try (Statement stmtBackup = conn.createStatement()) {
                    stmtBackup.execute("VACUUM INTO '" + backupPath.replace("\\", "/") + "'");
                    logger.info("Backup creado exitosamente en: " + backupPath);
                } catch (Exception ex) {
                    logger.warn("No se pudo crear backup de seguridad antes de migrar. Abortando migración.", ex);
                    throw ex;
                }
            }

            conn.setAutoCommit(false);
            try {
                if (currentVersion < 1) {
                    logger.info("Iniciando migración de base de datos a versión 1 (Conversión a centavos)...");
                    // Como db_version < 1, asumimos que toda la base es legacy y multiplicamos incondicionalmente por 100.
                    stmt.execute("UPDATE boletas SET total = CAST(ROUND(total * 100) AS INTEGER)");
                    stmt.execute("UPDATE boleta_items SET precio_unitario = CAST(ROUND(precio_unitario * 100) AS INTEGER)");
                    stmt.execute("UPDATE boleta_items SET subtotal = CAST(ROUND(subtotal * 100) AS INTEGER)");
                    stmt.execute("UPDATE productos SET precio = CAST(ROUND(precio * 100) AS INTEGER)");
                    stmt.execute("UPDATE presupuestos SET monto_estimado = CAST(ROUND(monto_estimado * 100) AS INTEGER)");
                    
                    stmt.execute("INSERT OR REPLACE INTO configuracion (clave, valor) VALUES ('db_version', '1')");
                    logger.info("Migración a versión 1 exitosa.");
                }

                if (currentVersion < 2) {
                    logger.info("Iniciando migración de base de datos a versión 2 (Emisores)...");
                    stmt.execute("CREATE TABLE IF NOT EXISTS emisores (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "nombre TEXT NOT NULL, " +
                            "cuit TEXT NOT NULL, " +
                            "calle TEXT, " +
                            "telefono TEXT)");
                    stmt.execute("INSERT OR REPLACE INTO configuracion (clave, valor) VALUES ('db_version', '2')");
                    logger.info("Migración a versión 2 exitosa.");
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                logger.error("Error crítico durante la migración. Rollback ejecutado.", e);
                throw e; // Abort startup
            } finally {
                conn.setAutoCommit(true);
            }

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

}
