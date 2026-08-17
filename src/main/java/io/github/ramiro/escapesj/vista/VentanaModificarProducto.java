package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import java.awt.*;

public class VentanaModificarProducto extends JDialog {
    private final ProductoRepository repository;
    private final Producto productoOriginal;
    private JPanel content;
    private JTextField txtCod, txtNom, txtDesc, txtPre, txtCosto, txtStock;
    private boolean actualizado = false;

    public VentanaModificarProducto(JFrame parent, Producto p, ProductoRepository repo) {
        super(parent, "Modificar: " + p.getNombre(), true);
        this.productoOriginal = p;
        this.repository = repo;
        initUI();
        cargarDatos();
    }

    private void initUI() {
        content = new JPanel(new GridBagLayout());
        content.setBackground(new Color(30, 35, 48)); // Un tono más oscuro para distinguir
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtCod = crearCampo("Código (Primary Key)", 0);
        txtNom = crearCampo("Nombre / Modelo", 2);
        txtDesc = crearCampo("Descripción Técnica", 4);
        txtPre = crearCampo("Precio Unitario", 6);
        txtCosto = crearCampo("Costo Unitario (Vacío = No configurado)", 8);
        txtStock = crearCampo("Stock Actual", 10);

        JButton btnGuardar = new JButton("Confirmar Cambios");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> procesarActualizacion());

        gbc.gridy = 12;
        content.add(btnGuardar, gbc);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(0, 43, 91));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        setContentPane(scrollPane);
        ZoomManager.packAndFitToScreen(this, 400, 500);
    }

    private void cargarDatos() {
        txtCod.setText(productoOriginal.getCodigo());
        txtNom.setText(productoOriginal.getNombre());
        txtDesc.setText(productoOriginal.getDescripcion());
        txtPre.setText(String.valueOf(productoOriginal.getPrecio()));
        txtCosto.setText(productoOriginal.getCostoUnitario() == null ? "" : String.valueOf(productoOriginal.getCostoUnitario()));
        txtStock.setText(String.valueOf(productoOriginal.getStock()));
    }

    private void procesarActualizacion() {
        try {
            java.math.BigDecimal precio = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(txtPre.getText());
            if (precio.compareTo(java.math.BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");

            String costoStr = txtCosto.getText().trim();
            java.math.BigDecimal costo = null;
            if (!costoStr.isEmpty()) {
                costo = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(costoStr);
                if (costo.compareTo(java.math.BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El costo no puede ser negativo.");
            }
            int stock = Integer.parseInt(txtStock.getText().trim());
            if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");

            Producto nuevo = new Producto(
                    txtCod.getText().trim(),
                    txtNom.getText().trim(),
                    txtDesc.getText().trim(),
                    precio,
                    stock,
                    costo
            );
            repository.actualizarConCambioDeCodigo(nuevo, productoOriginal.getCodigo());
            actualizado = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Verifique los formatos de precio y stock.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "modificar producto", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error general.");
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
        content.add(lbl, gbc);

        JTextField f = new JTextField(15);
        f.setBackground(new Color(45, 52, 71));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        gbc.gridy = y + 1;
        gbc.insets = new Insets(0, 15, 5, 15);
        content.add(f, gbc);
        return f;
    }

    public boolean isActualizado() {
        return actualizado;
    }
}
