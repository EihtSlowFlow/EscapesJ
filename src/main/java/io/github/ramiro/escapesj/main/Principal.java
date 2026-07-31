package io.github.ramiro.escapesj.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ramiro.escapesj.modelo.Inventario;
import io.github.ramiro.escapesj.persistencia.*;
import io.github.ramiro.escapesj.sdk.AfipService;
import io.github.ramiro.escapesj.vista.VentanaLogin;
import io.github.ramiro.escapesj.vista.VentanaSetupInicial;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class Principal {
    private static final Logger logger = LoggerFactory.getLogger(Principal.class);


    public static void main(String[] args) {

        configurarAparienciaGlobal();

        try {
            DatabaseService.inicializar();

            ProductoRepository productoRepo = new ProductoRepository();
            ServicioRepository servicioRepo = new ServicioRepository();
            UsuarioRepository usuarioRepo = new UsuarioRepository();
            ConfigRepository configRepo = new ConfigRepository();
            ClienteCacheRepository cacheRepo = new ClienteCacheRepository();
            BoletaRepository boletaRepo = new BoletaRepository();
            PresupuestoRepository presupuestoRepo = new PresupuestoRepository();
            Inventario inventario = new Inventario(productoRepo);

            AfipService afipService = new AfipService(configRepo, cacheRepo);

            if (usuarioRepo.isUsuariosEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    VentanaSetupInicial setup = new VentanaSetupInicial(usuarioRepo);
                    setup.setVisible(true);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    VentanaLogin login = new VentanaLogin(afipService, inventario, productoRepo, servicioRepo, usuarioRepo, configRepo, boletaRepo, presupuestoRepo);
                    login.setVisible(true);
                });
            }

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
            logger.error("Error:", e);
        }
    }
}