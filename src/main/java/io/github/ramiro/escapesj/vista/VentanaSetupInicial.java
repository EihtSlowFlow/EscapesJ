package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.UsuarioRepository;
import javax.swing.*;
import java.awt.*;

public class VentanaSetupInicial extends JFrame {

    private final UsuarioRepository usuarioRepository;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmarPassword;

    public VentanaSetupInicial(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;

        setTitle("Instalación Inicial - Crear Administrador");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Si la cierra no puede usar la app
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitulo = new JLabel("Configuración Inicial");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        JTextArea txtExplicacion = new JTextArea("Bienvenido. Para comenzar a usar el sistema, debe crear una cuenta de administrador segura. Por favor, ingrese un nombre de usuario y una contraseña.");
        txtExplicacion.setWrapStyleWord(true);
        txtExplicacion.setLineWrap(true);
        txtExplicacion.setEditable(false);
        txtExplicacion.setOpaque(false);
        txtExplicacion.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.add(lblTitulo, BorderLayout.NORTH);
        headerPanel.add(txtExplicacion, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(6, 1, 5, 5));

        formPanel.add(new JLabel("Nombre de Usuario:"));
        txtUsuario = new JTextField("admin");
        formPanel.add(txtUsuario);

        formPanel.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);

        formPanel.add(new JLabel("Confirmar Contraseña:"));
        txtConfirmarPassword = new JPasswordField();
        formPanel.add(txtConfirmarPassword);

        JButton btnGuardar = new JButton("Crear Cuenta y Continuar");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGuardar.addActionListener(e -> guardarSetup());

        panelPrincipal.add(headerPanel, BorderLayout.NORTH);
        panelPrincipal.add(formPanel, BorderLayout.CENTER);
        panelPrincipal.add(btnGuardar, BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
    }

    private void guardarSetup() {
        String usuario = txtUsuario.getText().trim();
        String pwd = new String(txtPassword.getPassword());
        String confPwd = new String(txtConfirmarPassword.getPassword());

        if (usuario.isEmpty() || pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe completar todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!pwd.equals(confPwd)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pwd.length() < 6) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean exito = usuarioRepository.crearAdminSetupInicial(usuario, pwd);
        if (exito) {
            JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente. Ahora iniciará sesión automáticamente o cerrará. En el primer login deberá cambiarla de todos modos.", "Configuración Exitosa", JOptionPane.INFORMATION_MESSAGE);
            // Wait, the plan was to set debe_cambiar_password = 1 in crearAdminSetupInicial, but since the user creates it manually using this dialog, we don't need to force a change again on the first login!
            // Actually, my plan was "Si optamos por la creación manual ...". So we can just set it to 0 or leave it, but I implemented `crearAdminSetupInicial` setting it to 1. I will change that logic slightly or let it force it. Let's modify `crearAdminSetupInicial` to NOT force a change if it's created manually in the dialog. Or we can just let it force the change to test the feature.
            // Let's close and let them login.
            JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente. El sistema se cerrará. Vuelva a iniciar la aplicación y acceda con su nueva cuenta.", "Configuración Exitosa", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } else {
            JOptionPane.showMessageDialog(this, "Error al crear la cuenta. Intente de nuevo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
