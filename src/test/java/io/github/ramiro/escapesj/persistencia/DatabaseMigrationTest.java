package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationTest {

    @TempDir
    Path tempDir;
    
    private String dbUrl;

    @BeforeEach
    void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test.db");
        dbUrl = "jdbc:sqlite:" + db.toAbsolutePath().toString();
        DatabaseService.reiniciarTest();
        DatabaseService.setCustomDbUrl(dbUrl);
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void migraImportesLegacyYMarcaLaVersion() throws Exception {
        crearBaseLegacy();

        DatabaseService.inicializar();

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {
            try (ResultSet version = statement.executeQuery(
                    "SELECT valor FROM configuracion WHERE clave = 'db_version'")) {
                assertTrue(version.next());
                assertEquals("2", version.getString(1));
            }

            assertImporteEnCentavos(statement, "boletas", "id", 1, "total", 100050L);
            assertImporteEnCentavos(statement, "boletas", "id", 2, "total", 25075L);
            assertImporteEnCentavos(statement, "boletas", "id", 3, "total", 50000L);
            assertImporteEnCentavos(statement, "boleta_items", "id", 1, "precio_unitario", 125050L);
            assertImporteEnCentavos(statement, "boleta_items", "id", 1, "subtotal", 250100L);
            assertImporteEnCentavos(statement, "productos", "codigo", "'P-1'", "precio", 4500000L);
            assertImporteEnCentavos(statement, "presupuestos", "id", 1, "monto_estimado", 999999L);
        }

        assertEquals(0, new BigDecimal("45000.00")
                .compareTo(new ProductoRepository().buscarPorCodigo("P-1").orElseThrow().getPrecio()));
    }

    @Test
    void migracionEsIdempotenteCuandoLaVersionYaEsUno() throws Exception {
        crearBaseLegacy();
        DatabaseService.inicializar();

        // El reinicio simula un segundo arranque de la aplicación.
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT total FROM boletas WHERE id = 1")) {
            assertTrue(result.next());
            assertEquals(100050L, result.getLong(1));
        }
    }

    private void crearBaseLegacy() throws Exception {
        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE configuracion (clave TEXT PRIMARY KEY, valor TEXT)");
            statement.execute("CREATE TABLE boletas (id INTEGER PRIMARY KEY, numero INTEGER, dni TEXT, fecha TEXT, total DECIMAL(10,2))");
            statement.execute("CREATE TABLE boleta_items (id INTEGER PRIMARY KEY, boleta_id INTEGER, cantidad INTEGER, precio_unitario DECIMAL(10,2), subtotal DECIMAL(10,2))");
            statement.execute("CREATE TABLE productos (codigo TEXT PRIMARY KEY, nombre TEXT, descripcion TEXT, precio DECIMAL(10,2), stock INTEGER)");
            statement.execute("CREATE TABLE presupuestos (id INTEGER PRIMARY KEY, monto_estimado DECIMAL(10,2))");

            statement.execute("INSERT INTO boletas VALUES (1, 1, '1', '2026-01-01', 1000.50)");
            statement.execute("INSERT INTO boletas VALUES (2, 2, '2', '2026-01-01', '250.75')");
            statement.execute("INSERT INTO boletas VALUES (3, 3, '3', '2026-01-01', 500)");
            statement.execute("INSERT INTO boleta_items VALUES (1, 1, 1, 1250.50, 2501.00)");
            statement.execute("INSERT INTO productos VALUES ('P-1', 'Producto', 'Legacy', 45000.00, 1)");
            statement.execute("INSERT INTO presupuestos VALUES (1, 9999.99)");
        }
    }

    private void assertImporteEnCentavos(Statement statement, String table, String keyColumn,
                                         Object keyValue, String column, long expected) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT " + column + ", typeof(" + column + ") FROM " + table
                        + " WHERE " + keyColumn + " = " + keyValue)) {
            assertTrue(result.next());
            assertEquals(expected, result.getLong(1));
            assertEquals("integer", result.getString(2));
        }
    }

    // deleteDatabaseFiles() is no longer needed with @TempDir
}
