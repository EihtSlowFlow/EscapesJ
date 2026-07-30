package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.UsuarioRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class VentanaRecuperacion extends JDialog {

    private final UsuarioRepository usuarioRepository;
    
    private CardLayout cardLayout;
    private JPanel cardsPanel;
    
    private JTextField txtUsuario;
    private JLabel lblPregunta;
    private JTextField txtRespuesta;
    private JPasswordField txtNuevaClave;
    private JPasswordField txtConfirmarClave;
    
    private String usuarioValido;

    public VentanaRecuperacion(JFrame parent, UsuarioRepository usuarioRepository) {
        super(parent, "Recuperación de Contraseña", true);
        this.usuarioRepository = usuarioRepository;
        
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        
        cardsPanel.add(crearPanelUsuario(), "Paso1");
        cardsPanel.add(crearPanelPregunta(), "Paso2");
        cardsPanel.add(crearPanelReset(), "Paso3");
        
        add(cardsPanel);
    }
    
    private JPanel crearPanelUsuario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(250, 250, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0;
        
        gbc.gridy = 0;
        JLabel lblTitulo = new JLabel("Paso 1: Identificación");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitulo, gbc);
        
        gbc.gridy = 1;
        panel.add(new JLabel("Ingresá tu nombre de usuario:"), gbc);
        
        gbc.gridy = 2;
        txtUsuario = new JTextField("admin");
        txtUsuario.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(txtUsuario, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 0, 0, 0);
        JButton btnSiguiente = new JButton("Siguiente");
        estilizarBoton(btnSiguiente);
        btnSiguiente.addActionListener(e -> validarUsuarioYPregunta());
        panel.add(btnSiguiente, gbc);
        
        return panel;
    }
    
    private JPanel crearPanelPregunta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(250, 250, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0;
        
        gbc.gridy = 0;
        JLabel lblTitulo = new JLabel("Paso 2: Pregunta de Seguridad");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitulo, gbc);
        
        gbc.gridy = 1;
        lblPregunta = new JLabel("Pregunta: ...");
        lblPregunta.setFont(new Font("SansSerif", Font.ITALIC, 13));
        panel.add(lblPregunta, gbc);
        
        gbc.gridy = 2;
        txtRespuesta = new JTextField();
        txtRespuesta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(txtRespuesta, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 0, 0, 0);
        JButton btnVerificar = new JButton("Verificar Respuesta");
        estilizarBoton(btnVerificar);
        btnVerificar.addActionListener(e -> validarRespuesta());
        panel.add(btnVerificar, gbc);
        
        return panel;
    }
    
    private JPanel crearPanelReset() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(250, 250, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = 1.0;
        
        gbc.gridy = 0;
        JLabel lblTitulo = new JLabel("Paso 3: Nueva Contraseña");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitulo, gbc);
        
        gbc.gridy = 1;
        panel.add(new JLabel("Nueva contraseña:"), gbc);
        
        gbc.gridy = 2;
        txtNuevaClave = new JPasswordField();
        txtNuevaClave.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(txtNuevaClave, gbc);
        
        gbc.gridy = 3;
        panel.add(new JLabel("Confirmar contraseña:"), gbc);
        
        gbc.gridy = 4;
        txtConfirmarClave = new JPasswordField();
        txtConfirmarClave.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(txtConfirmarClave, gbc);
        
        gbc.gridy = 5;
        gbc.insets = new Insets(15, 0, 0, 0);
        JButton btnGuardar = new JButton("Restablecer Contraseña");
        estilizarBoton(btnGuardar);
        btnGuardar.addActionListener(e -> restablecerClave());
        panel.add(btnGuardar, gbc);
        
        return panel;
    }
    
    private void validarUsuarioYPregunta() {
        String usr = txtUsuario.getText().trim();
        if (usr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Optional<String> pregunta = usuarioRepository.obtenerPreguntaSeguridad(usr);
        if (pregunta.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El usuario no existe o no tiene una pregunta de seguridad configurada.\n" +
                "Si sos el administrador, revisá la configuración.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        usuarioValido = usr;
        lblPregunta.setText("Pregunta: " + pregunta.get());
        cardLayout.show(cardsPanel, "Paso2");
    }
    
    private void validarRespuesta() {
        String rta = txtRespuesta.getText();
        if (rta.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ingresá la respuesta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (usuarioRepository.validarRespuestaSeguridad(usuarioValido, rta)) {
            cardLayout.show(cardsPanel, "Paso3");
        } else {
            JOptionPane.showMessageDialog(this, "Respuesta incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void restablecerClave() {
        String p1 = new String(txtNuevaClave.getPassword());
        String p2 = new String(txtConfirmarClave.getPassword());
        
        if (p1.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La contraseña no puede estar vacía.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        usuarioRepository.resetPassword(usuarioValido, p1);
        JOptionPane.showMessageDialog(this, "Contraseña actualizada exitosamente.\nYa podés iniciar sesión.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
    
    private void estilizarBoton(JButton btn) {
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
