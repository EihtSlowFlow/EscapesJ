package io.github.ramiro.escapesj.persistencia;

import java.sql.*;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioRepository {

    public UsuarioRepository() {
    }

    /**
     * Valida las credenciales de un usuario.
     * Retorna true si el usuario y contraseña coinciden usando BCrypt.
     */
    public boolean validarCredenciales(String usuario, String password) {
        String sql = "SELECT password FROM usuarios WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashDb = rs.getString("password");
                return BCrypt.checkpw(password, hashDb);
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            // BCrypt throws IllegalArgumentException if the hash is invalid (e.g., from old plaintext passwords)
            System.err.println("Hash de contraseña inválido para el usuario: " + usuario);
            return false;
        }
    }

    /**
     * Cambia la contraseña de un usuario existente encriptándola con BCrypt.
     */
    public boolean cambiarPassword(String usuario, String passwordActual, String passwordNueva) {
        if (!validarCredenciales(usuario, passwordActual)) {
            return false;
        }
        
        String hashNuevo = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
        String sql = "UPDATE usuarios SET password = ? WHERE usuario = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashNuevo);
            ps.setString(2, usuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
            e.printStackTrace();
            return false;
        }
    }
}
