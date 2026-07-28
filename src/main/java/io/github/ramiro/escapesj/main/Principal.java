package io.github.ramiro.escapesj.main;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.*;
import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.vista.VentanaLogin;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class Principal {

    public static void main(String[] args) {

        configurarAparienciaGlobal();

        try {
            Connection conexion = DatabaseService.getConnection();

            if (conexion == null) {
                JOptionPane.showMessageDialog(null,
                        "Error crítico al inicializar la base de datos local.",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            ProductoRepository productoRepo = new ProductoRepository(conexion);
            ServicioRepository servicioRepo = new ServicioRepository(conexion);
            UsuarioRepository usuarioRepo = new UsuarioRepository(conexion);
            ConfigRepository configRepo = new ConfigRepository(conexion);
            ClienteCacheRepository cacheRepo = new ClienteCacheRepository(conexion);
            BoletaRepository boletaRepo = new BoletaRepository(conexion);
            PresupuestoRepository presupuestoRepo = new PresupuestoRepository(conexion);
            Inventario inventario = new Inventario(productoRepo);

            AfipService afipService = new AfipService(configRepo, cacheRepo);

            // Cerrar la conexión SQLite al cerrar la aplicación
            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseService::cerrarConexion));

            SwingUtilities.invokeLater(() -> {

                VentanaLogin login = new VentanaLogin(afipService, inventario, productoRepo, servicioRepo, usuarioRepo, configRepo, boletaRepo, presupuestoRepo);
                login.setVisible(true);
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error crítico al iniciar la aplicación:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void configurarAparienciaGlobal() {
        try {

            UIManager.put("OptionPane.background", Color.WHITE);
            UIManager.put("OptionPane.messageForeground", Color.BLACK);

            UIManager.put("Panel.background", Color.WHITE);


            UIManager.put("TextField.background", new Color(45, 52, 71));
            UIManager.put("TextField.foreground", Color.WHITE);
            UIManager.put("TextField.caretForeground", Color.WHITE);


            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}