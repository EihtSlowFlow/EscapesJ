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

public class VentanaLogin extends JFrame {
    private final AfipService afip;
    private final Inventario inv;
    private final ProductoRepository prodRepo;
    private final ServicioRepository servRepo;
    private final UsuarioRepository usuarioRepo;
    private final ConfigRepository configRepo;
    private final BoletaRepository boletaRepo;
    private final PresupuestoRepository presupuestoRepo;

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public VentanaLogin(AfipService afip, Inventario inv, ProductoRepository prodRepo,
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
        setTitle("EscapesJ - Acceso");
        setSize(450, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout());

        // 1. Logo arriba
        add(new PanelCabecera(), BorderLayout.NORTH);

        // 2. Formulario
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblU = new JLabel("Usuario:");
        lblU.setForeground(Color.WHITE);
        pnlForm.add(lblU, gbc);

        txtUsuario = new JTextField(15);
        estilizarCampo(txtUsuario); // Aplicamos el estilo oscuro
        gbc.gridx = 1;
        pnlForm.add(txtUsuario, gbc);

        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblP = new JLabel("Contraseña:");
        lblP.setForeground(Color.WHITE);
        pnlForm.add(lblP, gbc);

        txtPassword = new JPasswordField(15);
        estilizarCampo(txtPassword); // Aplicamos el estilo oscuro
        gbc.gridx = 1;
        pnlForm.add(txtPassword, gbc);

        // Botón
        JButton btnIngresar = new JButton("Ingresar");
        btnIngresar.setBackground(new Color(231, 76, 60));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        pnlForm.add(btnIngresar, gbc);

        // Link "Olvidaste tu contraseña?"
        JButton btnOlvide = new JButton("¿Olvidaste tu contraseña?");
        btnOlvide.setContentAreaFilled(false);
        btnOlvide.setBorderPainted(false);
        btnOlvide.setForeground(new Color(150, 150, 150));
        btnOlvide.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnOlvide.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOlvide.addActionListener(e -> new VentanaRecuperacion(VentanaLogin.this, usuarioRepo).setVisible(true));
        
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 10, 10, 10);
        pnlForm.add(btnOlvide, gbc);

        add(pnlForm, BorderLayout.CENTER);

        // CONFIGURACIÓN DE UX
        this.getRootPane().setDefaultButton(btnIngresar); // Enter para entrar

        // Foco automático al abrir la ventana
        SwingUtilities.invokeLater(() -> txtUsuario.requestFocusInWindow());

        btnIngresar.addActionListener(e -> validarAcceso());
    }

    /**
     * Corrige el problema de visibilidad: Fondo oscuro, texto blanco y cursor blanco
     */
    private void estilizarCampo(JTextField campo) {
        campo.setBackground(new Color(45, 52, 71)); // Gris azulado oscuro
        campo.setForeground(Color.WHITE);           // Texto blanco
        campo.setCaretColor(Color.WHITE);            // Cursor blanco
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createLineBorder(new Color(70, 80, 105)));
    }

    private void validarAcceso() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuarioRepo.validarCredenciales(usuario, password)) {
            if (usuarioRepo.debeCambiarPassword(usuario)) {
                JOptionPane.showMessageDialog(this, "Por motivos de seguridad, debe cambiar su contraseña generada por defecto/migración.", "Cambio de Contraseña Obligatorio", JOptionPane.WARNING_MESSAGE);
                JPasswordField pf1 = new JPasswordField();
                JPasswordField pf2 = new JPasswordField();
                Object[] message = {
                    "Nueva Contraseña:", pf1,
                    "Confirmar Nueva Contraseña:", pf2
                };
                int option = JOptionPane.showConfirmDialog(this, message, "Cambio de Contraseña", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (option == JOptionPane.OK_OPTION) {
                    String p1 = new String(pf1.getPassword());
                    String p2 = new String(pf2.getPassword());
                    if (p1.isEmpty() || !p1.equals(p2)) {
                        JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden o están vacías. No se pudo iniciar sesión.");
                        return; // Aborts login
                    }
                    if (p1.length() < 6) {
                        JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres. No se pudo iniciar sesión.");
                        return; // Aborts login
                    }
                    // Attempt to change
                    if (usuarioRepo.cambiarPassword(usuario, password, p1)) {
                        JOptionPane.showMessageDialog(this, "Contraseña actualizada con éxito.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Hubo un error al actualizar la contraseña.");
                        return; // Aborts login
                    }
                } else {
                    return; // Aborts login if they hit cancel
                }
            }

            new VentanaMenu(afip, inv, prodRepo, servRepo, usuarioRepo, configRepo, boletaRepo, presupuestoRepo).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error de acceso: Credenciales inválidas.");
        }
    }

    private class PanelCabecera extends JPanel {
        private Image logo;

        public PanelCabecera() {
            setPreferredSize(new Dimension(0, 200));
            setOpaque(false);
            URL logoUrl = getClass().getResource("/Logo.png");
            if (logoUrl != null) {
                logo = new ImageIcon(logoUrl).getImage();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (logo != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(logo, (getWidth() - 180) / 2, 20, 180, 180, this);
                g2d.dispose();
            }
        }
    }
}