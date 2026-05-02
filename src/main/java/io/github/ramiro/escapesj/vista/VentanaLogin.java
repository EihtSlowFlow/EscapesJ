package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.sdk.AfipService;

import javax.swing.*;
import java.awt.*;

public class VentanaLogin extends JFrame {
    private final AfipService afipService;
    private final Inventario inventario;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private final ProductoRepository repo;

    public VentanaLogin(AfipService afip, Inventario inv, ProductoRepository repo) {
        this.afipService = afip;
        this.inventario = inv;
        this.repo = repo;
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Acceso");
        setSize(400, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91)); // Azul Logo

        // Usamos GridBagLayout para evitar que los componentes se estiren
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;
        // Establece que al presionar ENTER en cualquier lado de la ventana, se pulse este botón
        // 1. Cabecera con Logo (Marca de agua sutil)[cite: 1]
        PanelCabecera cabecera = new PanelCabecera();
        cabecera.setLayout(new BorderLayout());
        JLabel lblAcceso = new JLabel("ACCESO", SwingConstants.CENTER);
        lblAcceso.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblAcceso.setForeground(Color.WHITE);
        cabecera.add(lblAcceso, BorderLayout.SOUTH);
        gbc.gridy = 0;
        mainPanel.add(cabecera, gbc);

        // 2. Campo Usuario
        gbc.gridy = 1;
        mainPanel.add(crearLabelBlanco("Usuario:"), gbc);

        txtUsuario = new JTextField();
        estilizarCaja(txtUsuario, "ingrese su usuario");
        gbc.gridy = 2;
        mainPanel.add(txtUsuario, gbc);

        // 3. Campo Contraseña
        gbc.gridy = 3;
        mainPanel.add(crearLabelBlanco("Contraseña:"), gbc);

        txtPassword = new JPasswordField();
        estilizarCaja(txtPassword, "ingrese su contraseña");
        txtPassword.setEchoChar((char) 0); // Texto visible inicialmente para el placeholder
        gbc.gridy = 4;
        mainPanel.add(txtPassword, gbc);

        // 4. Botón Entrar
        JButton btnEntrar = new JButton("Entrar");
        this.getRootPane().setDefaultButton(btnEntrar);
        estilizarBoton(btnEntrar);
        btnEntrar.addActionListener(e -> validarAcceso());
        gbc.gridy = 5;
        gbc.insets = new Insets(25, 0, 10, 0);
        mainPanel.add(btnEntrar, gbc);

        add(mainPanel);
        configurarPlaceholders();
    }

    // --- MÉTODOS DE APOYO ---

    private JLabel crearLabelBlanco(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(Color.WHITE); // Corrección de visibilidad[cite: 1]
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        return lbl;
    }

    private void estilizarCaja(JTextField campo, String placeholder) {
        campo.setPreferredSize(new Dimension(250, 35)); // Tamaño proporcional[cite: 1]
        campo.setBackground(new Color(45, 52, 71));
        campo.setForeground(new Color(150, 150, 150));
        campo.setText(placeholder);
        campo.setCaretColor(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 105), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void estilizarBoton(JButton btn) {
        btn.setPreferredSize(new Dimension(250, 45));
        btn.setBackground(new Color(237, 28, 36)); // Rojo Logo[cite: 1]
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void configurarPlaceholders() {
        agregarEfecto(txtUsuario, "ingrese su usuario", false);
        agregarEfecto(txtPassword, "ingrese su contraseña", true);
    }

    private void agregarEfecto(JTextField f, String h, boolean esPass) {
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (f.getText().equals(h)) {
                    f.setText("");
                    f.setForeground(Color.WHITE);
                    if (esPass) ((JPasswordField) f).setEchoChar('•');
                }
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(h);
                    f.setForeground(new Color(150, 150, 150));
                    if (esPass) ((JPasswordField) f).setEchoChar((char) 0);
                }
            }
        });
    }

    private void validarAcceso() {
        String user = txtUsuario.getText();
        String pass = new String(txtPassword.getPassword());

        if (user.equals("AdrianAdmin") && pass.equals("escapes1")) {
            new VentanaMenu(afipService, inventario, repo).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- CLASE INTERNA PARA EL LOGO ---
    private class PanelCabecera extends JPanel {
        private Image img;

        public PanelCabecera() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 180));
            // Ruta absoluta en tu Kubuntu[cite: 1]
            img = new ImageIcon("/home/ramiro/Documentos/escapesJ/Logo.png").getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (img != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                int size = 150;
                g2d.drawImage(img, (getWidth() - size) / 2, 10, size, size, this);
                g2d.dispose();
            }
            super.paintComponent(g);
        }
    }
}