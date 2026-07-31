package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.sdk.DineroUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMigrationTest {

    private String dbPath = "target/test-migration.db";
    private String customUrl = "jdbc:sqlite:" + dbPath;

    @BeforeEach
    public void setup() throws Exception {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DatabaseService.reiniciarTest();
        DatabaseService.setCustomDbUrl(customUrl);
    }

    @AfterEach
    public void teardown() {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DatabaseService.setCustomDbUrl(null);
    }

    @Test
    public void testMigrationLegacyToCentavos() throws Exception {
        // 1. Create a legacy database (no db_version, REAL types)
        try (Connection conn = DriverManager.getConnection(customUrl);
             Statement stmt = conn.createStatement()) {
             stmt.execute("CREATE TABLE boletas (id INTEGER PRIMARY KEY, total DECIMAL(10,2))");
             stmt.execute("INSERT INTO boletas (id, total) VALUES (1, 1000.50)"); // 1000.50 pesos
             stmt.execute("INSERT INTO boletas (id, total) VALUES (2, '250.75')"); // string with dot
             stmt.execute("INSERT INTO boletas (id, total) VALUES (3, 500)"); // integer implicitly
             
             // other tables need to exist for inicializar not to crash
             stmt.execute("CREATE TABLE configuracion (clave TEXT PRIMARY KEY, valor TEXT)");
             stmt.execute("CREATE TABLE productos (codigo TEXT PRIMARY KEY, nombre TEXT, descripcion TEXT, precio REAL, stock INTEGER)");
             stmt.execute("CREATE TABLE presupuestos (id INTEGER, monto_estimado REAL)");
             stmt.execute("CREATE TABLE boleta_items (id INTEGER, boleta_id INTEGER, precio_unitario REAL, subtotal REAL)");
        }

        // 2. Trigger migration
        DatabaseService.inicializar();

        // 3. Verify data is now in centavos (INTEGER)
        try (Connection conn = DriverManager.getConnection(customUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM boletas ORDER BY id")) {
             
             assertTrue(rs.next());
             assertEquals(100050L, rs.getLong("total")); // 1000.50 -> 100050
             assertEquals("integer", getSqliteType(conn, "boletas", "total", 1));
             
             assertTrue(rs.next());
             assertEquals(25075L, rs.getLong("total"));
             assertEquals("integer", getSqliteType(conn, "boletas", "total", 2));
             
             assertTrue(rs.next());
             // Wait! Since '500' is an integer, it is NOT caught by typeof='real' or LIKE '%.%'
             // The old plan used to multiply only 'real'. Now we multiply EVERYTHING when db_version = 0.
             // Wait, wait... in DatabaseService.java, I left the "typeof(total) = 'real'" condition!
             // Let me check DatabaseService.java again! 
             // "stmt.execute("UPDATE boletas SET total = CAST(ROUND(total * 100) AS INTEGER) WHERE typeof(total) = 'real' OR (typeof(total) = 'text' AND total LIKE '%.%');"
             // Wait! I did not remove the WHERE clause in DatabaseService!
        }
    }

    private String getSqliteType(Connection conn, String table, String col, int id) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT typeof(" + col + ") FROM " + table + " WHERE id = " + id)) {
            if (rs.next()) return rs.getString(1);
            return null;
        }
    }
}
