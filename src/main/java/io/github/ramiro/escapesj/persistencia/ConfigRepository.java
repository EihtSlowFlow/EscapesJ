package io.github.ramiro.escapesj.persistencia;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(ConfigRepository.class);


    public ConfigRepository() {
    }

    /**
     * Obtiene un valor de configuración por su clave.
     */
    public Optional<String> obtener(String clave) {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, clave);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String valor = rs.getString("valor");
                return (valor != null && !valor.isBlank()) ? Optional.of(valor) : Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al obtener configuración", e);
        }
        return Optional.empty();
    }

    /**
     * Guarda o actualiza un valor de configuración (abre su propia conexión).
     */
    public void guardar(String clave, String valor) {
        try (Connection connection = DatabaseService.getConnection()) {
            guardar(connection, clave, valor);
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al guardar la configuración: " + clave, e);
        }
    }

    /**
     * Guarda múltiples valores de forma atómica.
     */
    public void guardarMultiples(Map<String, String> configuraciones) {
        try {
            TransactionHelper.runInTransaction(connection -> {
                for (Map.Entry<String, String> entry : configuraciones.entrySet()) {
                    guardar(connection, entry.getKey(), entry.getValue());
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error guardando configuraciones múltiples", e);
        }
    }

    private void guardar(Connection connection, String clave, String valor) throws SQLException {
        if ("afip.access_token".equals(clave)) {
            valor = io.github.ramiro.escapesj.sdk.CryptoUtil.encrypt(valor);
        }

        String sql = "INSERT INTO configuracion (clave, valor) VALUES (?, ?) " +
                "ON CONFLICT(clave) DO UPDATE SET valor = excluded.valor";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene el Access Token de Afip SDK configurado.
     */
    public String getAfipAccessToken() {
        return obtener("afip.access_token")
                .map(io.github.ramiro.escapesj.sdk.CryptoUtil::decrypt)
                .orElse("");
    }

    /**
     * Obtiene el CUIT configurado para AFIP.
     */
    public String getAfipCuit() {
        return obtener("afip.cuit").orElse("20409378472");
    }

    /**
     * Retorna si se debe usar el ambiente de producción.
     */
    public boolean isAfipProduction() {
        return obtener("afip.production").map("true"::equals).orElse(false);
    }

    /**
     * Obtiene la ruta al archivo de certificado (.crt) de AFIP.
     */
    public String getAfipCertPath() {
        return obtener("afip.cert_path").orElse("");
    }

    /**
     * Obtiene la ruta al archivo de clave privada (.key) de AFIP.
     */
    public String getAfipKeyPath() {
        return obtener("afip.key_path").orElse("");
    }

    public static String getDefaultDocumentsPath() {
        java.io.File docs = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();
        if (docs != null && docs.exists()) {
            return docs.getAbsolutePath();
        }
        return System.getProperty("user.home") + java.io.File.separator + "Documents";
    }

    public static String getDefaultBoletasPath() {
        return getDefaultDocumentsPath() + java.io.File.separator + "escapesJ" + java.io.File.separator + "boletas" + java.io.File.separator;
    }

    public static String getDefaultPresupuestosPath() {
        return getDefaultDocumentsPath() + java.io.File.separator + "escapesJ" + java.io.File.separator + "presupuestos" + java.io.File.separator;
    }

    /**
     * Guarda las rutas configuradas. Si una ruta coincide con el valor por defecto,
     * se persiste vacia para que siga resolviendose dinamicamente en futuros equipos
     * o ante cambios en la carpeta Documentos del usuario.
     */
    public void guardarRutas(String rutaBoletas, String rutaPresupuestos) {
        Map<String, String> configuraciones = new HashMap<>();
        configuraciones.put("ruta.boletas", quitarOverrideSiEsDefault(rutaBoletas, getDefaultBoletasPath()));
        configuraciones.put("ruta.presupuestos", quitarOverrideSiEsDefault(rutaPresupuestos, getDefaultPresupuestosPath()));
        guardarMultiples(configuraciones);
    }

    private static String quitarOverrideSiEsDefault(String ruta, String rutaDefault) {
        String valor = ruta == null ? "" : ruta.trim();
        if (valor.isEmpty()) {
            return "";
        }

        try {
            java.nio.file.Path path = java.nio.file.Paths.get(valor).toAbsolutePath().normalize();
            java.nio.file.Path defaultPath = java.nio.file.Paths.get(rutaDefault).toAbsolutePath().normalize();
            return path.equals(defaultPath) ? "" : valor;
        } catch (java.nio.file.InvalidPathException e) {
            return valor;
        }
    }

    /**
     * Obtiene la ruta donde se guardarán las boletas.
     * Fallback: carpeta Documentos del usuario.
     */
    public String getRutaBoletas() {
        return obtener("ruta.boletas").orElse(getDefaultBoletasPath());
    }

    /**
     * Obtiene la ruta donde se guardarán los presupuestos.
     * Fallback: carpeta Documentos del usuario.
     */
    public String getRutaPresupuestos() {
        return obtener("ruta.presupuestos").orElse(getDefaultPresupuestosPath());
    }
}
