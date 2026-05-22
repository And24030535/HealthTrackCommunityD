package com.itc.healthtrack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

//Clase principal que arranca la aplicacion y carga la pantalla de Login
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
        // Inicia la app en pantalla completa para la presentación
        stage.setFullScreen(true);
        // Evita que el usuario salga accidentalmente de pantalla completa con ESC
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    }
}