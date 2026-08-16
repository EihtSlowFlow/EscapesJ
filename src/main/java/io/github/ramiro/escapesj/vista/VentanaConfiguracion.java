package io.github.ramiro.escapesj.vista;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.UsuarioRepository;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class VentanaConfiguracion extends JFrame {
    private final ConfigRepository configRepo;
    private final UsuarioRepository usuarioRepo;

    private JTextField txtAccessToken, txtCuit, txtCertPath, txtKeyPath;
    private JCheckBox chkProduction;
    private JTextField txtUsuarioActual;
    private JTextField txtUsuarioNuevo;
    private JPasswordField txtPasswordActual;
    private JPasswordField txtPasswordNueva;
    private JPasswordField txtPasswordConfirmar;
    private JTextField txtPreguntaSeguridad;
    private JTextField txtRespuestaSeguridad;
    private JTextField txtRutaBoletas, txtRutaPresupuestos;

    public VentanaConfiguracion(ConfigRepository configRepo, UsuarioRepository usuarioRepo) {
        this.configRepo = configRepo;
        this.usuarioRepo = usuarioRepo;
        initUI();
        cargarDatos();
    }

    private void initUI() {
        setTitle("EscapesJ - Configuración");
        setSize(550, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(10, 10));

        // Cabecera
        JPanel pnlHeader = new JPanel(new GridBagLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setPreferredSize(new Dimension(0, 80));
        JLabel lblTitulo = new JLabel("⚙  CONFIGURACIÓN");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        pnlHeader.add(lblTitulo);
        add(pnlHeader, BorderLayout.NORTH);

        // Panel principal de pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(45, 52, 71));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setOpaque(false);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === SECCIÓN AFIP SDK ===
        JPanel pnlAfip = new JPanel(new GridBagLayout());
        pnlAfip.setOpaque(true);
        pnlAfip.setBackground(new Color(0, 43, 91));
        pnlAfip.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0;
        pnlAfip.add(crearLabel("Access Token (de app.afipsdk.com):"), gbc);
        gbc.gridy = 1;
        txtAccessToken = new JTextField();
        estilizarCampo(txtAccessToken);
        pnlAfip.add(txtAccessToken, gbc);

        gbc.gridy = 2;
        pnlAfip.add(crearLabel("CUIT del negocio (sin guiones):"), gbc);
        gbc.gridy = 3;
        txtCuit = new JTextField();
        estilizarCampo(txtCuit);
        pnlAfip.add(txtCuit, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 5, 5);
        chkProduction = new JCheckBox("Modo Producción (desmarcar para desarrollo/testing)");
        chkProduction.setOpaque(false);
        chkProduction.setForeground(new Color(200, 200, 200));
        chkProduction.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chkProduction.setFocusPainted(false);
        pnlAfip.add(chkProduction, gbc);

        gbc.gridy = 5;
        JLabel lblHelp = new JLabel("<html><small><font color='#888'>Obtené tu Access Token en <b>https://app.afipsdk.com</b></font></small></html>");
        pnlAfip.add(lblHelp, gbc);

        // Campos de certificado y clave (producción)
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 5, 2, 5);
        pnlAfip.add(crearLabel("Certificado (.crt) — solo para producción:"), gbc);
        gbc.gridy = 7;
        gbc.insets = new Insets(2, 5, 5, 5);
        JPanel pnlCert = new JPanel(new BorderLayout(5, 0));
        pnlCert.setOpaque(false);
        txtCertPath = new JTextField();
        estilizarCampo(txtCertPath);
        txtCertPath.setEditable(false);
        pnlCert.add(txtCertPath, BorderLayout.CENTER);
        JButton btnCert = new JButton("📂");
        btnCert.setPreferredSize(new Dimension(45, 30));
        btnCert.addActionListener(e -> elegirArchivoFichero(txtCertPath, "crt"));
        pnlCert.add(btnCert, BorderLayout.EAST);
        pnlAfip.add(pnlCert, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(5, 5, 2, 5);
        pnlAfip.add(crearLabel("Clave privada (.key) — solo para producción:"), gbc);
        gbc.gridy = 9;
        gbc.insets = new Insets(2, 5, 5, 5);
        JPanel pnlKey = new JPanel(new BorderLayout(5, 0));
        pnlKey.setOpaque(false);
        txtKeyPath = new JTextField();
        estilizarCampo(txtKeyPath);
        txtKeyPath.setEditable(false);
        pnlKey.add(txtKeyPath, BorderLayout.CENTER);
        JButton btnKey = new JButton("📂");
        btnKey.setPreferredSize(new Dimension(45, 30));
        btnKey.addActionListener(e -> elegirArchivoFichero(txtKeyPath, "key"));
        pnlKey.add(btnKey, BorderLayout.EAST);
        pnlAfip.add(pnlKey, gbc);

        gbc.gridy = 10;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton btnGuardarAfip = new JButton("Guardar Configuración AFIP");
        btnGuardarAfip.setBackground(new Color(52, 152, 219));
        btnGuardarAfip.setForeground(Color.WHITE);
        btnGuardarAfip.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGuardarAfip.setFocusPainted(false);
        btnGuardarAfip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardarAfip.addActionListener(e -> guardarConfigAfip());
        pnlAfip.add(btnGuardarAfip, gbc);

        tabbedPane.addTab("AFIP", pnlAfip);

        // === SECCIÓN CREDENCIALES ===
        JPanel pnlCreds = new JPanel(new GridBagLayout());
        pnlCreds.setOpaque(true);
        pnlCreds.setBackground(new Color(0, 43, 91));
        pnlCreds.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0;
        pnlCreds.add(crearLabel("Usuario Actual:"), gbc);
        gbc.gridy = 1;
        txtUsuarioActual = new JTextField();
        estilizarCampo(txtUsuarioActual);
        pnlCreds.add(txtUsuarioActual, gbc);

        gbc.gridy = 2;
        pnlCreds.add(crearLabel("Nuevo Usuario (dejar vacío para no cambiar):"), gbc);
        gbc.gridy = 3;
        txtUsuarioNuevo = new JTextField();
        estilizarCampo(txtUsuarioNuevo);
        pnlCreds.add(txtUsuarioNuevo, gbc);

        gbc.gridy = 4;
        pnlCreds.add(crearLabel("Contraseña Actual:"), gbc);
        gbc.gridy = 5;
        txtPasswordActual = new JPasswordField();
        estilizarCampo(txtPasswordActual);
        pnlCreds.add(txtPasswordActual, gbc);

        gbc.gridy = 6;
        pnlCreds.add(crearLabel("Nueva Contraseña:"), gbc);
        gbc.gridy = 7;
        txtPasswordNueva = new JPasswordField();
        estilizarCampo(txtPasswordNueva);
        pnlCreds.add(txtPasswordNueva, gbc);

        gbc.gridy = 8;
        pnlCreds.add(crearLabel("Confirmar Nueva Contraseña:"), gbc);
        gbc.gridy = 9;
        txtPasswordConfirmar = new JPasswordField();
        estilizarCampo(txtPasswordConfirmar);
        pnlCreds.add(txtPasswordConfirmar, gbc);

        gbc.gridy = 10;
        pnlCreds.add(crearLabel("Pregunta de Seguridad (Opcional, para recuperar clave):"), gbc);
        gbc.gridy = 11;
        txtPreguntaSeguridad = new JTextField();
        estilizarCampo(txtPreguntaSeguridad);
        pnlCreds.add(txtPreguntaSeguridad, gbc);

        gbc.gridy = 12;
        pnlCreds.add(crearLabel("Respuesta de Seguridad:"), gbc);
        gbc.gridy = 13;
        txtRespuestaSeguridad = new JTextField();
        estilizarCampo(txtRespuestaSeguridad);
        pnlCreds.add(txtRespuestaSeguridad, gbc);

        gbc.gridy = 14;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton btnCambiarCreds = new JButton("Actualizar Credenciales");
        btnCambiarCreds.setBackground(new Color(46, 204, 113));
        btnCambiarCreds.setForeground(Color.WHITE);
        btnCambiarCreds.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCambiarCreds.setFocusPainted(false);
        btnCambiarCreds.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCambiarCreds.addActionListener(e -> cambiarCredenciales());
        pnlCreds.add(btnCambiarCreds, gbc);

        tabbedPane.addTab("Credenciales", pnlCreds);

        // === SECCIÓN DIRECTORIOS ===
        JPanel pnlDirectorios = new JPanel(new GridBagLayout());
        pnlDirectorios.setOpaque(true);
        pnlDirectorios.setBackground(new Color(0, 43, 91));
        pnlDirectorios.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0;
        pnlDirectorios.add(crearLabel("Carpeta para Boletas:"), gbc);
        gbc.gridy = 1;
        JPanel pnlBoletasDir = new JPanel(new BorderLayout(5, 0));
        pnlBoletasDir.setOpaque(false);
        txtRutaBoletas = new JTextField();
        estilizarCampo(txtRutaBoletas);
        pnlBoletasDir.add(txtRutaBoletas, BorderLayout.CENTER);

        JPanel pnlBotonesBoletas = new JPanel(new GridLayout(1, 2, 5, 0));
        pnlBotonesBoletas.setOpaque(false);
        
        JButton btnRestaurarBoletas = new JButton("🔄");
        btnRestaurarBoletas.setToolTipText("Restablecer ruta por defecto");
        btnRestaurarBoletas.setPreferredSize(new Dimension(45, 30));
        btnRestaurarBoletas.addActionListener(e -> txtRutaBoletas.setText(ConfigRepository.getDefaultBoletasPath()));
        pnlBotonesBoletas.add(btnRestaurarBoletas);

        JButton btnBoletasDir = new JButton("📂");
        btnBoletasDir.setPreferredSize(new Dimension(45, 30));
        btnBoletasDir.addActionListener(e -> elegirDirectorio(txtRutaBoletas));
        pnlBotonesBoletas.add(btnBoletasDir);

        pnlBoletasDir.add(pnlBotonesBoletas, BorderLayout.EAST);
        pnlDirectorios.add(pnlBoletasDir, gbc);

        gbc.gridy = 2;
        pnlDirectorios.add(crearLabel("Carpeta para Presupuestos:"), gbc);
        gbc.gridy = 3;
        JPanel pnlPresupDir = new JPanel(new BorderLayout(5, 0));
        pnlPresupDir.setOpaque(false);
        txtRutaPresupuestos = new JTextField();
        estilizarCampo(txtRutaPresupuestos);
        pnlPresupDir.add(txtRutaPresupuestos, BorderLayout.CENTER);

        JPanel pnlBotonesPresup = new JPanel(new GridLayout(1, 2, 5, 0));
        pnlBotonesPresup.setOpaque(false);

        JButton btnRestaurarPresup = new JButton("🔄");
        btnRestaurarPresup.setToolTipText("Restablecer ruta por defecto");
        btnRestaurarPresup.setPreferredSize(new Dimension(45, 30));
        btnRestaurarPresup.addActionListener(e -> txtRutaPresupuestos.setText(ConfigRepository.getDefaultPresupuestosPath()));
        pnlBotonesPresup.add(btnRestaurarPresup);

        JButton btnPresupDir = new JButton("📂");
        btnPresupDir.setPreferredSize(new Dimension(45, 30));
        btnPresupDir.addActionListener(e -> elegirDirectorio(txtRutaPresupuestos));
        pnlBotonesPresup.add(btnPresupDir);

        pnlPresupDir.add(pnlBotonesPresup, BorderLayout.EAST);
        pnlDirectorios.add(pnlPresupDir, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(20, 5, 5, 5);
        JButton btnGuardarRutas = new JButton("Guardar Rutas");
        btnGuardarRutas.setBackground(new Color(155, 89, 182));
        btnGuardarRutas.setForeground(Color.WHITE);
        btnGuardarRutas.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGuardarRutas.setFocusPainted(false);
        btnGuardarRutas.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardarRutas.addActionListener(e -> guardarRutas());
        pnlDirectorios.add(btnGuardarRutas, gbc);

        tabbedPane.addTab("Directorios", pnlDirectorios);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void cargarDatos() {
        try {
            txtAccessToken.setText(configRepo.getAfipAccessToken());
            txtCuit.setText(configRepo.getAfipCuit());
            chkProduction.setSelected(configRepo.isAfipProduction());
            txtCertPath.setText(configRepo.getAfipCertPath());
            txtKeyPath.setText(configRepo.getAfipKeyPath());
            txtRutaBoletas.setText(configRepo.getRutaBoletas());
            txtRutaPresupuestos.setText(configRepo.getRutaPresupuestos());

            usuarioRepo.obtenerPreguntaSeguridad("admin").ifPresent(p -> txtPreguntaSeguridad.setText(p));
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "cargar configuraciones", e);
            SwingUtilities.invokeLater(this::dispose);
        }
    }

    private void elegirArchivoFichero(JTextField campo, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (path.endsWith("." + extension)) {
                campo.setText(path);
            } else {
                JOptionPane.showMessageDialog(this, "El archivo debe tener la extensión ." + extension);
            }
        }
    }

    private void elegirDirectorio(JTextField campo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith("/") && !path.endsWith("\\")) {
                path += System.getProperty("file.separator");
            }
            campo.setText(path);
        }
    }

    private void guardarRutas() {
        String rutaBoletas = txtRutaBoletas.getText().trim();
        String rutaPresupuestos = txtRutaPresupuestos.getText().trim();

        try {
            java.util.Map<String, String> configs = new java.util.HashMap<>();
            // Se guardan los valores incluso si están vacíos, para permitir "reset" en la BD.
            configs.put("ruta.boletas", rutaBoletas);
            configs.put("ruta.presupuestos", rutaPresupuestos);
            configRepo.guardarMultiples(configs);

            JOptionPane.showMessageDialog(this,
                    "Rutas actualizadas correctamente.",
                    "Configuración", JOptionPane.INFORMATION_MESSAGE);
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "guardar rutas", e);
        }
    }

    private void guardarConfigAfip() {
        String token = txtAccessToken.getText().trim();
        String cuit = txtCuit.getText().trim();
        boolean produccion = chkProduction.isSelected();
        String certPath = txtCertPath.getText().trim();
        String keyPath = txtKeyPath.getText().trim();

        if (token.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El Access Token no puede estar vacío.\nObtené uno en https://app.afipsdk.com",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cuit.isEmpty() || !cuit.matches("\\d{11}")) {
            JOptionPane.showMessageDialog(this,
                    "El CUIT debe tener 11 dígitos numéricos sin guiones.",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean esTestCuit = "20409378472".equals(cuit);
        String environment = (produccion && !esTestCuit) ? "prod" : "dev";

        if ("prod".equals(environment) && (certPath.isEmpty() || keyPath.isEmpty())) {
            JOptionPane.showMessageDialog(this,
                    "En modo Producción necesitás seleccionar el Certificado (.crt) y la Clave privada (.key).",
                    "Faltan archivos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Guardar en DB atómicamente
        try {
            java.util.Map<String, String> configs = new java.util.HashMap<>();
            configs.put("afip.access_token", token);
            configs.put("afip.cuit", cuit);
            configs.put("afip.production", produccion ? "true" : "false");
            configs.put("afip.cert_path", certPath);
            configs.put("afip.key_path", keyPath);
            configRepo.guardarMultiples(configs);
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "guardar configuración AFIP", e);
            return;
        }

        // Testear conexión en segundo plano
        JDialog dlgEspera = new JDialog(this, "Verificando...", true);
        JLabel lblEspera = new JLabel("  Probando conexión con AFIP...", JLabel.CENTER);
        lblEspera.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblEspera.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        dlgEspera.add(lblEspera);
        dlgEspera.pack();
        dlgEspera.setLocationRelativeTo(this);

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                return testearConexionAfip(token, cuit, environment, certPath, keyPath);
            }

            @Override
            protected void done() {
                dlgEspera.dispose();
                try {
                    String[] result = get(); // [0] = "ok" o "error", [1] = mensaje
                    if ("ok".equals(result[0])) {
                        JOptionPane.showMessageDialog(VentanaConfiguracion.this,
                                "✅ Configuración guardada y verificada.\n\n" + result[1],
                                "Conexión Exitosa", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(VentanaConfiguracion.this,
                                "⚠ Configuración guardada, pero la verificación falló:\n\n" + result[1],
                                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentanaConfiguracion.this,
                            "Configuración guardada.\nNo se pudo verificar: " + ex.getMessage(),
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();

        // Mostrar el diálogo de espera (se bloquea hasta que el worker lo cierre)
        dlgEspera.setVisible(true);
    }

    /**
     * Hace un POST real a la API de AfipSDK para verificar que las credenciales funcionan.
     * Retorna {"ok", mensaje} o {"error", mensaje}.
     */
    private String[] testearConexionAfip(String accessToken, String cuit, String env,
                                          String certPath, String keyPath) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("environment", env);
            body.addProperty("tax_id", cuit);
            body.addProperty("wsid", "ws_sr_padron_a13");

            // En producción, incluir cert y key
            if ("prod".equals(env)) {
                String certContent = Files.readString(Path.of(certPath), StandardCharsets.UTF_8);
                String keyContent = Files.readString(Path.of(keyPath), StandardCharsets.UTF_8);
                body.addProperty("cert", certContent);
                body.addProperty("key", keyContent);
            }

            HttpURLConnection conn = (HttpURLConnection)
                    URI.create("https://app.afipsdk.com/api/v1/afip/auth").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(new Gson().toJson(body).getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String responseBody;
            try (Scanner scanner = new Scanner(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8
            ).useDelimiter("\\A")) {
                responseBody = scanner.hasNext() ? scanner.next() : "";
            }

            if (status == 200) {
                JsonObject resp = new Gson().fromJson(responseBody, JsonObject.class);
                String expiration = resp.has("expiration") ? resp.get("expiration").getAsString() : "N/A";
                return new String[]{"ok",
                        "Ambiente: " + env.toUpperCase() + "\n" +
                        "CUIT: " + cuit + "\n" +
                        "Token AFIP obtenido correctamente.\n" +
                        "Expira: " + expiration};
            } else {
                // Extraer mensaje de error del JSON
                try {
                    JsonObject err = new Gson().fromJson(responseBody, JsonObject.class);
                    String msg = err.has("message") ? err.get("message").getAsString() : responseBody;
                    return new String[]{"error", "HTTP " + status + ": " + msg};
                } catch (Exception e) {
                    return new String[]{"error", "HTTP " + status + ": " + responseBody};
                }
            }
        } catch (java.nio.file.NoSuchFileException e) {
            return new String[]{"error", "No se encontró el archivo:\n" + e.getMessage()};
        } catch (Exception e) {
            return new String[]{"error", "Error de conexión: " + e.getMessage()};
        }
    }

    private void cambiarCredenciales() {
        String usuarioActual = txtUsuarioActual.getText().trim();
        String passwordActual = new String(txtPasswordActual.getPassword());
        String passwordNueva = new String(txtPasswordNueva.getPassword());
        String passwordConfirmar = new String(txtPasswordConfirmar.getPassword());

        if (usuarioActual.isEmpty() || passwordActual.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el usuario y contraseña actual.");
            return;
        }

        try {
            // Validar credenciales actuales
            if (!usuarioRepo.validarCredenciales(usuarioActual, passwordActual)) {
                JOptionPane.showMessageDialog(this,
                        "Las credenciales actuales son incorrectas.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!passwordNueva.isEmpty() && !passwordNueva.equals(passwordConfirmar)) {
                JOptionPane.showMessageDialog(this,
                        "Las contraseñas nuevas no coinciden.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String usuarioNuevo = txtUsuarioNuevo.getText().trim();
            String pregunta = txtPreguntaSeguridad.getText().trim();
            String respuesta = txtRespuestaSeguridad.getText().trim();

            usuarioRepo.actualizarCredenciales(usuarioActual, usuarioNuevo, passwordNueva, pregunta, respuesta);

            JOptionPane.showMessageDialog(this,
                    "Credenciales y seguridad actualizadas correctamente.",
                    "Configuración", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar campos
            txtPasswordActual.setText("");
            txtPasswordNueva.setText("");
            txtPasswordConfirmar.setText("");
            txtUsuarioNuevo.setText("");
            txtRespuestaSeguridad.setText("");
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "actualizar credenciales", e);
        }
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(200, 200, 200));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private JPanel crearSeparador(String titulo) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lbl = new JLabel(titulo);
        lbl.setForeground(new Color(52, 152, 219));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        pnl.add(lbl, BorderLayout.WEST);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(70, 80, 105));
        pnl.add(sep, BorderLayout.SOUTH);

        return pnl;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setBackground(new Color(45, 52, 71));
        campo.setForeground(Color.WHITE);
        campo.setCaretColor(Color.WHITE);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 105), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void elegirArchivo(JTextField destino, String extension) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos " + extension.toUpperCase(), extension));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            destino.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }
}
