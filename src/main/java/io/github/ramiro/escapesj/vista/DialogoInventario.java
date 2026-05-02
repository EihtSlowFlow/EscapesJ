package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.modelo.Producto;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

public class DialogoInventario extends JDialog {
    public DialogoInventario(JFrame parent, Inventario inventario, Consumer<Producto> alSeleccionar) {
        super(parent, "Seleccionar Producto", true);
        setSize(400, 500);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(45, 52, 71));

        // En un futuro, aquí el inventario cargará sus productos en una lista/tabla
        DefaultListModel<Producto> modeloLista = new DefaultListModel<>();
        // El inventario debería tener un método para volcar sus productos aquí

        JList<Producto> lista = new JList<>(modeloLista);
        lista.setBackground(new Color(30, 35, 48));
        lista.setForeground(Color.WHITE);

        JButton btnSeleccionar = new JButton("Confirmar Selección");
        btnSeleccionar.addActionListener(e -> {
            Optional.ofNullable(lista.getSelectedValue()).ifPresent(alSeleccionar);
            dispose();
        });

        add(new JScrollPane(lista), BorderLayout.CENTER);
        add(btnSeleccionar, BorderLayout.SOUTH);
    }
}