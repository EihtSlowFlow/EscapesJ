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
        lblPorcentaje.setHorizontalAlignment(SwingConstants.CENTER);

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
        updatePercentageContrast();
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

    private void updatePercentageContrast() {
        Container ancestor = getParent();
        while (ancestor instanceof JComponent component && !component.isOpaque()) {
            ancestor = ancestor.getParent();
        }
        Color background = ancestor == null ? getBackground() : ancestor.getBackground();
        double luminance = 0.2126 * background.getRed()
                + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue();
        lblPorcentaje.setForeground(luminance < 140 ? Color.WHITE : Color.BLACK);
    }
}
