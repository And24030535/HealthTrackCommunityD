package com.itc.healthtrack.controllers;

import com.itc.healthtrack.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

/**
 * Controlador principal que gestiona el menu lateral y el area de contenido dinamico.
 */
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private VBox contentArea;

    private User loggedInUser;

    /**
     * Inicializa el panel con la informacion del usuario que inicio sesion.
     */
    public void initData(User user) {
        this.loggedInUser = user;
        // Se asume el prefijo Dr. por el rol de medico asignado
        userNameLabel.setText("Dr. " + user.getLastName());

        // Carga el primer modulo por defecto al entrar
        onShowPatientsList();
    }

    @FXML
    protected void onShowPatientsList() {
        changeModule("/com/itc/healthtrack/views/patients-view.fxml", "patients");
    }

    @FXML
    protected void onShowMetrics() {
        changeModule("/com/itc/healthtrack/views/metrics-view.fxml", "metrics");
    }

    @FXML
    protected void onShowReports() {
        changeModule("/com/itc/healthtrack/views/reports-view.fxml", "reports");
    }

    /**
     * Metodo generico para inyectar vistas en el area central del BorderPane.
     */
    private void changeModule(String fxmlPath, String moduleType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            // Configuracion especifica segun el controlador cargado
            switch (moduleType) {
                case "patients":
                    PatientsController pc = loader.getController();
                    pc.initData(loggedInUser);
                    break;
                case "metrics":
                    MetricsController mc = loader.getController();
                    mc.initData(loggedInUser);
                    break;
                case "reports":
                    ReportsController rc = loader.getController();
                    rc.initData(loggedInUser);
                    break;
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar el módulo: " + fxmlPath);
        }
    }

    /**
     * Finaliza la sesion y restaura la escena de login con sus estilos.
     */
    @FXML
    protected void onLogout(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene loginScene = new Scene(fxmlLoader.load(), 800, 600);

            loginScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            loginScene.getStylesheets().add(cssPath);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onShowRecommendations() {
        changeModule("/com/itc/healthtrack/views/recommendations-view.fxml", "recommendations"); // Asume que agregas el caso "recommendations" en el switch de changeModule
    }
}