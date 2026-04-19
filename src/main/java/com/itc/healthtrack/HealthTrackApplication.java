package com.itc.healthtrack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

/**
 * Clase principal que arranca la aplicacion y carga la pantalla de Login.
 */
public class HealthTrackApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Carga la vista de inicio de sesion ubicada en la carpeta resources
        FXMLLoader fxmlLoader = new FXMLLoader(HealthTrackApplication.class.getResource("/com/itc/healthtrack/views/login-view.fxml"));

        // Se definen dimensiones mas adecuadas para una aplicacion de escritorio (800x600)
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Se inyecta la libreria BootstrapFX para mejorar el aspecto de los botones y campos
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        String cssPath = HealthTrackApplication.class.getResource("/css/main.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        stage.setTitle("HealthTrack Community - Login");
        stage.setScene(scene);
        stage.show();
    }
}