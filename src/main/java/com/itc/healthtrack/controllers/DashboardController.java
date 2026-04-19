package com.itc.healthtrack.controllers;

import com.itc.healthtrack.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

/**
 * Controlador principal que gestiona el menu lateral y el area de contenido dinamico.
 * Adapts the available modules based on the logged-in user's role.
 */
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;
    @FXML private VBox contentArea;
    @FXML private Button btnPatientsList;

    private User loggedInUser;

    /**
     * Initializes the panel with the information of the logged-in user.
     * Adjusts the UI based on the user role.
     */
    public void initData(User user) {
        this.loggedInUser = user;

        // Display the correct name prefix per role
        String role = user.getRole() != null ? user.getRole() : "patient";
        switch (role) {
            case "doctor":
                userNameLabel.setText("Dr. " + user.getLastName());
                roleLabel.setText("Médico");
                break;
            case "admin":
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Administrador");
                break;
            default: // patient
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Paciente");
                break;
        }

        // Hide doctor-only modules for patients
        if ("patient".equals(role)) {
            btnPatientsList.setVisible(false);
            btnPatientsList.setManaged(false);
            // Default module for patients is their own metrics
            onShowMetrics();
        } else {
            // Default module for doctors/admins is the patient list
            onShowPatientsList();
        }
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

    @FXML
    protected void onShowRecommendations() {
        changeModule("/com/itc/healthtrack/views/recommendations-view.fxml", "recommendations");
    }

    /**
     * Generic method to inject views into the central area of the BorderPane.
     */
    private void changeModule(String fxmlPath, String moduleType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

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
                case "recommendations":
                    RecommendationsController rcc = loader.getController();
                    rcc.initData(loggedInUser);
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
     * Ends the session and restores the login scene with its styles.
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
}