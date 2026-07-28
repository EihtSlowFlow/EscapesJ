package io.github.ramiro.escapesj.persistencia;

import java.sql.*;
import java.util.Optional;

public class UsuarioRepository {
    private final Connection connection;

    public UsuarioRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Valida las credenciales de un usuario.
     * Retorna true si el usuario y contraseña coinciden.
     */
    public boolean validarCredenciales(String usuario, String password) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cambia la contraseña de un usuario existente.
     */
    public boolean cambiarPassword(String usuario, String passwordActual, String passwordNueva) {
        if (!validarCredenciales(usuario, passwordActual)) {
            return false;
        }
        String sql = "UPDATE usuarios SET password = ? WHERE usuario = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, passwordNueva);
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, usuarioNuevo);
            ps.setString(2, usuarioActual);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
