package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMigrationTest {

    @TempDir
    Path tempDir;

    private String dbUrl;

    @BeforeEach
    public void setUp() {
        Path dbPath = tempDir.resolve("test-migration.db");
        dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
        DatabaseService.setCustomDbUrl(dbUrl);
        DatabaseService.reiniciarTest();
    }

    @AfterEach
    public void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    private Connection getTestConnection() throws Exception {
        return DatabaseService.getConnection();
    }

    private void prepareSchemaMigrationsTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS schema_migrations (" +
                    "version INTEGER PRIMARY KEY, " +
                    "descripcion TEXT, " +
                    "ejecutado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "ms_transcurridos INTEGER)");
        }
    }

    @Test
    public void testDbVersionLegacyPeroFaltaColumna() throws Exception {
        // Simulamos db_version = 2 pero no existen las columnas de seguridad (caso real del issue)
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE configuracion (clave VARCHAR(100) PRIMARY KEY, valor TEXT NOT NULL)");
            stmt.execute("INSERT INTO configuracion (clave, valor) VALUES ('db_version', '2')");
            stmt.execute("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, usuario TEXT, password TEXT)");
            prepareSchemaMigrationsTable(conn);
        }

        // Ejecutamos migraciones normales
        DatabaseService.inicializar();

        // Verificamos que la migración 3 se aplicó (reparó) a pesar de db_version=2
        try (Connection conn = getTestConnection()) {
            assertTrue(DatabaseService.existeColumna(conn, "usuarios", "pregunta_seguridad"));
            
            // Verificamos que se registraron todas las migraciones
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(
                         "SELECT version FROM schema_migrations ORDER BY version")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
                assertTrue(rs.next());
                assertEquals(4, rs.getInt(1));
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    public void testBaseParcialmenteMigrada() throws Exception {
        // Simulamos que una de las columnas ya existe pero las otras no
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, usuario TEXT, password TEXT, pregunta_seguridad VARCHAR(255))");
            prepareSchemaMigrationsTable(conn);
        }

        // Llamamos a inicializar() para que cree el resto del esquema (boletas, etc.)
        // y luego ejecute las migraciones.
        DatabaseService.inicializar();
        
        try (Connection conn = getTestConnection()) {
            // Verificamos que ahora existen las 3 columnas y no tiró error por pregunta_seguridad
            assertTrue(DatabaseService.existeColumna(conn, "usuarios", "pregunta_seguridad"));
            assertTrue(DatabaseService.existeColumna(conn, "usuarios", "respuesta_seguridad"));
            assertTrue(DatabaseService.existeColumna(conn, "usuarios", "debe_cambiar_password"));
        }
    }

    @Test
    public void testInicializacionEjecutadaDosVeces() throws Exception {
        DatabaseService.inicializar(); // Crea todo
        
        // Ejecutamos la segunda vez
        assertDoesNotThrow(() -> {
            try (Connection conn = getTestConnection()) {
                DatabaseService.ejecutarMigraciones(conn, DatabaseService.MIGRACIONES);
            }
        });

        // Verificamos que solo se insertaron una vez los registros de migración
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_migrations")) {
            rs.next();
            assertEquals(5, rs.getInt(1), "Solo deben haber 5 migraciones registradas");
        }
    }

    @Test
    public void testFalloAlRegistrarVersionRevierteTodo() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE dummy (id INTEGER)");
            // La consulta de versión funciona, pero el INSERT falla porque faltan sus columnas.
            stmt.execute("CREATE TABLE schema_migrations (version INTEGER PRIMARY KEY)");
        }

        List<DatabaseService.Migration> testMigrations = List.of(
            new DatabaseService.Migration(99, "Test Migracion", conn -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE dummy ADD COLUMN nueva_columna TEXT");
                }
            })
        );

        assertThrows(PersistenceException.class, () -> {
            try (Connection conn = getTestConnection()) {
                DatabaseService.ejecutarMigraciones(conn, testMigrations);
            }
        });

        // Verificamos que se revirtió el ALTER TABLE
        try (Connection conn = getTestConnection()) {
            assertFalse(DatabaseService.existeColumna(conn, "dummy", "nueva_columna"));
        }
    }

    @Test
    public void testFalloAlConsultarVersionAplicadaSePropagaSinEjecutar() throws Exception {
        try (Connection conn = getTestConnection()) {
            int[] ejecuciones = {0};
            List<DatabaseService.Migration> migraciones = List.of(
                    new DatabaseService.Migration(99, "No debe ejecutarse", c -> ejecuciones[0]++));

            assertThrows(PersistenceException.class,
                    () -> DatabaseService.ejecutarMigraciones(conn, migraciones));
            assertEquals(0, ejecuciones[0]);
        }
    }

    @Test
    public void testVersionesFueraDeOrdenSeEjecutanEnOrdenAscendente() throws Exception {
        try (Connection conn = getTestConnection()) {
            prepareSchemaMigrationsTable(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test_table (id INTEGER)");
            }
            
            StringBuilder orden = new StringBuilder();
            List<DatabaseService.Migration> testMigrations = List.of(
                new DatabaseService.Migration(20, "Migración 20", c -> {
                    orden.append("20");
                    try (Statement stmt = c.createStatement()) {
                        stmt.execute("ALTER TABLE test_table ADD COLUMN col20 TEXT");
                    }
                }),
                new DatabaseService.Migration(10, "Migración 10", c -> {
                    orden.append("10");
                    try (Statement stmt = c.createStatement()) {
                        stmt.execute("ALTER TABLE test_table ADD COLUMN col10 TEXT");
                    }
                })
            );
            
            DatabaseService.ejecutarMigraciones(conn, testMigrations);
            
            assertEquals("1020", orden.toString());
            assertTrue(DatabaseService.existeColumna(conn, "test_table", "col10"));
            assertTrue(DatabaseService.existeColumna(conn, "test_table", "col20"));
        }
    }

    @Test
    public void testVersionesDuplicadasSeRechazanSinEjecutar() throws Exception {
        try (Connection conn = getTestConnection()) {
            prepareSchemaMigrationsTable(conn);
            int[] ejecuciones = {0};
            List<DatabaseService.Migration> migraciones = List.of(
                    new DatabaseService.Migration(10, "Primera", c -> ejecuciones[0]++),
                    new DatabaseService.Migration(10, "Duplicada", c -> ejecuciones[0]++));

            assertThrows(PersistenceException.class,
                    () -> DatabaseService.ejecutarMigraciones(conn, migraciones));
            assertEquals(0, ejecuciones[0]);
        }
    }

    @Test
    public void testConversionMonetariaExactaYNoSeRepite() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE productos (codigo TEXT PRIMARY KEY, nombre TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL, precio REAL NOT NULL, stock INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("INSERT INTO productos VALUES ('TEST', 'Producto', 'Prueba', 1000.50, 1)");
        }

        DatabaseService.inicializar();
        assertEquals(100050L, obtenerPrecioProductoTest());

        try (Connection conn = getTestConnection()) {
            DatabaseService.ejecutarMigraciones(conn, DatabaseService.MIGRACIONES);
        }
        assertEquals(100050L, obtenerPrecioProductoTest());
    }

    @Test
    public void testDbVersionCorruptoFallaSinModificarImportes() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE configuracion (clave VARCHAR(100) PRIMARY KEY, valor TEXT NOT NULL)");
            stmt.execute("INSERT INTO configuracion VALUES ('db_version', 'abc')");
            stmt.execute("CREATE TABLE productos (codigo TEXT PRIMARY KEY, nombre TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL, precio REAL NOT NULL, stock INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("INSERT INTO productos VALUES ('TEST', 'Producto', 'Prueba', 1000.50, 1)");
        }

        assertThrows(PersistenceException.class, DatabaseService::inicializar);
        assertEquals(1000.50, obtenerPrecioProductoTest(), 0.001);
    }

    private double obtenerPrecioProductoTest() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT precio FROM productos WHERE codigo = 'TEST'")) {
            assertTrue(rs.next());
            return rs.getDouble(1);
        }
    }

    @Test
    public void testBaseNuevaRegistraSinEjecutarAlter() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            prepareSchemaMigrationsTable(conn);
            // Creamos usuarios ya con todas las columnas
            stmt.execute("CREATE TABLE usuarios (" +
                    "id INTEGER PRIMARY KEY, " +
                    "usuario VARCHAR(50), " +
                    "password VARCHAR(255), " +
                    "pregunta_seguridad VARCHAR(255), " +
                    "respuesta_seguridad VARCHAR(100), " +
                    "debe_cambiar_password INTEGER)");
        }

        try (Connection conn = getTestConnection()) {
            DatabaseService.ejecutarMigraciones(conn, List.of(DatabaseService.MIGRACIONES.get(2))); // Solo la 3
            
            // Verificamos que la migración se registró
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM schema_migrations WHERE version = 3")) {
                assertTrue(rs.next());
            }
        }
    }

    @Test
    public void testFilaDeMigracionYCambioEstructuralSeReviertenJuntos() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE dummy (id INTEGER)");
            prepareSchemaMigrationsTable(conn);
        }

        List<DatabaseService.Migration> testMigrations = List.of(
            new DatabaseService.Migration(100, "Migración Fallida", conn -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE dummy ADD COLUMN valida TEXT");
                    // Ahora ejecutamos un SQL erróneo
                    stmt.execute("SENTENCIA SQL INVALIDA");
                }
            })
        );

        assertThrows(PersistenceException.class, () -> {
            try (Connection conn = getTestConnection()) {
                DatabaseService.ejecutarMigraciones(conn, testMigrations);
            }
        });

        try (Connection conn = getTestConnection()) {
            // Verificamos que no existe la columna
            assertFalse(DatabaseService.existeColumna(conn, "dummy", "valida"));
            // Verificamos que no se insertó la migración
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version = 100")) {
                rs.next();
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    public void testMigracion5RespetaLegacyCosts() throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE productos (codigo TEXT PRIMARY KEY, nombre TEXT NOT NULL, descripcion TEXT NOT NULL, precio INTEGER NOT NULL, stock INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE boletas (id INTEGER PRIMARY KEY AUTOINCREMENT, numero INTEGER NOT NULL UNIQUE, dni VARCHAR(20) NOT NULL, nombre_cliente VARCHAR(100), fecha VARCHAR(20) NOT NULL, total INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE boleta_items (id INTEGER PRIMARY KEY AUTOINCREMENT, boleta_id INTEGER NOT NULL, tipo VARCHAR(10) NOT NULL, descripcion TEXT NOT NULL, codigo_producto VARCHAR(50), cantidad INTEGER NOT NULL DEFAULT 1, precio_unitario INTEGER NOT NULL DEFAULT 0, subtotal INTEGER NOT NULL DEFAULT 0)");
            prepareSchemaMigrationsTable(conn);
            
            stmt.execute("INSERT INTO boletas (id, numero, dni, fecha) VALUES (1, 100, '123', '2023-01-01')");
            
            // 1. Producto sin costo -> conservará NULL
            stmt.execute("INSERT INTO boleta_items (id, boleta_id, tipo, descripcion) VALUES (1, 1, 'PRODUCTO', 'P1')");
            // 2. Servicio sin costo -> pasará a 0
            stmt.execute("INSERT INTO boleta_items (id, boleta_id, tipo, descripcion) VALUES (2, 1, 'SERVICIO', 'S1')");
        }

        DatabaseService.inicializar();
        
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            
            // Forzamos un update directo para simular "costo conocido o cero" antes de aplicar la 5 (porque la init aplica todas).
            // Espera, inicializar() ya aplicó la 4 y la 5. 
            // Si inserté la data *antes* de inicializar, la 4 le puso NULL a costo_unitario_historico_centavos.
            // Luego la 5 lo actualizó a 0 solo si era SERVICIO.
            
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id, costo_unitario_historico_centavos FROM boleta_items ORDER BY id")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("id"));
                rs.getInt("costo_unitario_historico_centavos");
                assertTrue(rs.wasNull());
                
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("id"));
                assertEquals(0, rs.getInt("costo_unitario_historico_centavos"));
            }
        }
    }

    @Test
    public void testOnDeleteSetNullOperacionesHistoricas() throws Exception {
        DatabaseService.inicializar(); // Crea las tablas y aplica pragma foreign_keys = ON

        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("PRAGMA foreign_keys = ON;");
            
            // Insertar boleta digital
            stmt.execute("INSERT INTO boletas (id, numero, dni, fecha) VALUES (1, 200, '111', '2023-01-01')");
            
            // Insertar operación vinculada
            stmt.execute("INSERT INTO operaciones_historicas (fecha, descripcion, importe_total_centavos, estado, boleta_digital_id, creado_en, actualizado_en) " +
                    "VALUES ('2023-01-01', 'Test', 1000, 'DIGITALIZADO', 1, 'now', 'now')");
            
            // Eliminar la boleta digital
            stmt.execute("DELETE FROM boletas WHERE id = 1");
            
            // Verificar que boleta_digital_id se puso en NULL
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT boleta_digital_id FROM operaciones_historicas")) {
                assertTrue(rs.next());
                rs.getInt("boleta_digital_id");
                assertTrue(rs.wasNull());
            }
        }
    }
}
