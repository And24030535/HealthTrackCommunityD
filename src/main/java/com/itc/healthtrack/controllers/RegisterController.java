package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.UserDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

/**
 * Handles new user self-registration (patients and doctors).
 */
public class RegisterController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBirthDate;
    @FXML private TextField txtHeight;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> comboGender;
    @FXML private ComboBox<String> comboRole;
    @FXML private Label lblStatus;
    @FXML private Button btnRegister;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        comboGender.getItems().addAll("M", "F", "Other");
        comboRole.getItems().addAll("patient", "doctor");
        comboRole.getSelectionModel().selectFirst();
    }

    @FXML
    protected void onRegister(ActionEvent event) {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = comboRole.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Por favor, completa todos los campos obligatorios.", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showStatus("Las contraseñas no coinciden.", false);
            return;
        }

        if (password.length() < 6) {
            showStatus("La contraseña debe tener al menos 6 caracteres.", false);
            return;
        }

        btnRegister.setDisable(true);

        new Thread(() -> {
            try {
                User newUser = new User();
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setEmail(email);
                newUser.setPassword(password);
                newUser.setRole(role);
                newUser.setGender(comboGender.getValue());

                String birthDate = txtBirthDate.getText().trim();
                if (!birthDate.isEmpty()) {
                    newUser.setBirthDate(birthDate);
                }

                String heightText = txtHeight.getText().trim();
                if (!heightText.isEmpty()) {
                    try {
                        newUser.setHeight(Double.parseDouble(heightText));
                    } catch (NumberFormatException e) {
                        Platform.runLater(() -> showStatus("Altura inválida. Verifica que sea un número decimal (ej: 1.75).", false));
                        btnRegister.setDisable(false);
                        return;
                    }
                }

                userDAO.saveUser(newUser);

                Platform.runLater(() -> {
                    showStatus("¡Cuenta creada exitosamente! Redirigiendo al login...", true);
                    btnRegister.setDisable(false);
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        Platform.runLater(() -> goToLogin(event));
                    }).start();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus("Error al crear la cuenta. Intenta de nuevo.", false);
                    btnRegister.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onGoToLogin(ActionEvent event) {
        goToLogin(event);
    }

    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        lblStatus.setText(message);
        lblStatus.setTextFill(isSuccess ? Color.web("#4caf50") : Color.web("#ff5252"));
        lblStatus.setVisible(true);
    }
}
