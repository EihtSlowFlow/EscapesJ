package io.github.ramiro.escapesj.persistencia;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseService {

    private static final String DB_FILENAME = "escapesj.db";
    private static Connection sharedConnection;

    /**
     * Obtiene la conexión compartida a SQLite.
     * El archivo .db se crea junto al JAR ejecutable.
     */
    public static synchronized Connection getConnection() {
        try {
            if (sharedConnection == null || sharedConnection.isClosed()) {
                String dbPath = obtenerRutaDB();
                sharedConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                inicializarTablas(sharedConnection);
            }
            return sharedConnection;
        } catch (Exception e) {
            System.err.println("Error al conectar con SQLite: " + e.getMessage());
            return null;
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
    private static void inicializarTablas(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // Tabla Productos (Inventario)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS productos (
                    codigo VARCHAR(50) PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    descripcion TEXT NOT NULL,
                    precio DECIMAL(10,2) NOT NULL,
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

            // Tabla Usuarios (Login configurable)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL
                )
            """);

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
                    total DECIMAL(10,2) NOT NULL DEFAULT 0
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
                    precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0,
                    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
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
                    monto_estimado DECIMAL(10,2) NOT NULL DEFAULT 0,
                    fecha_emision VARCHAR(20) NOT NULL,
                    fecha_limite VARCHAR(20) NOT NULL
                )
            """);

            // Seed: usuario admin por defecto si la tabla está vacía
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO usuarios (usuario, password) VALUES ('admin', '1234')");
            }

            // Seed: configuración AFIP por defecto si no existe
            rs = stmt.executeQuery("SELECT COUNT(*) FROM configuracion");
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
                    ('ESC-001', 'Silenciador Universal', 'Silenciador de acero inoxidable 2" universal', 45000.00, 12),
                    ('ESC-002', 'Caño de Escape 2.5"', 'Caño recto acero aluminizado 2.5 pulgadas x metro', 18500.00, 20),
                    ('ESC-003', 'Catalizador Universal', 'Catalizador deportivo 200 celdas 2"', 62000.00, 5),
                    ('ESC-004', 'Abrazadera Escape 2"', 'Abrazadera acero inox. para unión de caños 2"', 3500.00, 50),
                    ('ESC-005', 'Flexible Escape', 'Flexible corrugado acero inox. 2" x 20cm', 12000.00, 15),
                    ('ESC-006', 'Silenciador Deportivo', 'Silenciador deportivo doble salida acero inox.', 78000.00, 4),
                    ('ESC-007', 'Junta de Escape', 'Junta grafitada para múltiple de escape', 2800.00, 30),
                    ('ESC-008', 'Sensor de O2 Universal', 'Sensor de oxígeno 4 cables universal', 22000.00, 8),
                    ('ESC-009', 'Múltiple de Escape 4-1', 'Header 4 en 1 acero inoxidable', 95000.00, 3),
                    ('ESC-010', 'Cola de Escape Cromada', 'Terminal de escape cromada ovalada', 8500.00, 18)
                """);
                System.out.println("DB Seed: 10 productos de escape cargados.");
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
                System.out.println("DB Seed: 6 registros de historial cargados.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cierra la conexión compartida. Llamar al cerrar la aplicación.
     */
    public static synchronized void cerrarConexion() {
        try {
            if (sharedConnection != null && !sharedConnection.isClosed()) {
                sharedConnection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}