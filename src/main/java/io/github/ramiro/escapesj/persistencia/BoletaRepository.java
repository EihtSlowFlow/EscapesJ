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

    private int siguienteNumero(Connection txConn) {
        String sql = "SELECT MAX(numero) FROM boletas";
        try (Statement stmt = txConn.createStatement();
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

    public int crearBoleta(String dni, String nombreCliente, String fecha, double total) {
        try (Connection conn = DatabaseService.getConnection()) {
            return crearBoleta(conn, dni, nombreCliente, fecha, total);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int crearBoleta(Connection txConn, String dni, String nombreCliente, String fecha, double total) {
        int numero = siguienteNumero(txConn);
        String sql = """
                INSERT INTO boletas (numero, dni, nombre_cliente, fecha, total)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = txConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, numero);
            pstmt.setString(2, dni);
            pstmt.setString(3, nombreCliente);
            pstmt.setString(4, fecha);
            pstmt.setBigDecimal(5, BigDecimal.valueOf(total));
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creando boleta", e);
        }
        return -1;
    }

    public void agregarItem(int boletaId, String tipo, String descripcion, String codigoProducto, int cantidad, double precioUnitario) {
        try (Connection conn = DatabaseService.getConnection()) {
            agregarItem(conn, boletaId, tipo, descripcion, codigoProducto, cantidad, precioUnitario);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void agregarItem(Connection txConn, int boletaId, String tipo, String descripcion, String codigoProducto, int cantidad, double precioUnitario) {
        String sql = """
                INSERT INTO boleta_items (boleta_id, tipo, descripcion, codigo_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
            pstmt.setInt(1, boletaId);
            pstmt.setString(2, tipo);
            pstmt.setString(3, descripcion);
            pstmt.setString(4, codigoProducto);
            pstmt.setInt(5, cantidad);
            pstmt.setBigDecimal(6, BigDecimal.valueOf(precioUnitario));
            pstmt.setBigDecimal(7, BigDecimal.valueOf(precioUnitario).multiply(BigDecimal.valueOf(cantidad)));
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error agregando item", e);
        }
    }

    public List<BoletaResumen> buscarBoletasPorDni(String dniBuscado) {
        try (Connection conn = DatabaseService.getConnection()) {
            return buscarBoletasPorDni(conn, dniBuscado);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<BoletaResumen> buscarBoletasPorDni(Connection txConn, String dniBuscado) {
        List<BoletaResumen> lista = new ArrayList<>();
        String sql = "SELECT * FROM boletas WHERE dni = ? ORDER BY fecha DESC";

        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
            pstmt.setString(1, dniBuscado);
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
        try (Connection conn = DatabaseService.getConnection()) {
            return obtenerItems(conn, boletaId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<BoletaItem> obtenerItems(Connection txConn, int boletaId) {
        List<BoletaItem> lista = new ArrayList<>();
        String sql = """
                SELECT id, tipo, descripcion, codigo_producto, cantidad, precio_unitario,
                       (cantidad * precio_unitario) AS subtotal
                FROM boleta_items
                WHERE boleta_id = ?
                ORDER BY id ASC
                """;
        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
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
