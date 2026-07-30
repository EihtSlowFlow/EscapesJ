package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import java.awt.*;

public class VentanaModificarProducto extends JDialog {
    private final ProductoRepository repository;
    private final Producto productoOriginal;
    private JTextField txtCod, txtNom, txtDesc, txtPre, txtStock;
    private boolean actualizado = false;

    public VentanaModificarProducto(JFrame parent, Producto p, ProductoRepository repo) {
        super(parent, "Modificar: " + p.getNombre(), true);
        this.productoOriginal = p;
        this.repository = repo;
        initUI();
        cargarDatos();
    }

    private void initUI() {
        setSize(400, 500);
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(new Color(30, 35, 48)); // Un tono más oscuro para distinguir
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtCod = crearCampo("Código (Primary Key)", 0);
        txtNom = crearCampo("Nombre / Modelo", 2);
        txtDesc = crearCampo("Descripción Técnica", 4);
        txtPre = crearCampo("Precio Unitario", 6);
        txtStock = crearCampo("Stock Actual", 8);

        JButton btnGuardar = new JButton("Confirmar Cambios");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> procesarActualizacion());

        gbc.gridy = 10;
        add(btnGuardar, gbc);
    }

    private void cargarDatos() {
        txtCod.setText(productoOriginal.getCodigo());
        txtNom.setText(productoOriginal.getNombre());
        txtDesc.setText(productoOriginal.getDescripcion());
        txtPre.setText(String.valueOf(productoOriginal.getPrecio()));
        txtStock.setText(String.valueOf(productoOriginal.getStock()));
    }

    private void procesarActualizacion() {
        try {
            Producto nuevo = new Producto(
                    txtCod.getText().trim(),
                    txtNom.getText().trim(),
                    txtDesc.getText().trim(),
                    new java.math.BigDecimal(txtPre.getText().trim()),
                    Integer.parseInt(txtStock.getText().trim())
            );
            repository.actualizarConCambioDeCodigo(nuevo, productoOriginal.getCodigo());
            actualizado = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Verifique los datos numéricos.");
        }
    }

    private JTextField crearCampo(String l, int y) {
        JLabel lbl = new JLabel(l);
        lbl.setForeground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 15, 0, 15);
        add(lbl, gbc);

        JTextField f = new JTextField(15);
        f.setBackground(new Color(45, 52, 71));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        gbc.gridy = y + 1;
        gbc.insets = new Insets(0, 15, 5, 15);
        add(f, gbc);
        return f;
    }

    public boolean isActualizado() {
        return actualizado;
    }
}