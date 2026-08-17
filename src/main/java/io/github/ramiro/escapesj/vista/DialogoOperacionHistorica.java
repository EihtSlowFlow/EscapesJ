package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.sdk.DineroUtil;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class DialogoOperacionHistorica extends JDialog {
    private final OperacionHistorica op;
    private final OperacionHistoricaRepository repo;
    private final BoletaRepository boletaRepo;
    private final VentanaOperacionesHistoricas parent;

    private JTextField txtFecha, txtRef, txtCliente, txtImporte, txtCosto, txtDesc, txtObs;

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
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        content.setBackground(new Color(30, 35, 48));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        txtFecha = crearCampo("Fecha (YYYY-MM-DD)", content, gbc);
        txtFecha.setText(op.getFecha());
        
        txtRef = crearCampo("Ref / Nro Boleta Papel", content, gbc);
        txtRef.setText(op.getReferenciaPapel() == null ? "" : op.getReferenciaPapel());
        
        txtCliente = crearCampo("Cliente", content, gbc);
        txtCliente.setText(op.getCliente() == null ? "" : op.getCliente());
        
        txtDesc = crearCampo("Descripción", content, gbc);
        txtDesc.setText(op.getDescripcion());
        
        txtImporte = crearCampo("Importe Total ($)", content, gbc);
        txtImporte.setText(op.getImporteTotal().toString());
        
        txtCosto = crearCampo("Costo Materiales ($)", content, gbc);
        txtCosto.setText(op.getCostoMateriales() == null ? "" : op.getCostoMateriales().toString());
        
        txtObs = crearCampo("Observaciones", content, gbc);
        txtObs.setText(op.getObservaciones() == null ? "" : op.getObservaciones());

        gbc.gridy++;
        content.add(crearLabel("Estado: " + op.getEstado()), gbc);

        gbc.gridy++;
        String vinc = "No vinculado";
        if (op.getBoletaDigitalNumero() != null) {
            vinc = "Boleta #" + op.getBoletaDigitalNumero();
        } else if (op.getBoletaDigitalId() != null) {
            vinc = "Boleta ID:" + op.getBoletaDigitalId();
        }
        content.add(crearLabel("Vinculación: " + vinc), gbc);

        JPanel pnlBotones = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlBotones.setOpaque(false);

        JButton btnGuardar = new JButton("Guardar Cambios");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> guardarCambios());
        pnlBotones.add(btnGuardar);

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.setBackground(new Color(231, 76, 60));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.addActionListener(e -> eliminarOperacion());
        pnlBotones.add(btnEliminar);

        if (op.getEstado().equals("PENDIENTE")) {
            JButton btnVincular = new JButton("Vincular Boleta Digital");
            btnVincular.setBackground(new Color(52, 152, 219));
            btnVincular.setForeground(Color.WHITE);
            btnVincular.addActionListener(e -> vincularBoleta());
            pnlBotones.add(btnVincular);
            
            JButton btnMarcar = new JButton("Marcar Digitalizado");
            btnMarcar.setBackground(new Color(241, 196, 15));
            btnMarcar.addActionListener(e -> marcarDigitalizado());
            pnlBotones.add(btnMarcar);
        }

        gbc.gridy++;
        gbc.insets = new Insets(15, 5, 5, 5);
        content.add(pnlBotones, gbc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(30, 35, 48));
        add(scroll);
        
        setSize(450, 600);
        setLocationRelativeTo(getParent());
    }

    private JTextField crearCampo(String label, JPanel pnl, GridBagConstraints gbc) {
        pnl.add(crearLabel(label), gbc);
        gbc.gridy++;
        JTextField txt = new JTextField(20);
        txt.setBackground(new Color(45, 52, 71));
        txt.setForeground(Color.WHITE);
        txt.setCaretColor(Color.WHITE);
        pnl.add(txt, gbc);
        gbc.gridy++;
        return txt;
    }

    private void guardarCambios() {
        try {
            String fecha = txtFecha.getText().trim();
            java.time.LocalDate.parse(fecha);
            
            String ref = txtRef.getText().trim();
            String cliente = txtCliente.getText().trim();
            String desc = txtDesc.getText().trim();
            
            BigDecimal importe = DineroUtil.parsearMontoArs(txtImporte.getText());
            
            String costoStr = txtCosto.getText().trim();
            BigDecimal costo = null;
            if (!costoStr.isEmpty()) {
                costo = DineroUtil.parsearMontoArs(costoStr);
            }
            
            String obs = txtObs.getText().trim();
            
            op.setFecha(fecha);
            op.setReferenciaPapel(ref.isEmpty() ? null : ref);
            op.setCliente(cliente.isEmpty() ? null : cliente);
            op.setDescripcion(desc);
            op.setImporteTotal(importe);
            op.setCostoMateriales(costo);
            op.setObservaciones(obs.isEmpty() ? null : obs);
            
            repo.guardar(op);
            JOptionPane.showMessageDialog(this, "Cambios guardados correctamente.");
            parent.actualizarTabla();
            dispose();
            
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "La fecha debe tener formato YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "guardar cambios en operación", ex);
        }
    }

    private void eliminarOperacion() {
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que desea eliminar el registro?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            try {
                repo.eliminar(op.getId());
                parent.actualizarTabla();
                dispose();
            } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
                ErrorHandler.mostrarErrorPersistencia(this, "eliminar operación", ex);
            }
        }
    }

    private void vincularBoleta() {
        String input = JOptionPane.showInputDialog(this, "Ingrese el NÚMERO de la Boleta Digital para vincular:");
        if (input != null && !input.isEmpty()) {
            try {
                int boletaNumero = Integer.parseInt(input);
                var boletaOpt = boletaRepo.buscarBoletaPorNumero(boletaNumero);
                
                if (boletaOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontró ninguna boleta con el número " + boletaNumero);
                    return;
                }
                
                int boletaId = boletaOpt.get().id();
                
                op.setBoletaDigitalId(boletaId);
                op.setEstado("DIGITALIZADO");
                repo.guardar(op);
                JOptionPane.showMessageDialog(this, "Vinculado correctamente a la Boleta #" + boletaNumero);
                parent.actualizarTabla();
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Número inválido.");
            } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
                ErrorHandler.mostrarErrorPersistencia(this, "vincular boleta", ex);
            }
        }
    }
    
    private void marcarDigitalizado() {
        int r = JOptionPane.showConfirmDialog(this, 
            "¿Desea marcar este registro como DIGITALIZADO?\n\n" +
            "Haga esto únicamente si la boleta ya existe en el sistema digital,\n" +
            "pero por algún motivo no desea o no puede vincularla directamente.", 
            "Confirmar", JOptionPane.YES_NO_OPTION);
            
        if (r == JOptionPane.YES_OPTION) {
            try {
                op.setEstado("DIGITALIZADO");
                repo.guardar(op);
                JOptionPane.showMessageDialog(this, "Marcado como DIGITALIZADO.");
                parent.actualizarTabla();
                dispose();
            } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
                ErrorHandler.mostrarErrorPersistencia(this, "marcar como digitalizado", ex);
            }
        }
    }

    private JLabel crearLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }
}
