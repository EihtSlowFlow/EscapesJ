package io.github.ramiro.escapesj.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para presupuestos: creación, búsqueda y validación por código único.
 */
public class PresupuestoRepository {
    private final Connection conexion;

    public PresupuestoRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public record Presupuesto(int id, String codigoUnico, String dniCliente, String nombreCliente,
                               String descripcionTrabajo, double montoEstimado,
                               String fechaEmision, String fechaLimite) {}

    /**
     * Genera el siguiente código único con formato PRE-XXXX.
     */
    private String generarCodigoUnico() {
        String sql = "SELECT MAX(id) FROM presupuestos";
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return String.format("PRE-%04d", max + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "PRE-0001";
    }

    /**
     * Crea un nuevo presupuesto y devuelve el código único generado.
     */
    public String crearPresupuesto(String dniCliente, String nombreCliente,
                                    String descripcionTrabajo, double montoEstimado,
                                    String fechaEmision, String fechaLimite) {
        String codigo = generarCodigoUnico();
        String sql = """
                INSERT INTO presupuestos (codigo_unico, dni_cliente, nombre_cliente,
                    descripcion_trabajo, monto_estimado, fecha_emision, fecha_limite)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            pstmt.setString(2, dniCliente);
            pstmt.setString(3, nombreCliente);
            pstmt.setString(4, descripcionTrabajo);
            pstmt.setDouble(5, montoEstimado);
            pstmt.setString(6, fechaEmision);
            pstmt.setString(7, fechaLimite);
            pstmt.executeUpdate();
            return codigo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Busca un presupuesto por su código único (para verificación).
     */
    public Presupuesto buscarPorCodigo(String codigoUnico) {
        String sql = """
                SELECT id, codigo_unico, dni_cliente, nombre_cliente,
                       descripcion_trabajo, monto_estimado, fecha_emision, fecha_limite
                FROM presupuestos WHERE codigo_unico = ?
                """;
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, codigoUnico);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Presupuesto(
                            rs.getInt("id"),
                            rs.getString("codigo_unico"),
                            rs.getString("dni_cliente"),
                            rs.getString("nombre_cliente"),
                            rs.getString("descripcion_trabajo"),
                            rs.getDouble("monto_estimado"),
                            rs.getString("fecha_emision"),
                            rs.getString("fecha_limite")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca todos los presupuestos de un cliente por DNI.
     */
    public List<Presupuesto> buscarPorDni(String dni) {
        List<Presupuesto> lista = new ArrayList<>();
        String sql = """
                SELECT id, codigo_unico, dni_cliente, nombre_cliente,
                       descripcion_trabajo, monto_estimado, fecha_emision, fecha_limite
                FROM presupuestos WHERE dni_cliente = ? ORDER BY fecha_emision DESC
                """;
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, dni);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Presupuesto(
                            rs.getInt("id"),
                            rs.getString("codigo_unico"),
                            rs.getString("dni_cliente"),
                            rs.getString("nombre_cliente"),
                            rs.getString("descripcion_trabajo"),
                            rs.getDouble("monto_estimado"),
                            rs.getString("fecha_emision"),
                            rs.getString("fecha_limite")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
