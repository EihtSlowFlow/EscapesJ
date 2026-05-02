package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class DialogoInventario extends JDialog {
    private final ProductoRepository repository;
    private DefaultTableModel modelo;
    private JTable tabla;
    private List<Producto> listaProductos;

    public DialogoInventario(JFrame parent, ProductoRepository repository, Consumer<Producto> alSeleccionar) {
        super(parent, "Seleccionar Producto", true);
        this.repository = repository;

        configurarVentana();
        initUI(alSeleccionar);
        cargarDatosDesdeDB();

        setLocationRelativeTo(parent);
    }

    private void configurarVentana() {
        setSize(500, 400);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(45, 52, 71)); // Estilo Kubuntu Dark
    }

    private void initUI(Consumer<Producto> alSeleccionar) {

        modelo = new DefaultTableModel(new Object[]{"Código", "Descripción", "Precio"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        estilizarTabla(tabla);

        JButton btnSeleccionar = new JButton("Confirmar Selección");
        btnSeleccionar.setBackground(new Color(46, 204, 113));
        btnSeleccionar.setForeground(Color.WHITE);

        btnSeleccionar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila != -1) {
                alSeleccionar.accept(listaProductos.get(fila));
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, seleccioná un producto de la lista.");
            }
        });

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(btnSeleccionar, BorderLayout.SOUTH);
    }

    private void cargarDatosDesdeDB() {
        modelo.setRowCount(0);
        listaProductos = repository.buscarTodos();

        listaProductos.forEach(producto -> {
            producto.representarEnFila(modelo::addRow);
        });
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setGridColor(new Color(70, 80, 105));
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(30);

        t.getTableHeader().setBackground(new Color(0, 43, 91));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        t.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(70, 80, 105)));
    }
}