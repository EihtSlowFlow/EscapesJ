package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.modelo.ProductoRepresentador;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoRepository {
    private final Connection conn;

    public ProductoRepository(Connection conn) {
        this.conn = conn;
    }

    public List<Producto> buscarTodos() {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productos.add(new Producto(
                        rs.getString("codigo"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productos;
    }

    public void guardar(Producto producto) {
        String sql = "INSERT INTO productos (codigo, descripcion, precio) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion), precio = VALUES(precio)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // El producto se "exporta" a la query sin getters
            producto.presentarseEn(new ProductoRepresentador() {
                public void definirCodigo(String c) {
                    try {
                        pstmt.setString(1, c);
                    } catch (Exception ignored) {
                    }
                }

                public void definirDescripcion(String d) {
                    try {
                        pstmt.setString(2, d);
                    } catch (Exception ignored) {
                    }
                }

                public void definirPrecio(double p) {
                    try {
                        pstmt.setDouble(3, p);
                    } catch (Exception ignored) {
                    }
                }
            });
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminar(String codigo) {
        String sql = "DELETE FROM productos WHERE codigo = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}