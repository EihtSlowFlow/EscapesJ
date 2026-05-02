package io.github.ramiro.escapesj.main;

import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.vista.VentanaPrincipal;
import io.github.ramiro.escapesj.modelo.Inventario;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Principal {
    public static void main(String[] args) {
        // Establecemos el LookAndFeel del sistema para que se integre mejor con Kubuntu
        configurarAparienciaSistema();

        // Inicializamos el servicio de AFIP (que cargará el config.properties internamente)
        AfipService afipService = new AfipService();

        // Lanzamos la interfaz de forma segura en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(afipService, new Inventario());
            ventana.setVisible(true);
        });
    }

    private static void configurarAparienciaSistema() {
        try {
            // Intentamos usar el LookAndFeel de GTK (nativo en Linux) o el del sistema
            String laf = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            System.err.println("No se pudo establecer el LookAndFeel nativo.");
        }
    }
}