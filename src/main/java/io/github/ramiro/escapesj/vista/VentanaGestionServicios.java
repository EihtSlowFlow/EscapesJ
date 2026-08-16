package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.ServicioRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VentanaGestionServicios extends JFrame {
    private final ServicioRepository repo;
    private final BoletaRepository boletaRepo;
    private DefaultTableModel modelBoletas;
    private DefaultTableModel modelItems;
    private JTextField txtDni;

    public VentanaGestionServicios(ServicioRepository repo, BoletaRepository boletaRepo) {
        this.repo = repo;
        this.boletaRepo = boletaRepo;
        initUI();
    }

    private void initUI() {
        setTitle("EscapesJ - Historial de Servicios");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(10, 10));

        // PANEL DE BÚSQUEDA (ARRIBA)
        JPanel pnlBusqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        pnlBusqueda.setOpaque(false);

        JLabel lblDni = new JLabel("DNI Cliente:");
        lblDni.setForeground(Color.WHITE);
        lblDni.setFont(new Font("SansSerif", Font.BOLD, 14));

        txtDni = new JTextField(15);
        txtDni.setBackground(new Color(45, 52, 71));
        txtDni.setForeground(Color.WHITE);
        txtDni.setCaretColor(Color.WHITE);
        txtDni.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtDni.setBorder(BorderFactory.createLineBorder(new Color(70, 80, 105)));

        JButton btnBuscar = new JButton("Buscar Historial");
        btnBuscar.setBackground(new Color(52, 152, 219));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFocusPainted(false);

        pnlBusqueda.add(lblDni);
        pnlBusqueda.add(txtDni);
        pnlBusqueda.add(btnBuscar);
        add(pnlBusqueda, BorderLayout.NORTH);

        // PANEL CENTRAL: BOLETAS (arriba) + ITEMS (abajo) con split
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // TABLA DE BOLETAS
        JPanel pnlBoletas = new JPanel(new BorderLayout());
        pnlBoletas.setOpaque(false);
        JLabel lblBoletas = new JLabel("  BOLETAS");
        lblBoletas.setForeground(new Color(52, 152, 219));
        lblBoletas.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblBoletas.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 0));
        pnlBoletas.add(lblBoletas, BorderLayout.NORTH);

        modelBoletas = new DefaultTableModel(new Object[]{"Nro.", "Fecha", "Cliente", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tablaBoletas = new JTable(modelBoletas);
        estilizarTabla(tablaBoletas);
        tablaBoletas.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaBoletas.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaBoletas.getColumnModel().getColumn(2).setPreferredWidth(250);
        tablaBoletas.getColumnModel().getColumn(3).setPreferredWidth(100);

        JScrollPane scrollBoletas = new JScrollPane(tablaBoletas);
        scrollBoletas.getViewport().setBackground(new Color(45, 52, 71));
        scrollBoletas.setBorder(BorderFactory.createEmptyBorder());
        pnlBoletas.add(scrollBoletas, BorderLayout.CENTER);
        splitPane.setTopComponent(pnlBoletas);

        // TABLA DE ITEMS (detalle de la boleta seleccionada)
        JPanel pnlItems = new JPanel(new BorderLayout());
        pnlItems.setOpaque(false);
        JLabel lblItems = new JLabel("  DETALLE DE BOLETA (doble click en una boleta)");
        lblItems.setForeground(new Color(46, 204, 113));
        lblItems.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblItems.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 0));
        pnlItems.add(lblItems, BorderLayout.NORTH);

        modelItems = new DefaultTableModel(
                new Object[]{"Tipo", "Descripción", "Producto", "Cant.", "Precio Unit.", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tablaItems = new JTable(modelItems);
        estilizarTabla(tablaItems);
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaItems.getColumnModel().getColumn(1).setPreferredWidth(250);
        tablaItems.getColumnModel().getColumn(2).setPreferredWidth(80);
        tablaItems.getColumnModel().getColumn(3).setPreferredWidth(50);
        tablaItems.getColumnModel().getColumn(4).setPreferredWidth(80);
        tablaItems.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scrollItems = new JScrollPane(tablaItems);
        scrollItems.getViewport().setBackground(new Color(45, 52, 71));
        scrollItems.setBorder(BorderFactory.createEmptyBorder());
        pnlItems.add(scrollItems, BorderLayout.CENTER);
        splitPane.setBottomComponent(pnlItems);

        add(splitPane, BorderLayout.CENTER);

        // --- EVENTOS ---
        btnBuscar.addActionListener(e -> cargarHistorial(lblItems));
        txtDni.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    cargarHistorial(lblItems);
                }
            }
        });

        // Doble click en una boleta → cargar sus ítems
        tablaBoletas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaBoletas.getSelectedRow();
                    if (fila != -1) {
                        // Obtener el ID de la boleta (guardado como dato oculto)
                        int boletaId = (int) modelBoletas.getValueAt(fila, 0);
                        String nroBoleta = modelBoletas.getValueAt(fila, 0).toString();
                        cargarItemsBoleta(boletaId, lblItems);
                    }
                }
            }
        });

        SwingUtilities.invokeLater(() -> txtDni.requestFocusInWindow());
    }

    private void cargarHistorial(JLabel lblItems) {
        String dniBusqueda = txtDni.getText().trim();
        if (dniBusqueda.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese un DNI para buscar.");
            return;
        }

        modelBoletas.setRowCount(0);
        modelItems.setRowCount(0);
        lblItems.setText("  DETALLE DE BOLETA (doble click en una boleta)");

        try {
            var boletas = boletaRepo.buscarBoletasPorDni(dniBusqueda);
            for (var b : boletas) {
                modelBoletas.addRow(new Object[]{
                        b.id(), // Guardamos el ID en la primera columna (se muestra como Nro.)
                        io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(b.fecha()),
                        b.nombreCliente(),
                        "$" + String.format("%,.0f", b.total())
                });
            }

            // Renombrar primera columna para mostrar el número real
            if (!boletas.isEmpty()) {
                // Recargar con el número de boleta real
                modelBoletas.setRowCount(0);
                for (var b : boletas) {
                    modelBoletas.addRow(new Object[]{
                            b.numero(),
                            io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(b.fecha()),
                            b.nombreCliente(),
                            "$" + String.format("%,.0f", b.total())
                    });
                }
            }

            if (modelBoletas.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No se encontraron boletas para el DNI: " + dniBusqueda);
            }

            // Guardar IDs por separado para el doble click
            // Usamos un truco: guardamos la lista de boletas como client property
            var tablaBoletas = getTablaBoletasDesdeUI();
            if (tablaBoletas != null) {
                tablaBoletas.putClientProperty("boletasList", boletas);
            }
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "buscar historial", ex);
        }
    }

    private void cargarItemsBoleta(int nroBoleta, JLabel lblItems) {
        // Buscar el ID real de la boleta por su número
        var tablaBoletas = getTablaBoletasDesdeUI();
        if (tablaBoletas == null) return;

        @SuppressWarnings("unchecked")
        var boletas = (java.util.List<BoletaRepository.BoletaResumen>)
                tablaBoletas.getClientProperty("boletasList");
        if (boletas == null) return;

        int fila = tablaBoletas.getSelectedRow();
        if (fila < 0 || fila >= boletas.size()) return;

        int boletaId = boletas.get(fila).id();
        int numero = boletas.get(fila).numero();

        modelItems.setRowCount(0);
        lblItems.setText("  DETALLE DE BOLETA #" + numero);

        try {
            var items = boletaRepo.obtenerItems(boletaId);
            for (var item : items) {
                modelItems.addRow(new Object[]{
                        item.tipo(),
                        item.descripcion(),
                        item.codigoProducto() != null ? item.codigoProducto() : "—",
                        item.cantidad(),
                        "$" + String.format("%,.0f", item.precioUnitario()),
                        "$" + String.format("%,.0f", item.subtotal())
                });
            }
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "cargar detalle de boleta", ex);
        }
    }

    /**
     * Busca la JTable de boletas en el árbol de componentes.
     */
    private JTable getTablaBoletasDesdeUI() {
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JSplitPane split) {
                Component top = split.getTopComponent();
                if (top instanceof JPanel panel) {
                    for (Component inner : panel.getComponents()) {
                        if (inner instanceof JScrollPane sp) {
                            if (sp.getViewport().getView() instanceof JTable t) {
                                return t;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setGridColor(new Color(70, 80, 105));
        t.setRowHeight(30);
        ZoomManager.registerBaseRowHeight(t, 30);
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setSelectionBackground(new Color(52, 152, 219));
        t.getTableHeader().setBackground(new Color(30, 35, 48));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
    }
}
