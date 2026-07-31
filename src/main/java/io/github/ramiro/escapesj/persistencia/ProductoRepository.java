package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Producto;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProductoRepository.class);


    public ProductoRepository() {
    }

    /**
     * Guarda un producto nuevo o actualiza uno existente si el código ya existe.
     */
    public void guardar(Producto p) {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, precio, stock) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(codigo) DO UPDATE SET nombre=excluded.nombre, descripcion=excluded.descripcion, precio=excluded.precio, stock=excluded.stock";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setLong(4, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(p.getPrecio()));
            ps.setInt(5, p.getStock());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
        }
    }

    /**
     * Permite modificar incluso el código (llave primaria) usando el código anterior como referencia.
     */
    public void actualizarConCambioDeCodigo(Producto producto, String viejoCodigo) {
        String sql = "UPDATE productos SET codigo=?, nombre=?, descripcion=?, precio=?, stock=? WHERE codigo=?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, producto.getCodigo());
            pstmt.setString(2, producto.getNombre());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setLong(4, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(producto.getPrecio()));
            pstmt.setInt(5, producto.getStock());
            pstmt.setString(6, viejoCodigo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
        }
    }

    /**
     * Busca un producto específico por su código.
     * Este es el método que te faltaba para el buscador.
     */
    public Optional<Producto> buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("precio")),
                        rs.getInt("stock")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
        }
        return Optional.empty();
    }

    /**
     * Trae la lista completa de productos para las tablas de gestión.
     */
    public List<Producto> buscarTodos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos";
        try (Connection connection = DatabaseService.getConnection();
             Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        io.github.ramiro.escapesj.sdk.DineroUtil.desdeCentavos(rs.getLong("precio")),
                        rs.getInt("stock")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
        }
        return lista;
    }

    /**
     * Verifica si hay stock disponible sin descontarlo.
     */
    public boolean verificarStockDisponible(String codigo, int cantidad) {
        String sql = "SELECT stock FROM productos WHERE codigo = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock") >= cantidad;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Resta unidades del inventario al confirmar una venta.
     */
    public boolean intentarRestarStock(String codigo, int cantidad) {
        try (Connection connection = DatabaseService.getConnection()) {
            return intentarRestarStock(connection, codigo, cantidad);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean intentarRestarStock(Connection txConn, String codigo, int cantidad) {
        String sql = "UPDATE productos SET stock = stock - ? WHERE codigo = ? AND stock >= ?";
        try (PreparedStatement ps = txConn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setString(2, codigo);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean actualizarStock(String codigo, int nuevoStock) {
        String sql = "UPDATE productos SET stock = ? WHERE codigo = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setString(2, codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error:", e);
            return false;
        }
    }

    /**
     * Elimina un producto.
     */
    public boolean eliminarProducto(String codigo) {
        String sql = "DELETE FROM productos WHERE codigo = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error:", e);
            return false;
        }
    }

    /**
     * Restaura unidades al inventario (al quitar un ítem de la orden).
     */
    public void sumarStock(String codigo, int cantidad) {
        String sql = "UPDATE productos SET stock = stock + ? WHERE codigo = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setString(2, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
        }
    }
}
