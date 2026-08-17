package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;

import javax.swing.*;
import java.awt.*;

public class DialogoOperacionHistorica extends JDialog {
    private final OperacionHistorica op;
    private final OperacionHistoricaRepository repo;
    private final BoletaRepository boletaRepo;
    private final VentanaOperacionesHistoricas parent;

    public DialogoOperacionHistorica(VentanaOperacionesHistoricas parent, OperacionHistorica op, 
                                     OperacionHistoricaRepository repo, BoletaRepository boletaRepo) {
        super(parent, "Detalle Operación: " + op.getId(), true);
        this.parent = parent;
        this.op = op;
        this.repo = repo;
        this.boletaRepo = boletaRepo;
        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel(new GridLayout(8, 1, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        content.setBackground(new Color(30, 35, 48));

        content.add(crearLabel("Fecha: " + op.getFecha()));
        content.add(crearLabel("Ref: " + op.getReferenciaPapel()));
        content.add(crearLabel("Cliente: " + op.getCliente()));
        content.add(crearLabel("Importe: $" + op.getImporteTotal()));
        
        String costoStr = op.getCostoMateriales() == null ? "No configurado" : "$" + op.getCostoMateriales();
        content.add(crearLabel("Costo: " + costoStr));
        
        content.add(crearLabel("Estado: " + op.getEstado()));
        
        String vinc = op.getBoletaDigitalId() == null ? "No vinculado" : "Boleta #" + op.getBoletaDigitalId();
        content.add(crearLabel("Vinculación: " + vinc));

        JPanel pnlBotones = new JPanel(new FlowLayout());
        pnlBotones.setOpaque(false);

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this, "¿Seguro que desea eliminar el registro?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                repo.eliminar(op.getId());
                dispose();
            }
        });
        pnlBotones.add(btnEliminar);

        if (op.getEstado().equals("PENDIENTE")) {
            JButton btnVincular = new JButton("Vincular Boleta Digital");
            btnVincular.addActionListener(e -> vincularBoleta());
            pnlBotones.add(btnVincular);
        }

        content.add(pnlBotones);
        add(content);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void vincularBoleta() {
        String input = JOptionPane.showInputDialog(this, "Ingrese el ID de la Boleta Digital para vincular:");
        if (input != null && !input.isEmpty()) {
            try {
                int boletaId = Integer.parseInt(input);
                try (java.sql.Connection conn = io.github.ramiro.escapesj.persistencia.DatabaseService.getConnection()) {
                    var items = boletaRepo.obtenerItems(conn, boletaId);
                }
                // Aquí solo marcamos como digitalizado y asociamos el boleta_id.
                // Podría agregarse validación extra, pero confiamos en la clave foránea.
                op.setBoletaDigitalId(boletaId);
                op.setEstado("DIGITALIZADO");
                repo.guardar(op);
                JOptionPane.showMessageDialog(this, "Vinculado correctamente.");
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID inválido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private JLabel crearLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }
}
