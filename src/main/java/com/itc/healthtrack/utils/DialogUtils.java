package com.itc.healthtrack.utils;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/**
 * Utilidad para aplicar estilo visual uniforme a los diálogos de la aplicación.
 *
 * Antes, cada controlador (AdminController, MetricsController, RegisterController)
 * tenía su propia copia del mismo método applyWhiteDialogStyle().
 * Esta clase centraliza ese estilo para evitar duplicación.
 */
public class DialogUtils {

    // Constructor privado: clase de utilidad, no se instancia
    private DialogUtils() {}

    /**
     * Aplica el estilo blanco estándar de HealthTrack al panel de un diálogo.
     * Cambia el fondo, el texto del contenido, el encabezado y los botones.
     *
     * @param dp el DialogPane del Alert o Dialog a estilizar
     */
    public static void applyWhiteStyle(DialogPane dp) {
        // Fondo blanco para el panel completo
        dp.setStyle("-fx-background-color: #ffffff; -fx-font-size: 13px;");

        // Texto del contenido en color oscuro para contraste
        javafx.scene.Node content = dp.lookup(".content.label");
        if (content != null) {
            content.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px;");
        }

        // Encabezado con fondo gris claro
        javafx.scene.Node header = dp.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: #f5f5f5;");
        }

        // Texto del encabezado en negro y negrita
        javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
        }

        // Botones: azul para confirmación, gris para cancelar/cerrar
        for (ButtonType bt : dp.getButtonTypes()) {
            javafx.scene.Node node = dp.lookupButton(bt);
            if (node instanceof Button) {
                Button btn = (Button) node;
                boolean isCancel = (bt == ButtonType.CANCEL
                        || bt == ButtonType.NO
                        || bt == ButtonType.CLOSE);
                String color = isCancel ? "#9e9e9e" : "#2196f3";
                btn.setStyle("-fx-background-color: " + color
                        + "; -fx-text-fill: #ffffff; -fx-cursor: hand;"
                        + " -fx-padding: 6 22; -fx-background-radius: 4;");
            }
        }
    }
}
