package io.github.ramiro.escapesj.vista;

import javax.swing.*;
import java.awt.*;

public class ZoomControls extends JPanel implements ZoomManager.ZoomListener {
    private final JLabel lblPorcentaje;
    private boolean listenerRegistered;

    public ZoomControls() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JButton btnMenos = new JButton("A-");
        btnMenos.setToolTipText("Reducir tamaño (Ctrl + -)");
        btnMenos.setFocusPainted(false);
        btnMenos.addActionListener(e -> ZoomManager.reducir());

        lblPorcentaje = new JLabel(ZoomManager.getScalePercent() + "%");
        // No se fuerza una fuente absoluta aquí, dejando que UIManager escale la fuente del Label
        lblPorcentaje.setPreferredSize(new Dimension(60, 30));
        lblPorcentaje.setHorizontalAlignment(SwingConstants.CENTER);
        lblPorcentaje.setForeground(Color.WHITE);

        JButton btnReset = new JButton("100%");
        btnReset.setToolTipText("Restablecer tamaño (Ctrl + 0)");
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(e -> ZoomManager.restablecer());

        JButton btnMas = new JButton("A+");
        btnMas.setToolTipText("Aumentar tamaño (Ctrl + +)");
        btnMas.setFocusPainted(false);
        btnMas.addActionListener(e -> ZoomManager.aumentar());

        add(btnMenos);
        add(lblPorcentaje);
        add(btnReset);
        add(btnMas);

    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listenerRegistered) {
            ZoomManager.addListener(this);
            listenerRegistered = true;
        }
        lblPorcentaje.setText(ZoomManager.getScalePercent() + "%");
    }

    @Override
    public void removeNotify() {
        if (listenerRegistered) {
            ZoomManager.removeListener(this);
            listenerRegistered = false;
        }
        super.removeNotify();
    }

    @Override
    public void onZoomChanged(int newPercent) {
        lblPorcentaje.setText(newPercent + "%");
    }
}
