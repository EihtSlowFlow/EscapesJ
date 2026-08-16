package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Muestra un mensaje de error genérico al usuario cuando falla la persistencia,
     * ocultando los detalles técnicos y registrándolos en el log.
     *
     * @param parent    Componente padre para el diálogo de error.
     * @param operacion Descripción de la operación fallida (e.g., "guardar producto").
     * @param error     Excepción original lanzada por la capa de persistencia.
     */
    public static void mostrarErrorPersistencia(Component parent, String operacion, PersistenceException error) {
        logger.error("Error de persistencia durante: {}", operacion, error);

        JOptionPane.showMessageDialog(
                parent,
                "No se pudo completar la operación debido a un error de almacenamiento.\n"
                        + "Los cambios no fueron guardados.",
                "Error de almacenamiento",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
