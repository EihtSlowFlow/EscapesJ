package io.github.ramiro.escapesj.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoletaRepository {
    private static final Logger logger = LoggerFactory.getLogger(BoletaRepository.class);


    public BoletaRepository() {
    }

    public record BoletaResumen(int id, int numero, String dni, String nombreCliente, String fecha, BigDecimal total) {}
    public record BoletaItem(int id, String tipo, String descripcion, String codigoProducto, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    private int siguienteNumero() {
        String sql = "SELECT MAX(numero) FROM boletas";
        try (Connection conexion = DatabaseService.getConnection();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return max > 0 ? max + 1 : 1;
            }
        } catch (Exception e) {
            logger.error("Error:", e);
        }
        return 1;
    }

    public int crearBoleta(String dni, String nombreCliente, String fecha, BigDecimal total) {
        int numero = siguienteNumero();
        String sql = """
                INSERT INTO boletas (numero, dni, nombre_cliente, fecha, total)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conexion = DatabaseService.getConnection();
             PreparedStatement pstmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, numero);
            pstmt.setString(2, dni);
            pstmt.setString(3, nombreCliente);
            pstmt.setString(4, fecha);
            pstmt.setBigDecimal(5, total);
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error:", e);
        }
        return -1;
    }

    public void agregarItem(int boletaId, String tipo, String descripcion, String codigoProducto, int cantidad, BigDecimal precioUnitario) {
        String sql = """
                INSERT INTO boleta_items (boleta_id, tipo, descripcion, codigo_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexion = DatabaseService.getConnection();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, boletaId);
            pstmt.setString(2, tipo);
            pstmt.setString(3, descripcion);
            pstmt.setString(4, codigoProducto);
            pstmt.setInt(5, cantidad);
            pstmt.setBigDecimal(6, precioUnitario);
            pstmt.setBigDecimal(7, precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error:", e);
        }
    }

    public List<BoletaResumen> buscarBoletasPorDni(String dni) {
        List<BoletaResumen> lista = new ArrayList<>();
        String sql = """
                SELECT id, numero, dni, nombre_cliente, fecha, total
                FROM boletas
                WHERE dni = ?
                ORDER BY fecha DESC
                """;
        try (Connection conexion = DatabaseService.getConnection();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, dni);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new BoletaResumen(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getString("dni"),
                            rs.getString("nombre_cliente"),
                            rs.getString("fecha"),
                            rs.getBigDecimal("total")
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error:", e);
        }
        return lista;
    }

    public List<BoletaItem> obtenerItems(int boletaId) {
        List<BoletaItem> lista = new ArrayList<>();
        String sql = """
                SELECT id, tipo, descripcion, codigo_producto, cantidad, precio_unitario, subtotal
                FROM boleta_items
                WHERE boleta_id = ?
                """;
        try (Connection conexion = DatabaseService.getConnection();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, boletaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new BoletaItem(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("descripcion"),
                            rs.getString("codigo_producto"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("subtotal")
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error:", e);
        }
        return lista;
    }
}
