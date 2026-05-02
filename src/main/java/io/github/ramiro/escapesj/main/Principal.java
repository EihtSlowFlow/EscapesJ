package io.github.ramiro.escapesj.main;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.vista.VentanaLogin;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class Principal {
    public static void main(String[] args) {
        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("CheckBox.foreground", Color.WHITE);
        UIManager.put("RadioButton.foreground", Color.WHITE);
        Connection connection = DatabaseService.getConnection();

        ProductoRepository repo = new ProductoRepository(connection);

        AfipService afip = new AfipService();
        Inventario inv = new Inventario();

        SwingUtilities.invokeLater(() -> {
            new VentanaLogin(afip, inv, repo).setVisible(true);
        });
    }
}