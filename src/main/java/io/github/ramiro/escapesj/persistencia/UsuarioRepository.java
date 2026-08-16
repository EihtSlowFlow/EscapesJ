package io.github.ramiro.escapesj.persistencia;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class UsuarioRepository {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioRepository.class);

    public UsuarioRepository() {
    }

    /**
     * Comprueba si la tabla de usuarios está vacía.
     */
    public boolean isUsuariosEmpty() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection connection = DatabaseService.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            logger.error("Error comprobando usuarios: ", e);
            throw new PersistenceException("Error comprobando usuarios", e);
        }
        return true;
    }

    /**
     * Crea el usuario administrador inicial y lo marca para requerir cambio de contraseña.
     */
    public boolean crearAdminSetupInicial(String username, String tempPassword) {
        String hash = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
        // Como el usuario elige su propia contraseña en el setup UI, no lo forzamos a cambiarla.
        String sql = "INSERT INTO usuarios (usuario, password, debe_cambiar_password) VALUES (?, ?, 0)";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creando admin inicial: ", e);
            throw new PersistenceException("Error creando admin inicial", e);
        }
    }

    /**
     * Valida si el usuario debe cambiar su contraseña obligatoriamente.
     */
    public boolean debeCambiarPassword(String usuario) {
        String sql = "SELECT debe_cambiar_password FROM usuarios WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("debe_cambiar_password") == 1;
            }
        } catch (SQLException e) {
            logger.error("Error leyendo debe_cambiar_password: ", e);
            throw new PersistenceException("Error leyendo debe_cambiar_password", e);
        }
        return false;
    }

    /**
     * Valida las credenciales de un usuario de forma segura con BCrypt.
     */
    public boolean validarCredenciales(String usuario, String password) {
        String sql = "SELECT password FROM usuarios WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbHash = rs.getString("password");

                // Migración suave
                if (!dbHash.startsWith("$2a$")) {
                    if (dbHash.equals(password)) {
                        String nuevoHash = BCrypt.hashpw(password, BCrypt.gensalt());
                        cambiarPasswordDirecto(usuario, nuevoHash);
                        return true;
                    }
                    return false;
                }

                return BCrypt.checkpw(password, dbHash);
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error validando credenciales", e);
        } catch (IllegalArgumentException e) {
            logger.error("Hash de contraseña inválido para el usuario: " + usuario);
        }
        return false;
    }

    /**
     * Helper interno para guardar el hash directamente.
     */
    private void cambiarPasswordDirecto(String usuario, String hashNueva) {
        String sql = "UPDATE usuarios SET password = ?, debe_cambiar_password = 0 WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashNueva);
            ps.setString(2, usuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error cambiando password directo", e);
        }
    }

    /**
     * Cambia la contraseña de un usuario existente validando la actual.
     */
    public boolean cambiarPassword(String usuario, String passwordActual, String passwordNueva) {
        if (!validarCredenciales(usuario, passwordActual)) {
            return false;
        }
        String hashNueva = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
        cambiarPasswordDirecto(usuario, hashNueva);
        return true;
    }

    /**
     * Restablece la contraseña sin conocer la actual (usado por recuperación).
     */
    public void resetPassword(String usuario, String passwordNueva) {
        String hashNueva = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
        cambiarPasswordDirecto(usuario, hashNueva);
    }

    /**
     * Cambia el nombre de usuario.
     */
    public boolean cambiarUsuario(String usuarioActual, String usuarioNuevo) {
        String sql = "UPDATE usuarios SET usuario = ? WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuarioNuevo);
            ps.setString(2, usuarioActual);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error cambiando usuario", e);
        }
    }

    /**
     * Obtiene la pregunta de seguridad configurada para el usuario.
     */
    public Optional<String> obtenerPreguntaSeguridad(String usuario) {
        String sql = "SELECT pregunta_seguridad FROM usuarios WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String preg = rs.getString("pregunta_seguridad");
                if (preg != null && !preg.trim().isEmpty()) {
                    return Optional.of(preg);
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo pregunta:", e);
            throw new PersistenceException("Error obteniendo pregunta", e);
        }
        return Optional.empty();
    }

    /**
     * Configura la pregunta y respuesta de seguridad (hasheando la respuesta).
     */
    public boolean configurarPreguntaSeguridad(String usuario, String pregunta, String respuestaPlana) {
        String respuestaNormalizada = respuestaPlana.trim().toLowerCase();
        String respuestaHash = BCrypt.hashpw(respuestaNormalizada, BCrypt.gensalt());

        String sql = "UPDATE usuarios SET pregunta_seguridad = ?, respuesta_seguridad = ? WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pregunta);
            ps.setString(2, respuestaHash);
            ps.setString(3, usuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error configurando pregunta:", e);
            throw new PersistenceException("Error configurando pregunta", e);
        }
    }

    /**
     * Valida la respuesta de seguridad de forma segura contra el hash.
     */
    public boolean validarRespuestaSeguridad(String usuario, String respuestaPlana) {
        String sql = "SELECT respuesta_seguridad FROM usuarios WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbHash = rs.getString("respuesta_seguridad");
                if (dbHash != null && !dbHash.isEmpty()) {
                    String respuestaNormalizada = respuestaPlana.trim().toLowerCase();
                    return BCrypt.checkpw(respuestaNormalizada, dbHash);
                }
            }
        } catch (SQLException e) {
            logger.error("Error validando respuesta:", e);
            throw new PersistenceException("Error validando respuesta", e);
        }
        return false;
    }

    public void actualizarCredenciales(String usuarioActual, String usuarioNuevo, String passwordNueva, String pregunta, String respuestaPlana) {
        try {
            TransactionHelper.runInTransaction(connection -> {
                // 1. Cambiar usuario
                if (usuarioNuevo != null && !usuarioNuevo.isBlank() && !usuarioActual.equals(usuarioNuevo)) {
                    String sqlUsr = "UPDATE usuarios SET usuario = ? WHERE usuario = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sqlUsr)) {
                        ps.setString(1, usuarioNuevo);
                        ps.setString(2, usuarioActual);
                        ps.executeUpdate();
                    }
                }
                String usuarioDestino = (usuarioNuevo != null && !usuarioNuevo.isBlank()) ? usuarioNuevo : usuarioActual;

                // 2. Cambiar contraseña
                if (passwordNueva != null && !passwordNueva.isBlank()) {
                    String hashNueva = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
                    String sqlPwd = "UPDATE usuarios SET password = ?, debe_cambiar_password = 0 WHERE usuario = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sqlPwd)) {
                        ps.setString(1, hashNueva);
                        ps.setString(2, usuarioDestino);
                        ps.executeUpdate();
                    }
                }

                // 3. Configurar pregunta de seguridad
                if (pregunta != null && !pregunta.isBlank() && respuestaPlana != null && !respuestaPlana.isBlank()) {
                    String respuestaNormalizada = respuestaPlana.trim().toLowerCase();
                    String respuestaHash = BCrypt.hashpw(respuestaNormalizada, BCrypt.gensalt());
                    String sqlSec = "UPDATE usuarios SET pregunta_seguridad = ?, respuesta_seguridad = ? WHERE usuario = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sqlSec)) {
                        ps.setString(1, pregunta);
                        ps.setString(2, respuestaHash);
                        ps.setString(3, usuarioDestino);
                        ps.executeUpdate();
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error actualizando credenciales", e);
        }
    }
}
