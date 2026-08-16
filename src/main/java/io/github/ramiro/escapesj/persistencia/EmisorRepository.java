package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Emisor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmisorRepository {

    public List<Emisor> listarTodos() {
        List<Emisor> emisores = new ArrayList<>();
        String sql = "SELECT id, nombre, cuit, calle, telefono FROM emisores";
        
        try (Connection conn = DatabaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                emisores.add(new Emisor(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("cuit"),
                        rs.getString("calle"),
                        rs.getString("telefono")
                ));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error listando emisores", e);
        }
        return emisores;
    }

    public Emisor guardar(Emisor emisor) {
        String sql = "INSERT INTO emisores (nombre, cuit, calle, telefono) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, emisor.nombre());
            pstmt.setString(2, emisor.cuit());
            pstmt.setString(3, emisor.calle());
            pstmt.setString(4, emisor.telefono());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Emisor(
                        rs.getInt(1),
                        emisor.nombre(),
                        emisor.cuit(),
                        emisor.calle(),
                        emisor.telefono()
                    );
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar el emisor", e);
        }
        throw new PersistenceException("La base de datos no devolvió el identificador del emisor guardado", null);
    }
}
