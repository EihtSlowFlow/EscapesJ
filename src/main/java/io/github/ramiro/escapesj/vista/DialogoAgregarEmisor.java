package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Emisor;
import io.github.ramiro.escapesj.persistencia.EmisorRepository;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DialogoAgregarEmisor extends JDialog {

    private final EmisorRepository emisorRepository;
    private final Consumer<Emisor> onGuardado;

    private JTextField txtNombre;
    private JTextField txtCuit;
    private JTextField txtCalle;
    private JTextField txtTelefono;

    public DialogoAgregarEmisor(Window owner, EmisorRepository repo, Consumer<Emisor> onGuardado) {
        super(owner, "Agregar Nuevo Emisor", ModalityType.APPLICATION_MODAL);
        this.emisorRepository = repo;
        this.onGuardado = onGuardado;

        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtNombre = new JTextField(20);
        txtCuit = new JTextField(15);
        txtCalle = new JTextField(20);
        txtTelefono = new JTextField(15);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nombre o Razón Social:"), gbc);
        gbc.gridx = 1;
        panel.add(txtNombre, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("CUIT:"), gbc);
        gbc.gridx = 1;
        panel.add(txtCuit, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Calle/Ubicación:"), gbc);
        gbc.gridx = 1;
        panel.add(txtCalle, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx = 1;
        panel.add(txtTelefono, gbc);

        row++;
        gbc.gridy = row;
        gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JButton btnGuardar = new JButton("Guardar Emisor");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGuardar.addActionListener(e -> guardar());
        panel.add(btnGuardar, gbc);

        setContentPane(panel);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String cuit = txtCuit.getText().trim().replaceAll("[\\s\\-]", "");
        String calle = txtCalle.getText().trim();
        String telefono = txtTelefono.getText().trim().replaceAll("[\\s\\-]", "");

        if (nombre.isEmpty() || cuit.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre y CUIT son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Emisor nuevo = new Emisor(0, nombre, cuit, calle, telefono);
        nuevo = emisorRepository.guardar(nuevo);
        
        if (onGuardado != null) {
            onGuardado.accept(nuevo);
        }
        dispose();
    }
}
