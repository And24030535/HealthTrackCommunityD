package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.UserDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

/**
 * Controlador encargado de la autenticacion de usuarios.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    /**
     * Procesa el intento de inicio de sesion.
     */
    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Por favor, ingresa correo y contraseña.");
            return;
        }

        // Bloqueo visual de seguridad mientras se consulta la nube
        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        // Ejecucion en hilo secundario para no congelar la UI
        new Thread(() -> {
            try {
                // Consulta a Firestore con correo y password
                User currentUser = userDAO.authenticateUser(email, password);

                Platform.runLater(() -> {
                    if (currentUser != null) {
                        loadDashboard(event, currentUser);
                    } else {
                        showError("Credenciales inválidas o error de conexión.");
                        loginButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error al conectar con la base de datos.");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Configura y muestra la escena del Dashboard inyectando los estilos CSS.
     */
    private void loadDashboard(ActionEvent event, User user) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/dashboard-view.fxml"));
            Scene dashboardScene = new Scene(fxmlLoader.load(), 1000, 700);

            // Inyeccion de estilos para garantizar visibilidad en modo oscuro
            dashboardScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            dashboardScene.getStylesheets().add(cssPath);

            DashboardController controller = fxmlLoader.getController();
            controller.initData(user);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(dashboardScene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error crítico al cargar la interfaz principal.");
        }
    }
}