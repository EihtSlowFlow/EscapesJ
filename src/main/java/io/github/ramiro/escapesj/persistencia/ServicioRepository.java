package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.ServicioRealizado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioRepository {

    public ServicioRepository() {
    }

    public void registrar(ServicioRealizado s) {
        registrar(this.connection, s);
    }

    public void registrar(Connection txConn, ServicioRealizado s) {
        String sql = "INSERT INTO servicios_historial (dni, nombre, trabajo, fecha) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = txConn.prepareStatement(sql)) {
            ps.setString(1, s.getDni());
            ps.setString(2, s.getNombre());
            ps.setString(3, s.getTrabajo());
            ps.setString(4, s.getFecha());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error registrando servicio", e);
        }
    }

    public List<ServicioRealizado> buscarPorDni(String dni) {
        List<ServicioRealizado> lista = new ArrayList<>();
        String sql = "SELECT * FROM servicios_historial WHERE dni = ? ORDER BY fecha DESC";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dni);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new ServicioRealizado(rs.getString("dni"), rs.getString("nombre"),
                        rs.getString("trabajo"), rs.getString("fecha")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}