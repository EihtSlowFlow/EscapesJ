package io.github.ramiro.escapesj.persistencia;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        }
        return Optional.empty();
    }

    /**
     * Guarda o actualiza un valor de configuración.
     */
    public void guardar(String clave, String valor) {
        if ("afip.access_token".equals(clave)) {
            valor = io.github.ramiro.escapesj.sdk.CryptoUtil.encrypt(valor);
        }
        
    public void guardar(String clave, String valor) throws SQLException {
        String sql = "INSERT INTO configuracion (clave, valor) VALUES (?, ?) " +
                "ON CONFLICT(clave) DO UPDATE SET valor = excluded.valor";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
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

    /**
     * Obtiene la ruta donde se guardarán las boletas.
     * Fallback: carpeta Documentos del usuario.
     */
    public String getRutaBoletas() {
        return obtener("ruta.boletas").orElse(
                System.getProperty("user.home") + "/Documentos/escapesJ/boletas/"
        );
    }

    /**
     * Obtiene la ruta donde se guardarán los presupuestos.
     * Fallback: carpeta Documentos del usuario.
     */
    public String getRutaPresupuestos() {
        return obtener("ruta.presupuestos").orElse(
                System.getProperty("user.home") + "/Documentos/escapesJ/presupuestos/"
        );
    }
}
