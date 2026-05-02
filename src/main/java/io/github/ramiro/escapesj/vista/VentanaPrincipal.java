package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Cliente;
import io.github.ramiro.escapesj.modelo.ClienteRepresentador;
import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.modelo.ProductoRepresentador;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.sdk.AfipService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;

public class VentanaPrincipal extends JFrame {
    private final AfipService afipService;
    private final Inventario inventario;
    private DefaultTableModel modeloTabla;
    private JTextField txtDni, txtNombre, txtCodProducto, txtCantidad, txtDescripcion;
    private JButton btnBuscar;
    private final ProductoRepository productoRepository;

    public VentanaPrincipal(AfipService afipService, Inventario inventario, ProductoRepository repo) {
        this.afipService = afipService;
        this.inventario = inventario;
        this.productoRepository = repo;
        initUI();

    }

    private void initUI() {
        setTitle("EscapesJ - Gestión de Taller");
        setSize(500, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(0, 43, 91)); // Azul Logo

        // 1. Cabecera con Logo detrás
        PanelCabecera cabecera = new PanelCabecera();
        cabecera.setLayout(new GridBagLayout());
        JLabel lblTitulo = new JLabel("FORMULARIO DE SERVICIO");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitulo.setForeground(Color.WHITE);
        cabecera.add(lblTitulo);
        mainPanel.add(cabecera, BorderLayout.NORTH);

        // 2. Panel de Entrada (DNI y Producto)
        JPanel entryPanel = new JPanel(new GridLayout(7, 1, 5, 5)); // Aumentamos filas a 7
        entryPanel.setOpaque(false);
        entryPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        txtNombre = crearCampoConGuia("Nombre del Cliente", "Se cargará desde AFIP...", entryPanel);
        txtNombre.setEditable(false);
        txtNombre.setFocusable(false); // Evitamos que el usuario haga click ahí
        txtDni = crearCampoConGuia("DNI Cliente", "DNI para AFIP", entryPanel);
        txtDescripcion = crearCampoConGuia("Descripción del Pedido", "Ej: Cambio de silenciador", entryPanel);

        txtCodProducto = crearCampoConGuia("Código Producto", "Acá aparece el código que tiene en el inventario", entryPanel);
        txtCodProducto.setEditable(false);
        txtCodProducto.setBackground(new Color(30, 35, 48)); // Fondo oscuro "modo lectura"
        txtCodProducto.setForeground(Color.WHITE);          // Texto blanco para legibilidad
        txtCodProducto.setCaretColor(Color.WHITE);
        txtCantidad = crearCampoConGuia("Cantidad", "1", entryPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JButton btnAgregar = new JButton("Agregar a la Orden");
        JButton btnBuscar = new JButton("Buscar Producto en Inventario");
        estilizarComponente(btnBuscar);
        btnBuscar.addActionListener(e -> {
            // Le pasamos el repositorio y una función (lambda) que dice qué hacer con el producto elegido
            DialogoInventario dialogo = new DialogoInventario(this, productoRepository, producto -> {
                // Aquí recibimos el producto seleccionado y llenamos los campos de texto
                producto.presentarseEn(new ProductoRepresentador() {
                    public void definirCodigo(String c) {
                        txtCodProducto.setText(c);
                    }

                    public void definirDescripcion(String d) { /* Podrías mostrarla en un label si querés */ }

                    public void definirPrecio(double p) { /* Guardar el precio para el cálculo */ }
                });
            });
            dialogo.setVisible(true);
        });

        entryPanel.add(btnBuscar);
        btnAgregar.setBackground(new Color(237, 28, 36)); // Rojo Logo
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.addActionListener(e -> agregarProductoALista());
        entryPanel.add(btnAgregar);

        mainPanel.add(entryPanel, BorderLayout.CENTER);

        // 3. Tabla de Stackeo (Lista de productos agregados)
        modeloTabla = new DefaultTableModel(new Object[]{"Producto", "Cant.", "Subtotal"}, 0);
        JTable tabla = new JTable(modeloTabla);
        estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(0, 250));
        scroll.getViewport().setBackground(new Color(45, 52, 71));

        mainPanel.add(scroll, BorderLayout.SOUTH);

        add(mainPanel);
        configurarEventos();
    }

    private void agregarProductoALista() {
        String codigo = txtCodProducto.getText();
        // Usamos el Pipeline de Inventario con Optional
        inventario.buscarPorCodigo(codigo).ifPresentOrElse(
                producto -> {
                    // El producto se representa a sí mismo en la tabla (Tell, Don't Ask)
                    producto.representarEnFila(fila -> modeloTabla.addRow(fila));
                    txtCodProducto.setText("");
                    txtCantidad.setText("1");
                },
                () -> JOptionPane.showMessageDialog(this, "Producto no encontrado en inventario")
        );
    }

    private class PanelCabecera extends JPanel {
        private Image logo;

        public PanelCabecera() {
            setOpaque(false);
            // Aumentamos la altura de la cabecera para que el logo luzca más
            setPreferredSize(new Dimension(0, 180));

            // Usamos ImageIcon para asegurar que la transparencia se maneje bien
            try {
                String ruta = "/home/ramiro/Documentos/escapesJ/Logo.png";
                logo = new ImageIcon(ruta).getImage();
            } catch (Exception e) {
                System.err.println("Error al cargar el logo en: " + e.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (logo != null) {
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));


                int nuevoAncho = 250;
                int nuevoAlto = 250;

                int x = (getWidth() - nuevoAncho) / 2;
                int y = (getHeight() - nuevoAlto) / 2;

                g2d.drawImage(logo, x, y, nuevoAncho, nuevoAlto, this);
                g2d.dispose();
            }
            super.paintComponent(g);
        }
    }

    private JTextField crearCampoFormulario(String label, String placeholder, JPanel container) {
        agregarLabel(label, container);
        JTextField field = new JTextField();
        estilizarComponente(field);
        configurarPlaceholder(field, placeholder);
        container.add(field);
        return field;
    }

    private void agregarLabel(String texto, JPanel container) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(200, 200, 200));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        container.add(lbl);
    }

    private void estilizarComponente(JComponent c) {
        c.setBackground(new Color(45, 52, 71));
        c.setForeground(Color.WHITE);

        if (c instanceof JTextComponent textComp) {
            textComp.setCaretColor(Color.WHITE);
        }

        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 105), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void configurarEventos() {
        txtDni.addActionListener(e -> {
            String dniIngresado = txtDni.getText();

            // Usamos el Service que devuelve un Optional<Cliente>
            afipService.buscarClientePorDni(dniIngresado)
                    .ifPresentOrElse(
                            cliente -> cliente.presentarseEn(new ClienteRepresentador() {
                                @Override
                                public void definirDni(String dni) {
                                    txtDni.setText(dni);
                                }

                                @Override
                                public void definirNombre(String nombre) {
                                    // 3. USO: Ahora txtNombre ya existe y se puede usar
                                    txtNombre.setText(nombre);
                                    txtNombre.setForeground(Color.WHITE);
                                }
                            }),
                            () -> {
                                txtNombre.setText("CLIENTE NO ENCONTRADO");
                                txtNombre.setForeground(new Color(255, 100, 100)); // Rojo sutil
                            }
                    );
        });
    }

    private void configurarPlaceholder(JTextField field, String hint) {
        // Estado inicial
        field.setText(hint);
        field.setForeground(new Color(150, 150, 150));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(hint)) {
                    field.setText("");
                    field.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Si el usuario no escribió nada, vuelve el texto de guía.
                if (field.getText().isEmpty()) {
                    field.setText(hint);
                    field.setForeground(new Color(150, 150, 150));
                }
            }
        });
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setBackground(new Color(45, 52, 71)); // Gris azulado oscuro
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(70, 80, 105));
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabla.setRowHeight(30);
        tabla.setSelectionBackground(new Color(237, 28, 36)); // Rojo Logo al seleccionar
        tabla.setSelectionForeground(Color.WHITE);

        // Estilo del encabezado
        tabla.getTableHeader().setBackground(new Color(30, 35, 48));
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        tabla.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(70, 80, 105)));
    }

    private JTextField crearCampoConGuia(String label, String placeholder, JPanel container) {
        agregarLabel(label, container);
        JTextField field = new JTextField();
        estilizarComponente(field);
        configurarPlaceholder(field, placeholder);
        container.add(field);
        return field;
    }

    private void mostrarDatosCliente(Cliente cliente) {
        // La vista implementa la interface para "recibir" los datos
        cliente.presentarseEn(new ClienteRepresentador() {
            @Override
            public void definirDni(String dni) {
                txtDni.setText(dni);
                txtDni.setForeground(Color.WHITE); // Limpiamos el color de placeholder
            }

            @Override
            public void definirNombre(String nombre) {
                txtNombre.setText(nombre);
            }
        });
    }

    private void completarDesdeAfip(Map<String, Object> datos) {
        // TELL, DON'T ASK: El mapa le dice a los campos qué poner
        // Aquí podrías implementar un método que recorra el mapa sin pedir datos
        System.out.println("Cargando datos: " + datos);
        // Ejemplo: txtServicio.setText(datos.getOrDefault("nombre", "").toString());
    }
}