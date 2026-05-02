package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.sdk.AfipService;

import javax.swing.*;
import java.awt.*;

public class VentanaMenu extends JFrame {
    private final AfipService afipService;
    private final Inventario inventario;
    private final ProductoRepository repo;

    public VentanaMenu(AfipService afipService, Inventario inventario, ProductoRepository repo) {
        this.afipService = afipService;
        this.inventario = inventario;
        this.repo = repo; // Recibimos el repo
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Menú Principal");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));

        setLayout(new BorderLayout());

        // Logo grande central
        JLabel lblLogo = new JLabel(new ImageIcon("/home/ramiro/Documentos/escapesJ/Logo.png"));
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblLogo, BorderLayout.CENTER);

        // Panel de acciones
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        pnlBotones.setOpaque(false);

        JButton btnInventario = crearBoton("Gestionar Inventario");
        JButton btnVenta = crearBoton("Registrar Venta");
        btnInventario.addActionListener(e -> {
            // AQUÍ ES DONDE SE "USA" LA CLASE
            // Si esta línea no existe o está comentada, IntelliJ dirá "never used"
            new VentanaGestionInventario(repo).setVisible(true);
        });
        // CONEXIÓN: Al clickear, abre la VentanaPrincipal
        btnVenta.addActionListener(e -> {
            // Pasamos el repo que el Menú ya conoce a la Ventana Principal
            new VentanaPrincipal(this.afipService, this.inventario, this.repo).setVisible(true);
        });

        pnlBotones.add(btnInventario);
        pnlBotones.add(btnVenta);
        add(pnlBotones, BorderLayout.SOUTH);
    }

    private JButton crearBoton(String t) {
        JButton b = new JButton(t);
        b.setPreferredSize(new Dimension(200, 60));
        b.setBackground(new Color(45, 52, 71));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setBorder(BorderFactory.createLineBorder(new Color(237, 28, 36), 2));
        return b;
    }
}