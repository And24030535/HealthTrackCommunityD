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

// controlador del menu lateral y el area central segun el rol del usuario
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;
    @FXML private VBox contentArea;
    @FXML private Button btnPatientsList;
    @FXML private Button btnAdminPanel;

    private User loggedInUser;

    // inicializa el panel con el usuario y ajusta la interfaz segun el rol
    public void initData(User user) {
        this.loggedInUser = user;

        // muestra el prefijo correcto segun el rol
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
            default:
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Paciente");
                break;
        }

        if ("patient".equals(role)) {
            btnPatientsList.setVisible(false);
            btnPatientsList.setManaged(false);
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            onShowMetrics();
        } else if ("admin".equals(role)) {
            onShowAdmin();
        } else {
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            onShowPatientsList();
        }
    }

    @FXML
    protected void onShowPatientsList() {
        changeModule("/com/itc/healthtrack/views/patients-view.fxml", "patients");
    }

    @FXML
    protected void onShowAdmin() {
        changeModule("/com/itc/healthtrack/views/admin-view.fxml", "admin");
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

    // carga la vista fxml e instancia el controlador pasandole los datos del usuario
    private void changeModule(String fxmlPath, String moduleType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            switch (moduleType) {
                case "admin":
                    AdminController ac = loader.getController();
                    ac.initData(loggedInUser);
                    break;
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

            // reemplaza el contenido anterior por el nuevo modulo
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar el módulo: " + fxmlPath);
        }
    }

    // cierra sesion y vuelve al login restaurando los estilos
    @FXML
    protected void onLogout(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene loginScene = new Scene(fxmlLoader.load(), 960, 620);

            loginScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            loginScene.getStylesheets().add(cssPath);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            // mantener pantalla completa al volver al login
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
