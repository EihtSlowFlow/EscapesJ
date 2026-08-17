package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.sdk.DineroUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OperacionHistoricaRepository {
    private static final Logger logger = LoggerFactory.getLogger(OperacionHistoricaRepository.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OperacionHistoricaRepository() {
    }

    public void guardar(OperacionHistorica op) {
        if (op.getId() == 0) {
            insertar(op);
        } else {
            actualizar(op);
        }
    }

    private void insertar(OperacionHistorica op) {
        String sql = """
            INSERT INTO operaciones_historicas (
                fecha, referencia_papel, cliente, descripcion,
                importe_total_centavos, costo_materiales_centavos,
                observaciones, estado, boleta_digital_id,
                creado_en, actualizado_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        String ahora = LocalDateTime.now().format(formatter);
        op.setCreadoEn(ahora);
        op.setActualizadoEn(ahora);

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, op.getFecha());
            ps.setString(2, op.getReferenciaPapel());
            ps.setString(3, op.getCliente());
            ps.setString(4, op.getDescripcion());
            ps.setLong(5, DineroUtil.aCentavos(op.getImporteTotal()));
            
            if (op.getCostoMateriales() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setLong(6, DineroUtil.aCentavos(op.getCostoMateriales()));
            }

            ps.setString(7, op.getObservaciones());
            ps.setString(8, op.getEstado());

            if (op.getBoletaDigitalId() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, op.getBoletaDigitalId());
            }

            ps.setString(10, op.getCreadoEn());
            ps.setString(11, op.getActualizadoEn());

            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    op.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al guardar operación histórica", e);
        }
    }

    private void actualizar(OperacionHistorica op) {
        String sql = """
            UPDATE operaciones_historicas SET
                fecha = ?, referencia_papel = ?, cliente = ?, descripcion = ?,
                importe_total_centavos = ?, costo_materiales_centavos = ?,
                observaciones = ?, estado = ?, boleta_digital_id = ?,
                actualizado_en = ?
            WHERE id = ?
        """;

        String ahora = LocalDateTime.now().format(formatter);
        op.setActualizadoEn(ahora);

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, op.getFecha());
            ps.setString(2, op.getReferenciaPapel());
            ps.setString(3, op.getCliente());
            ps.setString(4, op.getDescripcion());
            ps.setLong(5, DineroUtil.aCentavos(op.getImporteTotal()));
            
            if (op.getCostoMateriales() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setLong(6, DineroUtil.aCentavos(op.getCostoMateriales()));
            }

            ps.setString(7, op.getObservaciones());
            ps.setString(8, op.getEstado());

            if (op.getBoletaDigitalId() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, op.getBoletaDigitalId());
            }

            ps.setString(10, op.getActualizadoEn());
            ps.setInt(11, op.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new PersistenceException("No se encontró la operación histórica a actualizar");
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al actualizar operación histórica", e);
        }
    }

    public List<OperacionHistorica> buscarPorRangoFechaYEstado(Connection txConn, String fechaInicio, String fechaFin, String estado) {
        List<OperacionHistorica> lista = new ArrayList<>();
        String sql = "SELECT o.*, b.numero as boleta_numero FROM operaciones_historicas o LEFT JOIN boletas b ON o.boleta_digital_id = b.id WHERE o.fecha >= ? AND o.fecha < ?";
        if (estado != null) {
            sql += " AND o.estado = ?";
        }
        sql += " ORDER BY o.fecha ASC";

        try (PreparedStatement ps = txConn.prepareStatement(sql)) {
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);
            if (estado != null) {
                ps.setString(3, estado);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearOperacion(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al buscar operaciones históricas por rango", e);
        }
        return lista;
    }

    public java.util.Optional<OperacionHistorica> buscarPorId(int id) {
        String sql = "SELECT o.*, b.numero as boleta_numero FROM operaciones_historicas o LEFT JOIN boletas b ON o.boleta_digital_id = b.id WHERE o.id = ?";
        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapearOperacion(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al buscar operación histórica por ID", e);
        }
        return java.util.Optional.empty();
    }

    public List<OperacionHistorica> buscarTodas() {
        List<OperacionHistorica> lista = new ArrayList<>();
        String sql = "SELECT o.*, b.numero as boleta_numero FROM operaciones_historicas o LEFT JOIN boletas b ON o.boleta_digital_id = b.id ORDER BY o.fecha DESC, o.id DESC";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                lista.add(mapearOperacion(rs));
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al buscar operaciones históricas", e);
        }
        return lista;
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM operaciones_historicas WHERE id = ?";
        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new PersistenceException("No se encontró la operación histórica a eliminar");
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al eliminar operación histórica", e);
        }
    }

    private OperacionHistorica mapearOperacion(ResultSet rs) throws SQLException {
        OperacionHistorica op = new OperacionHistorica();
        op.setId(rs.getInt("id"));
        op.setFecha(rs.getString("fecha"));
        op.setReferenciaPapel(rs.getString("referencia_papel"));
        op.setCliente(rs.getString("cliente"));
        op.setDescripcion(rs.getString("descripcion"));
        op.setImporteTotal(DineroUtil.desdeCentavos(rs.getLong("importe_total_centavos")));
        
        long costoCentavos = rs.getLong("costo_materiales_centavos");
        if (rs.wasNull()) {
            op.setCostoMateriales(null);
        } else {
            op.setCostoMateriales(DineroUtil.desdeCentavos(costoCentavos));
        }

        op.setObservaciones(rs.getString("observaciones"));
        op.setEstado(rs.getString("estado"));
        
        int boletaId = rs.getInt("boleta_digital_id");
        if (rs.wasNull()) {
            op.setBoletaDigitalId(null);
        } else {
            op.setBoletaDigitalId(boletaId);
        }

        try {
            int boletaNum = rs.getInt("boleta_numero");
            if (!rs.wasNull()) {
                op.setBoletaDigitalNumero(boletaNum);
            }
        } catch (SQLException ignore) {
            // Ignorar si la columna no existe en alguna query particular (aunque ahora en ambas debería estar)
        }

        op.setCreadoEn(rs.getString("creado_en"));
        op.setActualizadoEn(rs.getString("actualizado_en"));
        return op;
    }
}
