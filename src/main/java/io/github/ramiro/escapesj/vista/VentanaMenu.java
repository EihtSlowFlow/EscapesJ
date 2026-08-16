package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.PresupuestoRepository;
import io.github.ramiro.escapesj.persistencia.ServicioRepository;
import io.github.ramiro.escapesj.persistencia.UsuarioRepository;
import io.github.ramiro.escapesj.sdk.AfipService;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class VentanaMenu extends JFrame {
    private final AfipService afip;
    private final Inventario inv;
    private final ProductoRepository prodRepo;
    private final ServicioRepository servRepo;
    private final UsuarioRepository usuarioRepo;
    private final ConfigRepository configRepo;
    private final BoletaRepository boletaRepo;
    private final PresupuestoRepository presupuestoRepo;

    public VentanaMenu(AfipService afip, Inventario inv, ProductoRepository prodRepo,
                       ServicioRepository servRepo, UsuarioRepository usuarioRepo,
                       ConfigRepository configRepo, BoletaRepository boletaRepo,
                       PresupuestoRepository presupuestoRepo) {
        this.afip = afip;
        this.inv = inv;
        this.prodRepo = prodRepo;
        this.servRepo = servRepo;
        this.usuarioRepo = usuarioRepo;
        this.configRepo = configRepo;
        this.boletaRepo = boletaRepo;
        this.presupuestoRepo = presupuestoRepo;
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Menú Principal");
        setSize(550, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 1. EL LOGO EN EL CENTRO
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new PanelLogoGrande(), gbc);

        // 2. LOS BOTONES EN EL CENTRO INFERIOR
        JPanel pnlBotones = new JPanel(new GridLayout(5, 1, 10, 10));
        pnlBotones.setOpaque(false);
        pnlBotones.setPreferredSize(new Dimension(360, 300));

        JButton btnVenta = crearBotonMenu("Registrar Venta / Servicio", new Color(231, 76, 60));
        JButton btnPresupuesto = crearBotonMenu("Generar Presupuesto", new Color(155, 89, 182));
        JButton btnInv = crearBotonMenu("Gestionar Inventario", new Color(52, 152, 219));
        JButton btnServ = crearBotonMenu("Gestionar Servicios (Historial)", new Color(46, 204, 113));
        JButton btnConfig = crearBotonMenu("Configuración", new Color(149, 165, 166));

        btnVenta.addActionListener(e -> new VentanaPrincipal(afip, inv, prodRepo, servRepo, boletaRepo, configRepo).setVisible(true));
        btnPresupuesto.addActionListener(e -> new VentanaPresupuesto(afip, presupuestoRepo, prodRepo, configRepo).setVisible(true));
        btnInv.addActionListener(e -> new VentanaGestionInventario(prodRepo).setVisible(true));
        btnServ.addActionListener(e -> new VentanaGestionServicios(servRepo, boletaRepo).setVisible(true));
        btnConfig.addActionListener(e -> new VentanaConfiguracion(configRepo, usuarioRepo).setVisible(true));

        pnlBotones.add(btnVenta);
        pnlBotones.add(btnPresupuesto);
        pnlBotones.add(btnInv);
        pnlBotones.add(btnServ);
        pnlBotones.add(btnConfig);

        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 40, 0);
        gbc.anchor = GridBagConstraints.PAGE_END;
        add(pnlBotones, gbc);
    }

    private JButton crearBotonMenu(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        return b;
    }

    private class PanelLogoGrande extends JPanel {
        private Image logo;

        public PanelLogoGrande() {
            setPreferredSize(new Dimension(350, 350));
            setOpaque(false);
            URL logoUrl = getClass().getResource("/Logo.png");
            if (logoUrl != null) {
                logo = new ImageIcon(logoUrl).getImage();
            } else {
                System.err.println("WARN: No se encontró /Logo.png en el classpath.");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (logo != null) {
                int size = Math.min(getWidth(), getHeight()) - 40;
                size = Math.min(size, 300);
                if (size > 0) {
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;
                    g.drawImage(logo, x, y, size, size, this);
                }
            }
        }
    }
}