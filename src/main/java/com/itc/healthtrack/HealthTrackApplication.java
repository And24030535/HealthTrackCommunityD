package com.itc.healthtrack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

// clase principal que arranca la app y carga el login
public class HealthTrackApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HealthTrackApplication.class.getResource("/com/itc/healthtrack/views/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 620);

        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        String cssPath = HealthTrackApplication.class.getResource("/css/main.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        stage.setTitle("HealthTrack Community");
        stage.setScene(scene);
        stage.show();
        // arrancamos en pantalla completa para la presentacion
        stage.setFullScreen(true);
        // evita que el usuario salga de pantalla completa con ESC
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    }
}