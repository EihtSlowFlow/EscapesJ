package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.ClienteRepresentador;
import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.modelo.ProductoRepresentador;
import io.github.ramiro.escapesj.modelo.ServicioRealizado;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.persistencia.ServicioRepository;
import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.servicio.BoletaPdfService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VentanaPrincipal extends JFrame {
    private final AfipService afipService;
    private final Inventario inventario;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final BoletaRepository boletaRepository;
    private final ConfigRepository configRepository;

    private DefaultTableModel modeloTabla;
    private JTextField txtDni, txtNombre, txtCodProducto, txtCantidad, txtDescripcion, txtMonto, txtDescuento;
    private JComboBox<String> cmbMetodoPago;
    private JLabel lblProductoInfo;

    private BigDecimal precioSeleccionado = BigDecimal.ZERO;
    private String descripcionProductoSeleccionado = "";
    private String codigoProductoSeleccionado = "";
    private static final String PLACEHOLDER_NOMBRE = "Se completa automáticamente";

    // Ítems acumulados para la boleta
    private final List<ItemOrden> itemsOrden = new ArrayList<>();

    // Registro interno para acumular los ítems antes de generar la boleta
    private record ItemOrden(String tipo, String descripcion, String codigoProducto,
                             int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    public VentanaPrincipal(AfipService afip, Inventario inv, ProductoRepository prodRepo,
                            ServicioRepository servRepo, BoletaRepository boletaRepo,
                            ConfigRepository configRepo) {
        this.afipService = afip;
        this.inventario = inv;
        this.productoRepository = prodRepo;
        this.servicioRepository = servRepo;
        this.boletaRepository = boletaRepo;
        this.configRepository = configRepo;
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Formulario de Servicio");
        setSize(780, 750);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(0, 43, 91));

        // ═══════════════════════════════════════════
        //  CABECERA CON LOGO (más compacta)
        // ═══════════════════════════════════════════
        PanelCabecera cabecera = new PanelCabecera();
        cabecera.setLayout(new GridBagLayout());
        cabecera.setPreferredSize(new Dimension(0, 80));
        JLabel lblTitulo = new JLabel("FORMULARIO DE SERVICIO");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        cabecera.add(lblTitulo);
        mainPanel.add(cabecera, BorderLayout.NORTH);

        // ═══════════════════════════════════════════
        //  FORMULARIO CENTRAL (scrollable)
        // ═══════════════════════════════════════════
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // ── SECCIÓN 1: DATOS DEL CLIENTE ──
        formPanel.add(crearSeparador("DATOS DEL CLIENTE"));
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 1: [DNI] [Buscar] [Nombre]
        JPanel filaDni = new JPanel(new GridBagLayout());
        filaDni.setOpaque(false);
        filaDni.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        // DNI campo
        gbc.gridx = 0; gbc.weightx = 0.35; gbc.weighty = 1;
        JPanel pnlDniInner = new JPanel(new BorderLayout());
        pnlDniInner.setOpaque(false);
        JLabel lblDni = new JLabel("DNI del Cliente:");
        lblDni.setForeground(new Color(200, 200, 200));
        lblDni.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlDniInner.add(lblDni, BorderLayout.NORTH);
        txtDni = new JTextField();
        estilizarComponente(txtDni);
        configurarPlaceholder(txtDni, "Ej: 46000698");
        pnlDniInner.add(txtDni, BorderLayout.CENTER);
        filaDni.add(pnlDniInner, gbc);

        // Botón Buscar
        gbc.gridx = 1; gbc.weightx = 0;
        gbc.insets = new Insets(14, 0, 0, 6);
        JButton btnBuscarDni = new JButton("🔍 Buscar");
        btnBuscarDni.setBackground(new Color(52, 152, 219));
        btnBuscarDni.setForeground(Color.WHITE);
        btnBuscarDni.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnBuscarDni.setFocusPainted(false);
        btnBuscarDni.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscarDni.setPreferredSize(new Dimension(90, 30));
        btnBuscarDni.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219).brighter(), 1));
        filaDni.add(btnBuscarDni, gbc);

        // Nombre campo
        gbc.gridx = 2; gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlNombreInner = new JPanel(new BorderLayout());
        pnlNombreInner.setOpaque(false);
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(new Color(200, 200, 200));
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlNombreInner.add(lblNombre, BorderLayout.NORTH);
        txtNombre = new JTextField(PLACEHOLDER_NOMBRE);
        estilizarComponente(txtNombre);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setEditable(false);
        txtNombre.setFocusable(false);
        txtNombre.setBackground(new Color(30, 35, 48));
        pnlNombreInner.add(txtNombre, BorderLayout.CENTER);
        filaDni.add(pnlNombreInner, gbc);

        formPanel.add(filaDni);
        formPanel.add(Box.createVerticalStrut(6));

        // ── SECCIÓN 2: DETALLE DEL TRABAJO ──
        formPanel.add(crearSeparador("DETALLE DEL TRABAJO"));
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 2: [Descripción (ancho)] [Monto]
        JPanel filaDesc = new JPanel(new GridBagLayout());
        filaDesc.setOpaque(false);
        filaDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.7; gbc.weighty = 1;
        JPanel pnlDescInner = crearCampoPanel("Descripción del Trabajo:", "Ej: Cambio de silenciador");
        txtDescripcion = (JTextField) ((BorderLayout) pnlDescInner.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        filaDesc.add(pnlDescInner, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlMontoInner = crearCampoPanel("Monto Mano de Obra ($):", "0");
        txtMonto = (JTextField) ((BorderLayout) pnlMontoInner.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        filaDesc.add(pnlMontoInner, gbc);

        formPanel.add(filaDesc);
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 3: [Producto + Buscar] [Cantidad]
        JPanel filaProd = new JPanel(new GridBagLayout());
        filaProd.setOpaque(false);
        filaProd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.55; gbc.weighty = 1;
        JPanel pnlProdInner = new JPanel(new BorderLayout());
        pnlProdInner.setOpaque(false);
        JLabel lblProd = new JLabel("Producto (opcional):");
        lblProd.setForeground(new Color(200, 200, 200));
        lblProd.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlProdInner.add(lblProd, BorderLayout.NORTH);
        txtCodProducto = new JTextField("Ninguno seleccionado");
        estilizarComponente(txtCodProducto);
        txtCodProducto.setEditable(false);
        txtCodProducto.setBackground(new Color(30, 35, 48));
        txtCodProducto.setForeground(new Color(150, 150, 150));
        pnlProdInner.add(txtCodProducto, BorderLayout.CENTER);
        filaProd.add(pnlProdInner, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        gbc.insets = new Insets(14, 0, 0, 6);
        JButton btnBuscarProd = new JButton("📦 Buscar");
        btnBuscarProd.setBackground(new Color(46, 125, 50));
        btnBuscarProd.setForeground(Color.WHITE);
        btnBuscarProd.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnBuscarProd.setFocusPainted(false);
        btnBuscarProd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscarProd.setPreferredSize(new Dimension(90, 30));
        btnBuscarProd.setBorder(BorderFactory.createLineBorder(new Color(46, 125, 50).brighter(), 1));
        filaProd.add(btnBuscarProd, gbc);

        gbc.gridx = 2; gbc.weightx = 0.2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlCantInner = crearCampoPanel("Cantidad:", "1");
        txtCantidad = (JTextField) ((BorderLayout) pnlCantInner.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        filaProd.add(pnlCantInner, gbc);

        formPanel.add(filaProd);

        // Info producto
        lblProductoInfo = new JLabel(" ");
        lblProductoInfo.setForeground(new Color(100, 200, 100));
        lblProductoInfo.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblProductoInfo.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 0));
        formPanel.add(lblProductoInfo);
        formPanel.add(Box.createVerticalStrut(4));

        // ── SECCIÓN 3: FORMA DE PAGO ──
        formPanel.add(crearSeparador("FORMA DE PAGO"));
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 4: [Método de Pago] [Descuento %]
        JPanel filaPago = new JPanel(new GridBagLayout());
        filaPago.setOpaque(false);
        filaPago.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.6; gbc.weighty = 1;
        JPanel pnlPagoInner = new JPanel(new BorderLayout());
        pnlPagoInner.setOpaque(false);
        JLabel lblPago = new JLabel("Método de Pago:");
        lblPago.setForeground(new Color(200, 200, 200));
        lblPago.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlPagoInner.add(lblPago, BorderLayout.NORTH);
        cmbMetodoPago = new JComboBox<>(new String[]{"EFECTIVO", "TRANSFERENCIA"});
        cmbMetodoPago.setBackground(new Color(45, 52, 71));
        cmbMetodoPago.setForeground(Color.WHITE);
        cmbMetodoPago.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pnlPagoInner.add(cmbMetodoPago, BorderLayout.CENTER);
        filaPago.add(pnlPagoInner, gbc);

        gbc.gridx = 1; gbc.weightx = 0.2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlDtoInner = crearCampoPanel("Descuento (%):", "10");
        txtDescuento = (JTextField) ((BorderLayout) pnlDtoInner.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        filaPago.add(pnlDtoInner, gbc);

        formPanel.add(filaPago);
        formPanel.add(Box.createVerticalStrut(8));

        cmbMetodoPago.addActionListener(e -> {
            boolean esEfectivo = "EFECTIVO".equals(cmbMetodoPago.getSelectedItem());
            txtDescuento.setEnabled(esEfectivo);
            txtDescuento.setBackground(esEfectivo ? new Color(45, 52, 71) : new Color(30, 35, 48));
        });

        // Botón Agregar
        JButton btnAgregar = new JButton("➕  Agregar a la Orden");
        btnAgregar.setBackground(new Color(231, 76, 60));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAgregar.setFocusPainted(false);
        btnAgregar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAgregar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnAgregar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAgregar.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60).brighter(), 1));
        formPanel.add(btnAgregar);

        // Scroll para el formulario
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(formScroll, BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        //  TABLA DE ORDEN + BOTÓN FINALIZAR (abajo)
        // ═══════════════════════════════════════════
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(0, 280));

        // Header con título + botón Quitar
        JPanel pnlOrdenHeader = new JPanel(new BorderLayout());
        pnlOrdenHeader.setOpaque(false);
        JLabel lblOrden = new JLabel("  ORDEN ACTUAL");
        lblOrden.setForeground(new Color(52, 152, 219));
        lblOrden.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblOrden.setBorder(BorderFactory.createEmptyBorder(5, 10, 3, 0));
        pnlOrdenHeader.add(lblOrden, BorderLayout.WEST);

        JButton btnQuitar = new JButton("❌ Quitar Seleccionado");
        btnQuitar.setBackground(new Color(192, 57, 43));
        btnQuitar.setForeground(Color.WHITE);
        btnQuitar.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnQuitar.setFocusPainted(false);
        btnQuitar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuitar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        pnlOrdenHeader.add(btnQuitar, BorderLayout.EAST);
        bottomPanel.add(pnlOrdenHeader, BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel(new Object[]{"Detalle", "Cant.", "Subtotal"}, 0);
        JTable tabla = new JTable(modeloTabla);
        estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(new Color(45, 52, 71));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        bottomPanel.add(scroll, BorderLayout.CENTER);

        // Botón Finalizar Boleta
        JButton btnFinalizar = new JButton("🧾  Finalizar y Generar Boleta");
        btnFinalizar.setBackground(new Color(46, 204, 113));
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnFinalizar.setFocusPainted(false);
        btnFinalizar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFinalizar.setPreferredSize(new Dimension(0, 45));
        btnFinalizar.setBorder(BorderFactory.createLineBorder(new Color(46, 204, 113).brighter(), 1));
        bottomPanel.add(btnFinalizar, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // ═══════════════════════════════════════════
        //  EVENTOS
        // ═══════════════════════════════════════════
        btnBuscarDni.addActionListener(e -> buscarClientePorDni(btnBuscarDni));
        txtDni.addActionListener(e -> buscarClientePorDni(btnBuscarDni)); // Enter = botón
        btnBuscarProd.addActionListener(e -> abrirBuscador());
        btnAgregar.addActionListener(e -> procesarAgregadoALista());
        btnFinalizar.addActionListener(e -> finalizarBoleta());
        btnQuitar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Seleccioná un ítem de la tabla para quitar.");
                return;
            }
            ItemOrden item = itemsOrden.get(fila);
            // Ya no restauramos stock aquí porque no lo restamos al agregar
            itemsOrden.remove(fila);
            modeloTabla.removeRow(fila);
        });

        // RESET: cuando el usuario cambia el DNI, resetear el nombre
        txtDni.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { resetearNombreSiCambio(); }
            public void removeUpdate(DocumentEvent e) { resetearNombreSiCambio(); }
            public void changedUpdate(DocumentEvent e) { resetearNombreSiCambio(); }
        });

        // Auto-lookup al salir del campo DNI: si ya está en cache, mostrar nombre al instante
        // Usa buscarSoloEnCache (solo SQLite local, SIN llamadas HTTP a AFIP)
        txtDni.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String dni = txtDni.getText().trim();
                if (dni.matches("\\d{7,8}") && esTextoDeEstado(txtNombre.getText())) {
                    afipService.buscarSoloEnCache(dni).ifPresent(cliente -> {
                        cliente.presentarseEn(new ClienteRepresentador() {
                            public void definirDni(String d) {} // No tocar el DNI
                            public void definirNombre(String nombre) {
                                txtNombre.setText(nombre);
                                txtNombre.setForeground(Color.WHITE);
                            }
                        });
                    });
                }
            }
        });

        // Cache manual al perder foco del nombre (solo si el usuario escribió algo real)
        txtNombre.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String dni = txtDni.getText().trim();
                String nombre = txtNombre.getText().trim();
                if (!dni.isBlank() && dni.matches("\\d{7,8}")
                        && !esTextoDeEstado(nombre)
                        && txtNombre.isEditable()) {
                    afipService.guardarEnCacheManual(dni, nombre);
                    txtNombre.setEditable(false);
                    txtNombre.setBackground(new Color(30, 35, 48));
                    System.out.println("Cache local: Guardado manual — DNI " + dni + " → " + nombre);
                }
            }
        });

        // Foco inicial en DNI
        SwingUtilities.invokeLater(() -> txtDni.requestFocusInWindow());
    }

    // ════════════════════════════════════════
    //  LÓGICA
    // ════════════════════════════════════════

    /**
     * Resetea el nombre cuando el usuario modifica el DNI.
     * Permite buscar otro cliente sin necesidad de cerrar/abrir la ventana.
     */
    private void resetearNombreSiCambio() {
        String dniActual = txtDni.getText().trim();
        // Solo resetear si el nombre no es ya el placeholder y el DNI cambió
        if (!esTextoDeEstado(txtNombre.getText()) || txtNombre.getText().equals("Buscando...")) {
            txtNombre.setText(PLACEHOLDER_NOMBRE);
            txtNombre.setForeground(new Color(150, 150, 150));
            txtNombre.setEditable(false);
            txtNombre.setFocusable(false);
            txtNombre.setBackground(new Color(30, 35, 48));
        }
    }

    /**
     * Verifica si un texto es un placeholder o mensaje de estado (no un nombre real).
     */
    private boolean esTextoDeEstado(String texto) {
        return texto == null || texto.isEmpty()
                || texto.equals(PLACEHOLDER_NOMBRE)
                || texto.equals("Buscando...")
                || texto.equals("Error al buscar")
                || texto.startsWith("No se encontró");
    }

    private void buscarClientePorDni(JButton btnBuscar) {
        String dniIngresado = txtDni.getText().trim();

        // Validar que no sea placeholder
        if (dniIngresado.isEmpty() || dniIngresado.startsWith("Ej:")) {
            JOptionPane.showMessageDialog(this, "Ingresá un DNI para buscar.");
            return;
        }

        // Validar formato: solo 7-8 dígitos (DNI argentino real)
        if (!dniIngresado.matches("\\d{7,8}")) {
            JOptionPane.showMessageDialog(this,
                    "El DNI debe tener 7 u 8 dígitos numéricos.\nNo ingreses el CUIT, solo el DNI.",
                    "DNI inválido", JOptionPane.WARNING_MESSAGE);
            txtDni.requestFocus();
            txtDni.selectAll();
            return;
        }

        // Si ya encontramos a este cliente, no buscar de nuevo
        String nombreActual = txtNombre.getText().trim();
        if (!esTextoDeEstado(nombreActual)) {
            txtDescripcion.requestFocus();
            return;
        }

        // Feedback visual + deshabilitar botón
        btnBuscar.setEnabled(false);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setText("Buscando...");
        txtNombre.setEditable(false);

        // Búsqueda asíncrona
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                var resultado = afipService.buscarClientePorDni(dniIngresado);
                if (resultado.isPresent()) {
                    final String[] nombre = {null};
                    resultado.get().presentarseEn(new ClienteRepresentador() {
                        public void definirDni(String cuit) {} // No tocar DNI
                        public void definirNombre(String n) { nombre[0] = n; }
                    });
                    return nombre[0];
                }
                return null;
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                try {
                    String nombre = get();
                    if (nombre != null && !nombre.isBlank()) {
                        txtDni.setText(dniIngresado);
                        txtNombre.setText(nombre);
                        txtNombre.setForeground(Color.WHITE);
                        txtNombre.setEditable(false);
                        txtNombre.setBackground(new Color(30, 35, 48));
                        txtDescripcion.requestFocus();
                    } else {
                        txtDni.setText(dniIngresado);
                        txtNombre.setText("");
                        txtNombre.setForeground(Color.WHITE);
                        txtNombre.setEditable(true);
                        txtNombre.setFocusable(true);
                        txtNombre.setBackground(new Color(60, 60, 80));
                        txtNombre.requestFocus();
                        txtNombre.setToolTipText("Ingresá el nombre del cliente manualmente");
                    }
                } catch (Exception ex) {
                    txtDni.setText(dniIngresado);
                    txtNombre.setText("Error al buscar");
                    txtNombre.setForeground(new Color(255, 100, 100));
                }
            }
        }.execute();
    }

    private void abrirBuscador() {
        DialogoInventario dialogo = new DialogoInventario(this, productoRepository, producto -> {
            producto.presentarseEn(new ProductoRepresentador() {
                public void definirCodigo(String c) {
                    codigoProductoSeleccionado = c;
                    txtCodProducto.setText(c);
                    txtCodProducto.setForeground(Color.WHITE);
                }

                public void definirDescripcion(String d) {
                    descripcionProductoSeleccionado = d;
                }

                public void definirPrecio(BigDecimal p) {
                    precioSeleccionado = p;
                }
            });
            SwingUtilities.invokeLater(() ->
                lblProductoInfo.setText("✓ " + descripcionProductoSeleccionado + "  —  $" + String.format("%,.0f", precioSeleccionado))
            );
        });
        dialogo.setVisible(true);
    }

    private void procesarAgregadoALista() {
        String codigo = txtCodProducto.getText().trim();
        String detallePedido = txtDescripcion.getText().trim();
        String cantStr = txtCantidad.getText().trim();
        String montoStr = txtMonto.getText().trim();

        boolean tieneServicio = !detallePedido.isEmpty() && !detallePedido.startsWith("Ej:");
        boolean tieneProducto = !codigo.isEmpty() && !codigo.contains("Ninguno");

        if (!tieneServicio && !tieneProducto) {
            JOptionPane.showMessageDialog(this, "Debe especificar el trabajo o seleccionar un producto.");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantStr.isEmpty() || cantStr.equals("1") ? "1" : cantStr);

            // Agregar SERVICIO (mano de obra) si hay descripción
            if (tieneServicio) {
                BigDecimal montoManoObra = BigDecimal.ZERO;
                try {
                    String montoLimpio = montoStr.replace("$", "").replace(".", "").replace(",", ".").trim();
                    if (!montoLimpio.isEmpty() && !montoLimpio.equals("0")) {
                        montoManoObra = new BigDecimal(montoLimpio);
                    }
                } catch (NumberFormatException ignored) {}

                BigDecimal subtotalServicio = montoManoObra.multiply(BigDecimal.valueOf(cantidad));
                String subtotalTexto = montoManoObra.compareTo(BigDecimal.ZERO) > 0
                        ? "$" + String.format("%,.0f", subtotalServicio)
                        : "A COTIZAR";

                modeloTabla.addRow(new Object[]{detallePedido, cantidad, subtotalTexto});
                itemsOrden.add(new ItemOrden("SERVICIO", detallePedido, null,
                        cantidad, montoManoObra, subtotalServicio));

                txtDescripcion.setText("");
                txtMonto.setText("0");
            }

            // Agregar PRODUCTO si hay uno seleccionado
            if (tieneProducto) {
                if (inventario.procesarVenta(codigo, cantidad)) {
                    BigDecimal subtotal = precioSeleccionado.multiply(BigDecimal.valueOf(cantidad));
                    modeloTabla.addRow(new Object[]{descripcionProductoSeleccionado, cantidad,
                            "$" + String.format("%,.0f", subtotal)});

                    itemsOrden.add(new ItemOrden("PRODUCTO", descripcionProductoSeleccionado,
                            codigoProductoSeleccionado, cantidad, precioSeleccionado, subtotal));

                    resetearInputsProducto();
                } else {
                    JOptionPane.showMessageDialog(this, "Stock insuficiente.");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese una cantidad válida.");
        }
    }

    /**
     * Finaliza la orden: crea la boleta en DB, registra en historial, genera PDF.
     */
    private void finalizarBoleta() {
        if (itemsOrden.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La orden está vacía. Agregá al menos un ítem.");
            return;
        }

        String dni = txtDni.getText().trim();
        String nombre = txtNombre.getText().trim();
        if (esTextoDeEstado(nombre)) {
            JOptionPane.showMessageDialog(this, "Primero buscá o ingresá el nombre del cliente.");
            return;
        }

        // Leer método de pago y descuento
        String metodoPago = (String) cmbMetodoPago.getSelectedItem();
        double descuentoPct = 0;
        if ("EFECTIVO".equals(metodoPago)) {
            try {
                descuentoPct = Double.parseDouble(txtDescuento.getText().trim());
            } catch (NumberFormatException ignored) {}
        }

        String fechaHoy = java.time.LocalDate.now().toString();

        // Convert UI items to service DTO
        java.util.List<io.github.ramiro.escapesj.servicio.ItemFacturacion> itemsDto = itemsOrden.stream()
                .map(item -> new io.github.ramiro.escapesj.servicio.ItemFacturacion(
                        item.tipo(), item.descripcion(), item.codigoProducto(), item.cantidad(), item.precioUnitario()))
                .toList();

        io.github.ramiro.escapesj.servicio.FacturacionRequest request = new io.github.ramiro.escapesj.servicio.FacturacionRequest(
                dni, nombre, fechaHoy, itemsDto, descuentoPct
        );

        io.github.ramiro.escapesj.servicio.FacturacionResult resultadoFacturacion;

        try {
            resultadoFacturacion = facturacionService.facturarOrden(request);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error procesando la facturación. Los cambios fueron revertidos.\n" + e.getMessage(),
                    "Error en Transacción", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return; // Detener flujo, no limpiar el formulario ni generar PDF
        }

        String carpetaPdf = configRepository.getRutaBoletas();
        try {
            String rutaPdf = BoletaPdfService.generarPdf(resultadoFacturacion.numero(), fechaHoy, dni, nombre,
                    resultadoFacturacion.items(), resultadoFacturacion.subtotal(), metodoPago, descuentoPct, carpetaPdf);

            String resumen = "✅ Boleta #" + resultadoFacturacion.numero() + " generada correctamente.\n\n"
                    + "Subtotal: $" + String.format("%,.0f", resultadoFacturacion.subtotal()) + "\n";
            if (descuentoMonto > 0) {
                resumen += "Descuento (" + String.format("%.0f", descuentoPct) + "%): -$"
                        + String.format("%,.0f", descuentoMonto) + "\n";
            }
            resumen += "Total: $" + String.format("%,.0f", resultadoFacturacion.totalFinal()) + "\n"
                    + "Método: " + metodoPago + "\n\n"
                    + "PDF guardado en:\n" + rutaPdf;

            JOptionPane.showMessageDialog(this, resumen,
                    "Boleta Generada", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Boleta guardada en DB pero hubo un error al generar el PDF:\n" + ex.getMessage(),
                    "Error PDF", JOptionPane.WARNING_MESSAGE);
            ex.printStackTrace();
        }

        // Limpiar todo el formulario solo después de que la factura se guardó exitosamente
        limpiarFormularioCompleto();
    }

    private void resetearInputsProducto() {
        txtCodProducto.setText("Ninguno seleccionado");
        txtCodProducto.setForeground(new Color(150, 150, 150));
        txtCantidad.setText("1");
        precioSeleccionado = BigDecimal.ZERO;
        descripcionProductoSeleccionado = "";
        codigoProductoSeleccionado = "";
        lblProductoInfo.setText(" ");
    }

    private void limpiarFormularioCompleto() {
        txtDni.setText("");
        txtNombre.setText(PLACEHOLDER_NOMBRE);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setEditable(false);
        txtNombre.setFocusable(false);
        txtNombre.setBackground(new Color(30, 35, 48));
        txtDescripcion.setText("");
        txtMonto.setText("0");
        resetearInputsProducto();
        modeloTabla.setRowCount(0);
        itemsOrden.clear();
        SwingUtilities.invokeLater(() -> txtDni.requestFocusInWindow());
    }

    // ════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════

    private JPanel crearCampoPanel(String labelText, String hint) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(new Color(200, 200, 200));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        wrapper.add(lbl, BorderLayout.NORTH);

        JTextField f = new JTextField();
        estilizarComponente(f);
        configurarPlaceholder(f, hint);
        wrapper.add(f, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel crearSeparador(String titulo) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel lbl = new JLabel(titulo);
        lbl.setForeground(new Color(52, 152, 219));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        pnl.add(lbl, BorderLayout.WEST);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(70, 80, 105));
        pnl.add(sep, BorderLayout.SOUTH);

        return pnl;
    }

    private void configurarPlaceholder(JTextField f, String hint) {
        f.setText(hint);
        f.setForeground(new Color(150, 150, 150));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(hint)) {
                    f.setText("");
                    f.setForeground(Color.WHITE);
                }
            }

            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(hint);
                    f.setForeground(new Color(150, 150, 150));
                }
            }
        });
    }

    private void estilizarComponente(JComponent c) {
        c.setBackground(new Color(45, 52, 71));
        c.setForeground(Color.WHITE);
        if (c instanceof JTextComponent tc) tc.setCaretColor(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 105), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setRowHeight(30);
        t.setGridColor(new Color(70, 80, 105));
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.getTableHeader().setBackground(new Color(30, 35, 48));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
    }

    private class PanelCabecera extends JPanel {
        private Image logo;

        public PanelCabecera() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 120));
            URL logoUrl = getClass().getResource("/Logo.png");
            if (logoUrl != null) {
                logo = new ImageIcon(logoUrl).getImage();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (logo != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g2d.drawImage(logo, (getWidth() - 160) / 2, (getHeight() - 160) / 2, 160, 160, this);
                g2d.dispose();
            }
            super.paintComponent(g);
        }
    }
}