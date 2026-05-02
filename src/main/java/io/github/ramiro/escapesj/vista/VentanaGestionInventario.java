package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.modelo.ProductoRepresentador;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VentanaGestionInventario extends JFrame {
    private final ProductoRepository repository;
    private DefaultTableModel model;
    private JTextField txtCod, txtDesc, txtPre;

    public VentanaGestionInventario(ProductoRepository repository) {
        this.repository = repository;
        initUI();
        actualizarTabla();
    }

    private void initUI() {
        setTitle("EscapesJ - Gestión de Inventario");
        setSize(800, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // No cierra el programa
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));

        setLayout(new BorderLayout(10, 10));

        // 1. Tabla de Productos
        model = new DefaultTableModel(new Object[]{"Código", "Descripción", "Precio"}, 0);
        JTable tabla = new JTable(model);
        estilizarTabla(tabla);
        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.getViewport().setBackground(new Color(0, 43, 91));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // 2. Formulario Lateral
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        pnlForm.setPreferredSize(new Dimension(300, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtCod = crearCampo("Código", pnlForm, gbc, 0);
        txtDesc = crearCampo("Descripción", pnlForm, gbc, 2);
        txtPre = crearCampo("Precio", pnlForm, gbc, 4);

        JButton btnGuardar = new JButton("Guardar / Actualizar");
        estilizarBoton(btnGuardar, new Color(46, 204, 113));
        btnGuardar.addActionListener(e -> guardarProducto());
        gbc.gridy = 6;
        add(btnGuardar); // Simplificado para el ejemplo
        pnlForm.add(btnGuardar, gbc);

        JButton btnEliminar = new JButton("Eliminar Seleccionado");
        estilizarBoton(btnEliminar, new Color(231, 76, 60));
        btnEliminar.addActionListener(e -> eliminarProducto(tabla));
        gbc.gridy = 7;
        pnlForm.add(btnEliminar, gbc);

        add(pnlForm, BorderLayout.EAST);
    }

    private void actualizarTabla() {
        model.setRowCount(0);
        repository.buscarTodos().forEach(p -> {
            p.presentarseEn(new ProductoRepresentador() {
                public void definirCodigo(String c) {
                    model.addRow(new Object[]{c, "", 0});
                }

                public void definirDescripcion(String d) {
                    model.setValueAt(d, model.getRowCount() - 1, 1);
                }

                public void definirPrecio(double p) {
                    model.setValueAt(p, model.getRowCount() - 1, 2);
                }
            });
        });
    }

    private void guardarProducto() {
        Producto p = new Producto(txtCod.getText(), txtDesc.getText(), Double.parseDouble(txtPre.getText()));
        repository.guardar(p);
        actualizarTabla();
        limpiarCampos();
    }

    private void eliminarProducto(JTable t) {
        int row = t.getSelectedRow();
        if (row != -1) {
            String cod = model.getValueAt(row, 0).toString();
            repository.eliminar(cod);
            actualizarTabla();
        }
    }

    private JTextField crearCampo(String textoLabel, JPanel p, GridBagConstraints g, int y) {
        // 1. El Label (Ya lo tenemos en blanco)
        JLabel label = new JLabel(textoLabel);
        label.setForeground(Color.WHITE);
        g.gridy = y;
        p.add(label, g);

        // 2. El Cuadro de Texto (JTextField)
        JTextField f = new JTextField();

        // Color de fondo: Un toque más claro que el azul marino (RGB 45, 52, 71)
        f.setBackground(new Color(45, 52, 71));

        // Color de letra y cursor: Blanco puro para que se lea perfecto
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);

        // EL TRUCO: Un borde gris claro para que se vea el cuadro
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 110, 130), 1), // Borde exterior
                BorderFactory.createEmptyBorder(5, 5, 5, 5)                  // Margen interno para el texto
        ));

        g.gridy = y + 1;
        p.add(f, g);
        return f;
    }

    private void estilizarBoton(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setRowHeight(25);
    }

    private void limpiarCampos() {
        txtCod.setText("");
        txtDesc.setText("");
        txtPre.setText("");
    }
}