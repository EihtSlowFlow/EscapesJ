package io.github.ramiro.escapesj.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BoletaRepository {
    private final Connection conexion;

    public BoletaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public record BoletaResumen(int id, int numero, String dni, String nombreCliente, String fecha, double total) {}
    public record BoletaItem(int id, String tipo, String descripcion, String codigoProducto, int cantidad, double precioUnitario, double subtotal) {}

    private int siguienteNumero(Connection txConn) {
        String sql = "SELECT MAX(numero) FROM boletas";
        try (Statement stmt = txConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return max > 0 ? max + 1 : 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public int crearBoleta(String dni, String nombreCliente, String fecha, double total) {
        return crearBoleta(this.conexion, dni, nombreCliente, fecha, total);
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
            pstmt.setDouble(5, total);
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
        agregarItem(this.conexion, boletaId, tipo, descripcion, codigoProducto, cantidad, precioUnitario);
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
            pstmt.setDouble(6, precioUnitario);
            pstmt.setDouble(7, cantidad * precioUnitario);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error agregando item", e);
        }
    }

    public List<BoletaResumen> buscarBoletasPorDni(String dniBuscado) {
        return buscarBoletasPorDni(this.conexion, dniBuscado);
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
                            rs.getDouble("total")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<BoletaItem> obtenerItems(int boletaId) {
        return obtenerItems(this.conexion, boletaId);
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
                            rs.getDouble("precio_unitario"),
                            rs.getDouble("subtotal")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
