package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.PersistenceException;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ZoomManager {
    private static ConfigRepository configRepo;
    private static int scalePercent = 100;
    private static final int MIN_ZOOM = 80;
    private static final int MAX_ZOOM = 200;
    private static final int STEP = 10;
    private static boolean shortcutsRegistered;
    private static boolean windowListenerRegistered;
    private static Consumer<PersistenceException> persistenceErrorHandler = ZoomManager::showPersistenceError;
    private static final String BASE_FONT = "zoom.baseFont";
    private static final String BASE_ROW_HEIGHT = "zoom.baseRowHeight";
    private static final String BASE_PREFERRED_SIZE = "zoom.basePreferredSize";
    private static final String BASE_MINIMUM_SIZE = "zoom.baseMinimumSize";
    private static final String BASE_MAXIMUM_SIZE = "zoom.baseMaximumSize";
    private static final String BASE_MARGIN = "zoom.baseMargin";
    private static final String SCALE_SIZE = "zoom.scaleSize";

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

        if (!windowListenerRegistered) {
            AWTEventListener listener = event -> {
                if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Window window) {
                    applyScaleToTree(window);
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);
            windowListenerRegistered = true;
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
        aplicarEscalaGlobal(true);
        for (ZoomListener l : listeners) {
            l.onZoomChanged(scalePercent);
        }
        if (configRepo != null) {
            try {
                configRepo.guardar("ui.scale_percent", String.valueOf(scalePercent));
            } catch (PersistenceException e) {
                SwingUtilities.invokeLater(() -> persistenceErrorHandler.accept(e));
            }
        }
    }

    private static void showPersistenceError(PersistenceException error) {
        ErrorHandler.mostrarErrorPersistencia(
                null, "guardar la preferencia de zoom; el cambio se mantendrá durante esta sesión", error);
    }

    static void setPersistenceErrorHandler(Consumer<PersistenceException> handler) {
        persistenceErrorHandler = handler == null ? ZoomManager::showPersistenceError : handler;
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
                    applyScaleToTree(window);
                    window.revalidate();
                    window.repaint();
                    // No usamos pack() aquí en ventanas principales para no perder el maximizado
                }
            });
        }
    }

    static void applyScaleToTree(Component component) {
        if (component instanceof JComponent swingComponent) {
            scaleExplicitFont(swingComponent);
            scaleExplicitSizes(swingComponent);
            scaleMargin(swingComponent);
            scaleTable(swingComponent);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyScaleToTree(child);
            }
        }
    }

    private static void scaleExplicitFont(JComponent component) {
        Font base = (Font) component.getClientProperty(BASE_FONT);
        Font current = component.getFont();
        if (base == null && current != null && !(current instanceof UIResource)) {
            base = current;
            component.putClientProperty(BASE_FONT, base);
        }
        if (base != null) {
            component.setFont(scaledFont(base));
        }
    }

    private static void scaleExplicitSizes(JComponent component) {
        if (!Boolean.TRUE.equals(component.getClientProperty(SCALE_SIZE))) {
            return;
        }
        scaleDimensionProperty(component, BASE_PREFERRED_SIZE, component.isPreferredSizeSet(), component.getPreferredSize(), component::setPreferredSize);
        scaleDimensionProperty(component, BASE_MINIMUM_SIZE, component.isMinimumSizeSet(), component.getMinimumSize(), component::setMinimumSize);
        scaleDimensionProperty(component, BASE_MAXIMUM_SIZE, component.isMaximumSizeSet(), component.getMaximumSize(), component::setMaximumSize);
    }

    private static void scaleDimensionProperty(JComponent component, String key, boolean explicitlySet,
                                               Dimension current, java.util.function.Consumer<Dimension> setter) {
        Dimension base = (Dimension) component.getClientProperty(key);
        if (base == null && explicitlySet && current != null) {
            base = new Dimension(current);
            component.putClientProperty(key, base);
        }
        if (base != null) {
            setter.accept(scaleDimension(base.width, base.height));
        }
    }

    private static void scaleMargin(JComponent component) {
        Insets current = null;
        if (component instanceof AbstractButton button) {
            current = button.getMargin();
        } else if (component instanceof JTextComponent textComponent) {
            current = textComponent.getMargin();
        }
        if (current == null) {
            return;
        }
        Insets base = (Insets) component.getClientProperty(BASE_MARGIN);
        if (base == null) {
            base = new Insets(current.top, current.left, current.bottom, current.right);
            component.putClientProperty(BASE_MARGIN, base);
        }
        Insets scaled = new Insets(scale(base.top), scale(base.left), scale(base.bottom), scale(base.right));
        if (component instanceof AbstractButton button) {
            button.setMargin(scaled);
        } else {
            ((JTextComponent) component).setMargin(scaled);
        }
    }

    private static void scaleTable(JComponent component) {
        if (!(component instanceof JTable table)) {
            return;
        }
        Integer base = (Integer) table.getClientProperty(BASE_ROW_HEIGHT);
        if (base != null) {
            table.setRowHeight(scale(base));
        }
        if (table.getTableHeader() != null) {
            scaleExplicitFont(table.getTableHeader());
            scaleExplicitSizes(table.getTableHeader());
        }
    }

    public static void scaleExplicitSize(JComponent component) {
        component.putClientProperty(SCALE_SIZE, Boolean.TRUE);
    }

    public static void registerBaseRowHeight(JTable table, int baseRowHeight) {
        table.putClientProperty(BASE_ROW_HEIGHT, baseRowHeight);
        table.setRowHeight(scale(baseRowHeight));
    }

    public static void packAndFitToScreen(Window window, int baseWidth, int baseHeight) {
        window.pack();
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int availableWidth = bounds.width - screenInsets.left - screenInsets.right;
        int availableHeight = bounds.height - screenInsets.top - screenInsets.bottom;
        int desiredWidth = Math.max(window.getWidth(), scale(baseWidth));
        int desiredHeight = Math.max(window.getHeight(), scale(baseHeight));
        window.setSize(Math.min(desiredWidth, availableWidth), Math.min(desiredHeight, availableHeight));
        window.setLocationRelativeTo(window.getOwner());
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
