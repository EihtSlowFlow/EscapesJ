package io.github.ramiro.escapesj.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;

public class DatabaseService {
    private String url;
    private String user;
    private String password;

    public DatabaseService() {
        cargarCredenciales();
        inicializarTablas();
    }

    private void cargarCredenciales() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            this.url = prop.getProperty("db.url");
            this.user = prop.getProperty("db.user");
            this.password = prop.getProperty("db.password");
        } catch (Exception e) {
            System.err.println("Error cargando config.properties para DB");
        }
    }

    private void inicializarTablas() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Tabla Clientes: DNI como PK, ID como Unique Auto-increment
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS clientes (
                            id INT NOT NULL AUTO_INCREMENT UNIQUE,
                            dni VARCHAR(11) NOT NULL,
                            nombre VARCHAR(100),
                            PRIMARY KEY (dni)
                        )
                    """);

            // Tabla Productos (Inventario)
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS productos (
                            codigo VARCHAR(50) PRIMARY KEY,
                            descripcion TEXT NOT NULL,
                            precio DECIMAL(10,2) NOT NULL
                        )
                    """);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);

            // DEBUG: Vamos a ver qué está leyendo Java realmente
            System.out.println("Intentando conectar con usuario: " + props.getProperty("db.user"));
            System.out.println("URL cargada: " + props.getProperty("db.url"));

            return DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
        } catch (Exception e) {
            // Esto te dirá si el archivo directamente no se encuentra (FileNotFoundException)
            System.err.println("Error al cargar config.properties: " + e.getMessage());
            return null;
        }
    }
}