package io.github.ramiro.escapesj.vista;

import com.toedter.calendar.JDateChooser;
import io.github.ramiro.escapesj.modelo.ClienteRepresentador;
import io.github.ramiro.escapesj.modelo.Emisor;
import io.github.ramiro.escapesj.modelo.ProductoRepresentador;
import io.github.ramiro.escapesj.persistencia.EmisorRepository;
import io.github.ramiro.escapesj.persistencia.PresupuestoRepository;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.servicio.PresupuestoPdfService;
import io.github.ramiro.escapesj.servicio.PresupuestoPdfService.ItemPresupuesto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Ventana para generar presupuestos con múltiples ítems, código único y fecha de validez.
 */
public class VentanaPresupuesto extends JFrame {
    private final AfipService afipService;
    private final PresupuestoRepository presupuestoRepo;
    private final ProductoRepository productoRepo;
    private final io.github.ramiro.escapesj.persistencia.ConfigRepository configRepository;
    private final EmisorRepository emisorRepo = new EmisorRepository();

    private JTextField txtDni, txtNombre, txtDescripcion, txtMonto, txtCodProducto, txtCantidad;
    private JDateChooser dateChooserLimite;
    private JLabel lblProductoInfo;
    private DefaultTableModel modeloTabla;
    private JComboBox<Emisor> comboEmisores;
    private static final String PLACEHOLDER_NOMBRE = "Se completa automáticamente";

    // Ítems acumulados
    private final List<ItemPresupuesto> itemsPresupuesto = new ArrayList<>();

    // Producto seleccionado del inventario
    private String descProductoSel = "";
    private BigDecimal precioProductoSel = BigDecimal.ZERO;

    public VentanaPresupuesto(AfipService afipService, PresupuestoRepository presupuestoRepo,
                               ProductoRepository productoRepo,
                               io.github.ramiro.escapesj.persistencia.ConfigRepository configRepo) {
        this.afipService = afipService;
        this.presupuestoRepo = presupuestoRepo;
        this.productoRepo = productoRepo;
        this.configRepository = configRepo;
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Generar Presupuesto");
        setSize(780, 720);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(0, 43, 91));

        // ═══════════════════════════════════════════
        //  CABECERA
        // ═══════════════════════════════════════════
        PanelCabecera cabecera = new PanelCabecera();
        cabecera.setLayout(new GridBagLayout());
        cabecera.setPreferredSize(new Dimension(0, 70));
        JLabel lblTitulo = new JLabel("GENERAR PRESUPUESTO");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        cabecera.add(lblTitulo);
        mainPanel.add(cabecera, BorderLayout.NORTH);

        // ═══════════════════════════════════════════
        //  FORMULARIO
        // ═══════════════════════════════════════════
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // ── DATOS DEL CLIENTE ──
        formPanel.add(crearSeparador("DATOS DEL CLIENTE"));
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 1: [DNI] [Buscar] [Nombre]
        JPanel filaDni = new JPanel(new GridBagLayout());
        filaDni.setOpaque(false);
        filaDni.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.35; gbc.weighty = 1;
        JPanel pnlDni = crearCampoPanel("DNI del Cliente:", "Ej: 46000698");
        txtDni = extraerTextField(pnlDni);
        filaDni.add(pnlDni, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        gbc.insets = new Insets(14, 0, 0, 6);
        JButton btnBuscar = crearBoton("🔍 Buscar", new Color(52, 152, 219), 90);
        filaDni.add(btnBuscar, gbc);

        gbc.gridx = 2; gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlNombre = new JPanel(new BorderLayout());
        pnlNombre.setOpaque(false);
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(new Color(200, 200, 200));
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlNombre.add(lblNombre, BorderLayout.NORTH);
        txtNombre = new JTextField(PLACEHOLDER_NOMBRE);
        estilizarComponente(txtNombre);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setEditable(false);
        txtNombre.setFocusable(false);
        txtNombre.setBackground(new Color(30, 35, 48));
        pnlNombre.add(txtNombre, BorderLayout.CENTER);
        filaDni.add(pnlNombre, gbc);

        formPanel.add(filaDni);
        formPanel.add(Box.createVerticalStrut(6));

        // ── AGREGAR ÍTEM AL PRESUPUESTO ──
        formPanel.add(crearSeparador("AGREGAR ÍTEM"));
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 2: [Descripción] [Monto]
        JPanel filaDesc = new JPanel(new GridBagLayout());
        filaDesc.setOpaque(false);
        filaDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.7; gbc.weighty = 1;
        JPanel pnlDesc = crearCampoPanel("Descripción (mano de obra/trabajo):", "Ej: Instalación silenciador");
        txtDescripcion = extraerTextField(pnlDesc);
        filaDesc.add(pnlDesc, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlMonto = crearCampoPanel("Monto ($):", "0");
        txtMonto = extraerTextField(pnlMonto);
        filaDesc.add(pnlMonto, gbc);

        formPanel.add(filaDesc);
        formPanel.add(Box.createVerticalStrut(4));

        // FILA 3: [Producto (opcional)] [Buscar] [Cantidad]
        JPanel filaProd = new JPanel(new GridBagLayout());
        filaProd.setOpaque(false);
        filaProd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);

        gbc.gridx = 0; gbc.weightx = 0.55; gbc.weighty = 1;
        JPanel pnlProd = new JPanel(new BorderLayout());
        pnlProd.setOpaque(false);
        JLabel lblProd = new JLabel("Producto (opcional):");
        lblProd.setForeground(new Color(200, 200, 200));
        lblProd.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlProd.add(lblProd, BorderLayout.NORTH);
        txtCodProducto = new JTextField("Ninguno seleccionado");
        estilizarComponente(txtCodProducto);
        txtCodProducto.setEditable(false);
        txtCodProducto.setBackground(new Color(30, 35, 48));
        txtCodProducto.setForeground(new Color(150, 150, 150));
        pnlProd.add(txtCodProducto, BorderLayout.CENTER);
        filaProd.add(pnlProd, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        gbc.insets = new Insets(14, 0, 0, 6);
        JButton btnBuscarProd = crearBoton("📦 Buscar", new Color(46, 125, 50), 90);
        filaProd.add(btnBuscarProd, gbc);

        gbc.gridx = 2; gbc.weightx = 0.2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pnlCant = crearCampoPanel("Cantidad:", "1");
        txtCantidad = extraerTextField(pnlCant);
        filaProd.add(pnlCant, gbc);

        formPanel.add(filaProd);

        lblProductoInfo = new JLabel(" ");
        lblProductoInfo.setForeground(new Color(100, 200, 100));
        lblProductoInfo.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblProductoInfo.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 0));
        formPanel.add(lblProductoInfo);
        formPanel.add(Box.createVerticalStrut(4));

        // Botón Agregar
        JButton btnAgregar = new JButton("➕  Agregar al Presupuesto");
        btnAgregar.setBackground(new Color(231, 76, 60));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAgregar.setFocusPainted(false);
        btnAgregar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAgregar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnAgregar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAgregar.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60).brighter(), 1));
        formPanel.add(btnAgregar);

        // ── VALIDEZ ──
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(crearSeparador("VALIDEZ"));
        formPanel.add(Box.createVerticalStrut(4));

        JPanel filaFecha = new JPanel(new GridBagLayout());
        filaFecha.setOpaque(false);
        filaFecha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0; gbc.weightx = 0.45; gbc.weighty = 1;
        JPanel pnlFecha = new JPanel(new BorderLayout());
        pnlFecha.setOpaque(false);
        JLabel lblFecha = new JLabel("Válido Hasta:");
        lblFecha.setForeground(new Color(200, 200, 200));
        lblFecha.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pnlFecha.add(lblFecha, BorderLayout.NORTH);
        dateChooserLimite = new JDateChooser();
        dateChooserLimite.setDateFormatString("dd/MM/yyyy");
        // Default: 30 días a partir de hoy
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        dateChooserLimite.setDate(cal.getTime());
        dateChooserLimite.setMinSelectableDate(new Date()); // no permitir fechas pasadas
        dateChooserLimite.setBackground(new Color(45, 52, 71));
        dateChooserLimite.setForeground(Color.WHITE);
        dateChooserLimite.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pnlFecha.add(dateChooserLimite, BorderLayout.CENTER);
        filaFecha.add(pnlFecha, gbc);

        gbc.gridx = 1; gbc.weightx = 0.55;
        gbc.insets = new Insets(0, 10, 0, 0);
        JPanel pnlInfoFecha = new JPanel(new BorderLayout());
        pnlInfoFecha.setOpaque(false);
        JLabel lblInfoFecha = new JLabel("Se garantiza el precio hasta esta fecha");
        lblInfoFecha.setForeground(new Color(150, 200, 150));
        lblInfoFecha.setFont(new Font("SansSerif", Font.ITALIC, 11));
        pnlInfoFecha.add(lblInfoFecha, BorderLayout.SOUTH);
        filaFecha.add(pnlInfoFecha, gbc);

        formPanel.add(filaFecha);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        //  TABLA + BOTONES (abajo)
        // ═══════════════════════════════════════════
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(0, 250));

        // Header: título + quitar
        JPanel pnlTablaHeader = new JPanel(new BorderLayout());
        pnlTablaHeader.setOpaque(false);
        JLabel lblItems = new JLabel("  ÍTEMS DEL PRESUPUESTO");
        lblItems.setForeground(new Color(155, 89, 182));
        lblItems.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblItems.setBorder(BorderFactory.createEmptyBorder(5, 10, 3, 0));
        pnlTablaHeader.add(lblItems, BorderLayout.WEST);

        JButton btnQuitar = new JButton("❌ Quitar Seleccionado");
        btnQuitar.setBackground(new Color(192, 57, 43));
        btnQuitar.setForeground(Color.WHITE);
        btnQuitar.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnQuitar.setFocusPainted(false);
        btnQuitar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuitar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        pnlTablaHeader.add(btnQuitar, BorderLayout.EAST);
        bottomPanel.add(pnlTablaHeader, BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel(new Object[]{"Descripción", "Cant.", "Precio", "Subtotal"}, 0);
        JTable tabla = new JTable(modeloTabla);
        estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(new Color(45, 52, 71));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        bottomPanel.add(scroll, BorderLayout.CENTER);

        // Botones: Emisor + Verificar + Generar
        JPanel pnlOpcionesInf = new JPanel(new BorderLayout());
        pnlOpcionesInf.setBackground(new Color(45, 52, 71));

        JPanel pnlEmisor = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlEmisor.setBackground(new Color(45, 52, 71));
        pnlEmisor.add(new JLabel("<html><font color='white'>Emisor:</font></html>"));
        comboEmisores = new JComboBox<>();
        cargarEmisores();
        pnlEmisor.add(comboEmisores);

        JButton btnAgregarEmisor = new JButton("➕");
        btnAgregarEmisor.setToolTipText("Añadir nuevo emisor");
        btnAgregarEmisor.addActionListener(e -> {
            DialogoAgregarEmisor diag = new DialogoAgregarEmisor(this, emisorRepo, nuevo -> {
                cargarEmisores();
                comboEmisores.setSelectedItem(nuevo);
            });
            diag.setVisible(true);
        });
        pnlEmisor.add(btnAgregarEmisor);
        pnlOpcionesInf.add(pnlEmisor, BorderLayout.NORTH);

        JPanel pnlBotones = new JPanel(new GridLayout(1, 2, 8, 0));
        pnlBotones.setOpaque(false);
        pnlBotones.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JButton btnVerificar = new JButton("🔎 Verificar Presupuesto");
        btnVerificar.setBackground(new Color(243, 156, 18));
        btnVerificar.setForeground(Color.WHITE);
        btnVerificar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnVerificar.setFocusPainted(false);
        btnVerificar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerificar.setBorder(BorderFactory.createLineBorder(new Color(243, 156, 18).brighter(), 1));
        pnlBotones.add(btnVerificar);

        JButton btnGenerar = new JButton("📄 Generar Presupuesto");
        btnGenerar.setBackground(new Color(155, 89, 182));
        btnGenerar.setForeground(Color.WHITE);
        btnGenerar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGenerar.setFocusPainted(false);
        btnGenerar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGenerar.setPreferredSize(new Dimension(0, 42));
        btnGenerar.setBorder(BorderFactory.createLineBorder(new Color(155, 89, 182).brighter(), 1));
        pnlBotones.add(btnGenerar);

        pnlOpcionesInf.add(pnlBotones, BorderLayout.CENTER);
        bottomPanel.add(pnlOpcionesInf, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // ═══════════════════════════════════════════
        //  EVENTOS
        // ═══════════════════════════════════════════
        btnBuscar.addActionListener(e -> buscarCliente(btnBuscar));
        txtDni.addActionListener(e -> buscarCliente(btnBuscar));
        btnBuscarProd.addActionListener(e -> abrirBuscadorProducto());
        btnAgregar.addActionListener(e -> agregarItem());
        btnQuitar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Seleccioná un ítem de la tabla para quitar.");
                return;
            }
            itemsPresupuesto.remove(fila);
            modeloTabla.removeRow(fila);
        });
        btnGenerar.addActionListener(e -> generarPresupuesto());
        btnVerificar.addActionListener(e -> {
            String codigo = JOptionPane.showInputDialog(this,
                    "Ingresá el código del presupuesto (ej: PRE-0001):",
                    "Verificar Presupuesto", JOptionPane.QUESTION_MESSAGE);
            if (codigo != null && !codigo.isBlank()) verificarPresupuesto(codigo.trim());
        });
    }

    // ════════════════════════════════════════
    //  LÓGICA
    // ════════════════════════════════════════

    private void cargarEmisores() {
        comboEmisores.removeAllItems();
        try {
            for (Emisor e : emisorRepo.listarTodos()) {
                comboEmisores.addItem(e);
            }
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "cargar emisores", ex);
        }
        if (comboEmisores.getItemCount() > 0) {
            comboEmisores.setSelectedIndex(0);
        }
    }

    private void buscarCliente(JButton btnBuscar) {
        String dni = txtDni.getText().trim();
        if (dni.isEmpty() || dni.startsWith("Ej:")) {
            JOptionPane.showMessageDialog(this, "Ingresá un DNI válido.");
            return;
        }
        if (!dni.matches("\\d{7,8}")) {
            JOptionPane.showMessageDialog(this, "El DNI debe tener 7 u 8 dígitos numéricos.",
                    "DNI inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombreActual = txtNombre.getText().trim();
        if (!esTextoDeEstado(nombreActual)) {
            txtDescripcion.requestFocus();
            return;
        }

        btnBuscar.setEnabled(false);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setText("Buscando...");

        afipService.buscarClientePorDniAsync(dni)
            .thenAcceptAsync(resultado -> {
                btnBuscar.setEnabled(true);
                if (resultado.isPresent()) {
                    final String[] nombre = {null};
                    resultado.get().presentarseEn(new ClienteRepresentador() {
                        public void definirDni(String cuit) {}
                        public void definirNombre(String n) { nombre[0] = n; }
                    });

                    if (nombre[0] != null && !nombre[0].isBlank()) {
                        txtDni.setText(dni);
                        txtNombre.setText(nombre[0]);
                        txtNombre.setForeground(Color.WHITE);
                        txtDescripcion.requestFocus();
                        return;
                    }
                }

                txtDni.setText(dni);
                txtNombre.setText("");
                txtNombre.setForeground(Color.WHITE);
                txtNombre.setEditable(true);
                txtNombre.setFocusable(true);
                txtNombre.setBackground(new Color(60, 60, 80));
                txtNombre.requestFocus();
            }, SwingUtilities::invokeLater)
            .exceptionally(ex -> {
                Throwable cause = ex;
                while (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException) {
                    cause = cause.getCause();
                }
                if (cause instanceof io.github.ramiro.escapesj.persistencia.PersistenceException pEx) {
                    SwingUtilities.invokeLater(() -> {
                        btnBuscar.setEnabled(true);
                        txtNombre.setText("");
                        txtNombre.setForeground(Color.WHITE);
                        txtNombre.setEditable(true);
                        txtNombre.setBackground(new Color(60, 60, 80));
                        ErrorHandler.mostrarErrorPersistencia(VentanaPresupuesto.this, "buscar cliente", pEx);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        btnBuscar.setEnabled(true);
                        txtDni.setText(dni);
                        txtNombre.setText("Error al buscar");
                        txtNombre.setForeground(new Color(255, 100, 100));
                    });
                }
                return null;
            });
    }

    private void abrirBuscadorProducto() {
        DialogoInventario dialogo = new DialogoInventario(this, productoRepo, producto -> {
            producto.presentarseEn(new ProductoRepresentador() {
                public void definirCodigo(String c) { txtCodProducto.setText(c); }
                public void definirDescripcion(String d) { descProductoSel = d; }
                public void definirPrecio(BigDecimal p) { precioProductoSel = p; }
            });
            lblProductoInfo.setText("📦 " + descProductoSel + " — $" + String.format("%,.0f", precioProductoSel));
        });
        dialogo.setVisible(true);
    }

    private void agregarItem() {
        String detalle = txtDescripcion.getText().trim();
        String montoStr = txtMonto.getText().trim();
        String cantStr = txtCantidad.getText().trim();
        String codProd = txtCodProducto.getText().trim();

        boolean tieneServicio = !detalle.isEmpty() && !detalle.startsWith("Ej:");
        boolean tieneProducto = !codProd.isEmpty() && !codProd.contains("Ninguno");

        if (!tieneServicio && !tieneProducto) {
            JOptionPane.showMessageDialog(this, "Ingresá una descripción o seleccioná un producto.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantStr.isEmpty() ? "1" : cantStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }

        // Agregar servicio/mano de obra
        if (tieneServicio) {
            BigDecimal monto = BigDecimal.ZERO;
            try {
                String limpio = montoStr.replace("$", "").replace(".", "").replace(",", ".").trim();
                if (!limpio.isEmpty() && !limpio.equals("0")) monto = new BigDecimal(limpio);
            } catch (NumberFormatException ignored) {}

            BigDecimal sub = monto.multiply(BigDecimal.valueOf(cantidad));
            itemsPresupuesto.add(new ItemPresupuesto(detalle, cantidad, monto, sub));
            modeloTabla.addRow(new Object[]{detalle, cantidad,
                    "$" + String.format("%,.0f", monto), "$" + String.format("%,.0f", sub)});
            txtDescripcion.setText("");
            txtMonto.setText("0");
        }

        // Agregar producto
        if (tieneProducto) {
            BigDecimal sub = precioProductoSel.multiply(BigDecimal.valueOf(cantidad));
            itemsPresupuesto.add(new ItemPresupuesto(descProductoSel, cantidad, precioProductoSel, sub));
            modeloTabla.addRow(new Object[]{descProductoSel, cantidad,
                    "$" + String.format("%,.0f", precioProductoSel), "$" + String.format("%,.0f", sub)});

            txtCodProducto.setText("Ninguno seleccionado");
            txtCodProducto.setForeground(new Color(150, 150, 150));
            lblProductoInfo.setText(" ");
            precioProductoSel = BigDecimal.ZERO;
            descProductoSel = "";
        }

        txtCantidad.setText("1");
    }

    private void generarPresupuesto() {
        if (itemsPresupuesto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregá al menos un ítem al presupuesto.");
            return;
        }

        String dni = txtDni.getText().trim();
        String nombre = txtNombre.getText().trim();
        if (esTextoDeEstado(nombre)) {
            JOptionPane.showMessageDialog(this, "Primero buscá el cliente por DNI.");
            return;
        }

        Date fechaSeleccionada = dateChooserLimite.getDate();
        if (fechaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una fecha límite del calendario.");
            return;
        }
        LocalDate fechaLimite = fechaSeleccionada.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!esFechaLimiteValida(fechaLimite, LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "La fecha límite debe ser posterior a hoy.");
            return;
        }
        String fechaLimiteStr = fechaLimite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        BigDecimal totalEstimado = itemsPresupuesto.stream().map(ItemPresupuesto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Armar descripción concatenada para la BD
        StringBuilder descBd = new StringBuilder();
        for (ItemPresupuesto item : itemsPresupuesto) {
            if (descBd.length() > 0) descBd.append(" | ");
            descBd.append(item.descripcion()).append(" x").append(item.cantidad());
        }

        String fechaHoy = LocalDate.now().toString();
        String fechaLimiteISO = fechaLimite.toString();

        Emisor emisor = (Emisor) comboEmisores.getSelectedItem();
        if (emisor == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un emisor. Puede agregarlo usando el botón '+'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String codigo;
        try {
            codigo = presupuestoRepo.crearPresupuesto(
                    dni, nombre, descBd.toString(), totalEstimado, fechaHoy, fechaLimiteISO);
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "generar presupuesto", e);
            return;
        }

        if (codigo == null) {
            JOptionPane.showMessageDialog(this, "Error al guardar el presupuesto.");
            return;
        }

        String carpetaPdf = configRepository.getRutaPresupuestos();
        try {
            String rutaPdf = PresupuestoPdfService.generarPdf(
                    codigo, fechaHoy, fechaLimiteISO,
                    dni, nombre, itemsPresupuesto, totalEstimado, carpetaPdf, emisor);

            JOptionPane.showMessageDialog(this,
                    "✅ Presupuesto generado correctamente.\n\n"
                    + "Código: " + codigo + "\n"
                    + "Total estimado: $" + String.format("%,.0f", totalEstimado) + "\n"
                    + "Válido hasta: " + fechaLimiteStr + "\n\n"
                    + "PDF guardado en:\n" + rutaPdf,
                    "Presupuesto Generado", JOptionPane.INFORMATION_MESSAGE);

            limpiar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificarPresupuesto(String codigo) {
        io.github.ramiro.escapesj.persistencia.PresupuestoRepository.Presupuesto p;
        try {
            p = presupuestoRepo.buscarPorCodigo(codigo.toUpperCase());
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException e) {
            ErrorHandler.mostrarErrorPersistencia(this, "verificar presupuesto", e);
            return;
        }

        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "❌ No se encontró ningún presupuesto con código: " + codigo,
                    "No Encontrado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean vigente = esPresupuestoVigente(p.fechaLimite(), LocalDate.now());
        String estado = vigente ? "✅ VIGENTE" : "⚠️ VENCIDO";

        JOptionPane.showMessageDialog(this,
                "Presupuesto: " + p.codigoUnico() + "\n"
                + "Estado: " + estado + "\n\n"
                + "Cliente: " + p.nombreCliente() + "\n"
                + "DNI: " + p.dniCliente() + "\n"
                + "Trabajo: " + p.descripcionTrabajo() + "\n"
                + "Monto: $" + String.format("%,.0f", p.montoEstimado()) + "\n\n"
                + "Emitido: " + io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(p.fechaEmision()) + "\n"
                + "Válido hasta: " + io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(p.fechaLimite()),
                "Presupuesto Encontrado", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean esFechaLimiteValida(LocalDate fechaLimite, LocalDate hoy) {
        if (fechaLimite == null || hoy == null) {
            return false;
        }
        return fechaLimite.isAfter(hoy);
    }

    public static boolean esPresupuestoVigente(String fechaLimiteISO, LocalDate hoy) {
        if (fechaLimiteISO == null || hoy == null) {
            return false;
        }
        try {
            LocalDate fechaLimite = LocalDate.parse(fechaLimiteISO);
            return !fechaLimite.isBefore(hoy);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean esTextoDeEstado(String texto) {
        return texto.equals(PLACEHOLDER_NOMBRE) || texto.equals("Buscando...")
                || texto.equals("No encontrado") || texto.equals("Error al buscar")
                || texto.isEmpty();
    }

    private void limpiar() {
        txtDni.setText("");
        txtNombre.setText(PLACEHOLDER_NOMBRE);
        txtNombre.setForeground(new Color(150, 150, 150));
        txtNombre.setEditable(false);
        txtNombre.setFocusable(false);
        txtNombre.setBackground(new Color(30, 35, 48));
        txtDescripcion.setText("");
        txtMonto.setText("0");
        txtCodProducto.setText("Ninguno seleccionado");
        txtCodProducto.setForeground(new Color(150, 150, 150));
        lblProductoInfo.setText(" ");
        txtCantidad.setText("1");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        dateChooserLimite.setDate(cal.getTime());
        itemsPresupuesto.clear();
        modeloTabla.setRowCount(0);
        txtDni.requestFocusInWindow();
    }

    // ════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════

    private void estilizarComponente(JTextComponent c) {
        c.setBackground(new Color(45, 52, 71));
        c.setForeground(Color.WHITE);
        c.setCaretColor(Color.WHITE);
        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.setBorder(BorderFactory.createLineBorder(new Color(70, 80, 105)));
    }

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

    private JTextField extraerTextField(JPanel panel) {
        return (JTextField) ((BorderLayout) panel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
    }

    private JButton crearBoton(String texto, Color color, int ancho) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(ancho, 30));
        b.setBorder(BorderFactory.createLineBorder(color.brighter(), 1));
        return b;
    }

    private JPanel crearSeparador(String titulo) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(titulo);
        lbl.setForeground(new Color(155, 89, 182));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
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
                if (f.getText().equals(hint)) { f.setText(""); f.setForeground(Color.WHITE); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(hint); f.setForeground(new Color(150, 150, 150)); }
            }
        });
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setBackground(new Color(45, 52, 71));
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(70, 80, 105));
        tabla.setSelectionBackground(new Color(52, 152, 219));
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabla.getTableHeader().setBackground(new Color(30, 35, 48));
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.setRowHeight(22);
    }

    private class PanelCabecera extends JPanel {
        private Image logo;
        public PanelCabecera() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 120));
            URL logoUrl = getClass().getResource("/Logo.png");
            if (logoUrl != null) logo = new ImageIcon(logoUrl).getImage();
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
