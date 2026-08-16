package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.ServicioRealizado;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServicioRepository {
    private static final Logger logger = LoggerFactory.getLogger(ServicioRepository.class);


    public ServicioRepository() {
    }

    public void registrar(ServicioRealizado s) {
        registrarServicio(s.getDni(), s.getNombre(), s.getTrabajo(), s.getFecha());
    }

    public void registrarServicio(String dni, String nombre, String trabajo, String fecha) {
        String sql = "INSERT INTO servicios_historial (dni, nombre, trabajo, fecha) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, nombre);
            ps.setString(3, trabajo);
            ps.setString(4, fecha);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error registrando servicio", e);
        }
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
            logger.error("Error:", e);
            logger.error("Error obteniendo número de presupuesto:", e);
            throw new PersistenceException("Error registrando servicio", e);
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
            logger.error("Error:", e);
            throw new PersistenceException("Error buscando servicios", e);
        }
        return lista;
    }
}