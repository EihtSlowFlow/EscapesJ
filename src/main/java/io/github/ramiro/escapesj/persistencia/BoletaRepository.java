package io.github.ramiro.escapesj.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoletaRepository {
    private static final Logger logger = LoggerFactory.getLogger(BoletaRepository.class);

    public BoletaRepository() {
    }

    public record BoletaResumen(int id, int numero, String dni, String nombreCliente, String fecha, BigDecimal total) {}
    public record BoletaItem(int id, String tipo, String descripcion, String codigoProducto, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, BigDecimal costoUnitarioHistorico) {}

    private int siguienteNumero(Connection txConn) {
        String sql = "SELECT MAX(numero) FROM boletas";
        try (Statement stmt = txConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return max > 0 ? max + 1 : 1;
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al obtener siguiente número", e);
        }
        return 1;
    }

    public int crearBoleta(String dni, String nombreCliente, String fecha, BigDecimal total) {
        try (Connection conn = DatabaseService.getConnection()) {
            return crearBoleta(conn, dni, nombreCliente, fecha, total);
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error creando boleta", e);
        }
    }

    public int crearBoleta(Connection txConn, String dni, String nombreCliente, String fecha, BigDecimal total) {
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
            pstmt.setLong(5, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(total));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error creando boleta", e);
        }
        return -1;
    }

    public void agregarItem(int boletaId, String tipo, String descripcion, String codigoProducto, int cantidad, BigDecimal precioUnitario, BigDecimal costoUnitarioHistorico) {
        try (Connection conn = DatabaseService.getConnection()) {
            agregarItem(conn, boletaId, tipo, descripcion, codigoProducto, cantidad, precioUnitario, costoUnitarioHistorico);
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error agregando item", e);
        }
    }

    public void agregarItem(Connection txConn, int boletaId, String tipo, String descripcion, String codigoProducto, int cantidad, BigDecimal precioUnitario, BigDecimal costoUnitarioHistorico) {
        String sql = """
                INSERT INTO boleta_items (boleta_id, tipo, descripcion, codigo_producto, cantidad, precio_unitario, subtotal, costo_unitario_historico_centavos)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
            pstmt.setInt(1, boletaId);
            pstmt.setString(2, tipo);
            pstmt.setString(3, descripcion);
            pstmt.setString(4, codigoProducto);
            pstmt.setInt(5, cantidad);
            pstmt.setLong(6, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(precioUnitario));
            pstmt.setLong(7, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad))));
            if (costoUnitarioHistorico == null) {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(8, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(costoUnitarioHistorico));
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error agregando item", e);
        }
    }

    public List<BoletaResumen> buscarBoletasPorDni(String dniBuscado) {
        try (Connection conn = DatabaseService.getConnection()) {
            return buscarBoletasPorDni(conn, dniBuscado);
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error buscando boletas", e);
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
                            io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("total"))
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error buscando boletas", e);
        }
        return lista;
    }

    public List<BoletaResumen> obtenerBoletasPorRango(Connection txConn, String fechaInicio, String fechaFin) {
        List<BoletaResumen> lista = new ArrayList<>();
        String sql = "SELECT * FROM boletas WHERE fecha >= ? AND fecha < ? ORDER BY fecha ASC";

        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
            pstmt.setString(1, fechaInicio);
            pstmt.setString(2, fechaFin);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new BoletaResumen(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getString("dni"),
                            rs.getString("nombre_cliente"),
                            rs.getString("fecha"),
                            io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("total"))
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error obteniendo boletas por rango", e);
        }
        return lista;
    }

    public List<BoletaItem> obtenerItems(int boletaId) {
        try (Connection conn = DatabaseService.getConnection()) {
            return obtenerItems(conn, boletaId);
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error obteniendo items", e);
        }
    }

    public List<BoletaItem> obtenerItems(Connection txConn, int boletaId) {
        List<BoletaItem> lista = new ArrayList<>();
        String sql = """
                SELECT id, tipo, descripcion, codigo_producto, cantidad, precio_unitario,
                       (cantidad * precio_unitario) AS subtotal, costo_unitario_historico_centavos
                FROM boleta_items
                WHERE boleta_id = ?
                ORDER BY id ASC
                """;
        try (PreparedStatement pstmt = txConn.prepareStatement(sql)) {
            pstmt.setInt(1, boletaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long costoHistoricoCentavos = rs.getLong("costo_unitario_historico_centavos");
                    BigDecimal costoHistorico = rs.wasNull() ? null : io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(costoHistoricoCentavos);

                    lista.add(new BoletaItem(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("descripcion"),
                            rs.getString("codigo_producto"),
                            rs.getInt("cantidad"),
                            io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("precio_unitario")),
                            io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("subtotal")),
                            costoHistorico
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error obteniendo items", e);
        }
        return lista;
    }
}
