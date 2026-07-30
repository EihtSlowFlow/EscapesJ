package io.github.ramiro.escapesj.persistencia;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Optional;

public class UsuarioRepository {
    private final Connection connection;

    public UsuarioRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Valida las credenciales de un usuario de forma segura con BCrypt.
     * Si detecta una contraseña antigua en texto plano que coincide, la encripta automáticamente.
     */
    public boolean validarCredenciales(String usuario, String password) {
        String sql = "SELECT password FROM usuarios WHERE usuario = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbHash = rs.getString("password");
                
                // Migración suave: Si la contraseña no está encriptada con BCrypt
                if (!dbHash.startsWith("$2a$")) {
                    if (dbHash.equals(password)) {
                        // Coincide texto plano -> Encriptarla para el futuro
                        String nuevoHash = BCrypt.hashpw(password, BCrypt.gensalt());
                        cambiarPasswordDirecto(usuario, nuevoHash);
                        return true;
                    }
                    return false;
                }
                
                // Validación BCrypt normal
                return BCrypt.checkpw(password, dbHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper interno para guardar el hash directamente.
     */
    private void cambiarPasswordDirecto(String usuario, String hashNueva) {
        String sql = "UPDATE usuarios SET password = ? WHERE usuario = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashNueva);
            ps.setString(2, usuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuarioNuevo);
            ps.setString(2, usuarioActual);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene la pregunta de seguridad configurada para el usuario.
     */
    public Optional<String> obtenerPreguntaSeguridad(String usuario) {
        String sql = "SELECT pregunta_seguridad FROM usuarios WHERE usuario = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String preg = rs.getString("pregunta_seguridad");
                if (preg != null && !preg.trim().isEmpty()) {
                    return Optional.of(preg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pregunta);
            ps.setString(2, respuestaHash);
            ps.setString(3, usuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valida la respuesta de seguridad de forma segura contra el hash.
     */
    public boolean validarRespuestaSeguridad(String usuario, String respuestaPlana) {
        String sql = "SELECT respuesta_seguridad FROM usuarios WHERE usuario = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
            e.printStackTrace();
        }
        return false;
    }
}
