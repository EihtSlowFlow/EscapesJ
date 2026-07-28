package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class DialogoInventario extends JDialog {
    private final ProductoRepository repository;
    private final Consumer<Producto> alSeleccionar;
    private DefaultTableModel model;
    private JTable tabla;

    public DialogoInventario(JFrame parent, ProductoRepository repository, Consumer<Producto> alSeleccionar) {
        super(parent, "Seleccionar Producto", true);
        this.repository = repository;
        this.alSeleccionar = alSeleccionar;
        initUI();
        cargarDatos();
    }

    private void initUI() {
        setSize(700, 450); // Un poco más ancho para que entre la nueva columna
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout());

        // 1. DEFINICIÓN DE COLUMNAS: Agregamos "Stock" al final
        model = new DefaultTableModel(new Object[]{"Código", "Nombre", "Precio", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(model);
        estilizarTabla(tabla);

        // Selección rápida con doble click
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmarSeleccion();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(new Color(45, 52, 71));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // 2. BOTÓN DE CONFIRMACIÓN
        JButton btnConfirmar = new JButton("Confirmar Selección");
        btnConfirmar.setBackground(new Color(46, 204, 113));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnConfirmar.setPreferredSize(new Dimension(0, 50));
        btnConfirmar.addActionListener(e -> confirmarSeleccion());

        add(btnConfirmar, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        model.setRowCount(0);
        // Traemos todos los productos y agregamos el stock a la fila
        repository.buscarTodos().forEach(p -> {
            model.addRow(new Object[]{
                    p.getCodigo(),
                    p.getNombre(),
                    p.getPrecio(),
                    p.getStock() // Mostramos el stock actual en tiempo real
            });
        });
    }

    private void confirmarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila != -1) {
            String codigo = model.getValueAt(fila, 0).toString();
            repository.buscarPorCodigo(codigo).ifPresent(alSeleccionar);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un producto.");
        }
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setRowHeight(30);
        t.setSelectionBackground(new Color(52, 152, 219));
        t.getTableHeader().setBackground(new Color(30, 35, 48));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
    }
}