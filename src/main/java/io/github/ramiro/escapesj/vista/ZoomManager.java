package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.ConfigRepository;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ZoomManager {
    private static ConfigRepository configRepo;
    private static int scalePercent = 100;
    private static final int MIN_ZOOM = 80;
    private static final int MAX_ZOOM = 200;
    private static final int STEP = 10;
    private static boolean shortcutsRegistered;

    // Caché inmutable de los valores base para evitar zoom acumulativo
    private static final Map<Object, Object> baseDefaults = new HashMap<>();

    public interface ZoomListener {
        void onZoomChanged(int newPercent);
    }
    private static final CopyOnWriteArrayList<ZoomListener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(ZoomListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeListener(ZoomListener listener) {
        listeners.remove(listener);
    }

    public static void inicializar(ConfigRepository repo) {
        configRepo = repo;

        // 1. Respaldar valores base
        if (baseDefaults.isEmpty()) {
            UIDefaults defaults = UIManager.getDefaults();
            for (Object key : defaults.keySet()) {
                Object value = defaults.get(key);
                if (value instanceof Font) {
                    baseDefaults.put(key, value);
                }
            }
            // Respaldo de dimensiones conocidas que deben escalar
            int rowHeight = defaults.getInt("Table.rowHeight");
            baseDefaults.put("Table.rowHeight", rowHeight > 0 ? rowHeight : 16);
        }

        // 2. Leer persistencia
        String saved = configRepo.obtener("ui.scale_percent").orElse("100");
        try {
            scalePercent = Integer.parseInt(saved);
        } catch (NumberFormatException e) {
            scalePercent = 100;
        }

        if (scalePercent < MIN_ZOOM || scalePercent > MAX_ZOOM) {
            scalePercent = 100;
        }

        // 3. Registrar atajos globales
        if (!shortcutsRegistered) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(e -> {
                if (e.getID() == KeyEvent.KEY_PRESSED && e.isControlDown()) {
                    if (e.getKeyCode() == KeyEvent.VK_ADD || e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                        aumentar();
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_SUBTRACT || e.getKeyCode() == KeyEvent.VK_MINUS) {
                        reducir();
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0) {
                        restablecer();
                        return true;
                    }
                }
                return false;
            });
            shortcutsRegistered = true;
        }

        // 4. Aplicar siempre: también restaura los defaults si se reinicializa al 100%.
        aplicarEscalaGlobal(false);
    }

    public static int getScalePercent() {
        return scalePercent;
    }

    public static void aumentar() {
        if (scalePercent < MAX_ZOOM) {
            scalePercent = Math.min(MAX_ZOOM, scalePercent + STEP);
            guardarYAplicar();
        }
    }

    public static void reducir() {
        if (scalePercent > MIN_ZOOM) {
            scalePercent = Math.max(MIN_ZOOM, scalePercent - STEP);
            guardarYAplicar();
        }
    }

    public static void restablecer() {
        if (scalePercent != 100) {
            scalePercent = 100;
            guardarYAplicar();
        }
    }

    private static void guardarYAplicar() {
        if (configRepo != null) {
            configRepo.guardar("ui.scale_percent", String.valueOf(scalePercent));
        }
        aplicarEscalaGlobal(true);
        for (ZoomListener l : listeners) {
            l.onZoomChanged(scalePercent);
        }
    }

    private static void aplicarEscalaGlobal(boolean updateWindows) {
        float factor = scalePercent / 100.0f;
        UIDefaults defaults = UIManager.getDefaults();

        for (Map.Entry<Object, Object> entry : baseDefaults.entrySet()) {
            Object key = entry.getKey();
            Object baseVal = entry.getValue();

            if (baseVal instanceof Font) {
                Font baseFont = (Font) baseVal;
                float newSize = baseFont.getSize() * factor;
                defaults.put(key, new FontUIResource(baseFont.deriveFont(newSize)));
            } else if ("Table.rowHeight".equals(key)) {
                int baseHeight = (Integer) baseVal;
                defaults.put(key, (int) (baseHeight * factor));
            }
        }

        if (updateWindows) {
            // El usuario indicó que se debe ejecutar en el EDT
            SwingUtilities.invokeLater(() -> {
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                    window.revalidate();
                    window.repaint();
                    // No usamos pack() aquí en ventanas principales para no perder el maximizado
                }
            });
        }
    }

    // --- Utilidades para componentes de tamaño fijo o fuentes instanciadas manualmente ---

    public static Font scaledFont(Font base) {
        float factor = scalePercent / 100.0f;
        return base.deriveFont(base.getSize() * factor);
    }

    public static int scale(int base) {
        float factor = scalePercent / 100.0f;
        return (int) (base * factor);
    }

    public static Dimension scaleDimension(int width, int height) {
        return new Dimension(scale(width), scale(height));
    }
}
