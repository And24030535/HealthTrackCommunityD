package com.itc.healthtrack.controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.itc.healthtrack.config.AppConfig;
import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.util.List;

// controlador para registrar nuevos usuarios en el sistema
// segun el rol elegido puede requerir un token de acceso para medicos y admins
public class RegisterController {

    // Los tokens de seguridad están centralizados en AppConfig para evitar duplicación

    // Campos del formulario
    @FXML private TextField        txtFirstName;
    @FXML private TextField        txtLastName;
    @FXML private TextField        txtEmail;
    @FXML private TextField        txtHeight;
    @FXML private DatePicker       dpBirthDate;
    @FXML private PasswordField    txtPassword;
    @FXML private PasswordField    txtConfirmPassword;
    @FXML private ComboBox<String> comboGender;    // M / F / Otro
    @FXML private ComboBox<String> comboRole;      // Paciente / Doctor / Admin

    // Sección de token
    @FXML private VBox      tokenSection;
    @FXML private Label     lblTokenLabel;
    @FXML private TextField txtToken;

    @FXML private Label  lblStatus;
    @FXML private Button btnRegister;

    // unico dao que necesitamos para guardar el nuevo perfil en firestore
    private final GenericDAO<User> userDAO = new GenericDAO<>(User.class, "users");

    // Inicialización
    @FXML
    public void initialize() {
        comboGender.getItems().addAll("M", "F", "Otro");
        comboRole.getItems().addAll("Paciente", "Doctor", "Admin");
        comboRole.setValue("Paciente");

        // Mostrar / ocultar el campo de token según el rol seleccionado
        comboRole.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsToken = "Doctor".equals(newVal) || "Admin".equals(newVal);
            tokenSection.setVisible(needsToken);
            tokenSection.setManaged(needsToken);

            if (!needsToken) {
                txtToken.clear();
            } else {
                lblTokenLabel.setText("Doctor".equals(newVal)
                        ? "Código de acceso médico:"
                        : "Código de acceso administrador:");
            }
        });
    }

    // Registro
    @FXML
    protected void onRegister(ActionEvent event) {

        // Leer TODOS los valores de la UI en el hilo FX antes del hilo
        final String firstName    = txtFirstName.getText().trim();
        final String lastName     = txtLastName.getText().trim();
        final String email        = txtEmail.getText().trim();
        final String password     = txtPassword.getText();
        final String confirmPass  = txtConfirmPassword.getText();
        final String gender       = comboGender.getValue();
        final String roleLabel    = comboRole.getValue();
        final String tokenInput   = txtToken.getText().trim();
        final String heightText   = txtHeight.getText().trim();
        final String birthDateStr = dpBirthDate.getValue() != null
                ? dpBirthDate.getValue().toString() : null;

        //Validaciones básicas
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Por favor, completa todos los campos obligatorios.", false);
            return;
        }
        if (!password.equals(confirmPass)) {
            showStatus("Las contraseñas no coinciden.", false);
            return;
        }
        if (password.length() < 6) {
            showStatus("La contraseña debe tener al menos 6 caracteres.", false);
            return;
        }
        if (roleLabel == null || roleLabel.isEmpty()) {
            showStatus("Por favor, selecciona un rol.", false);
            return;
        }

        // Validación de token para roles elevados
        if ("Doctor".equals(roleLabel)) {
            if (tokenInput.isEmpty()) {
                showTokenAlert("Se requiere el código de acceso médico para registrarse como Doctor.\n"
                        + "Solicítalo al administrador del sistema.");
                return;
            }
            if (!AppConfig.TOKEN_DOCTOR.equals(tokenInput)) {
                showTokenAlert("Código de acceso médico incorrecto.\n"
                        + "Verifica el código e intenta de nuevo.");
                return;
            }
        } else if ("Admin".equals(roleLabel)) {
            if (tokenInput.isEmpty()) {
                showTokenAlert("Se requiere el código maestro de administrador.\n"
                        + "Solicítalo al administrador principal del sistema.");
                return;
            }
            if (!AppConfig.TOKEN_ADMIN.equals(tokenInput)) {
                showTokenAlert("Código maestro de administrador incorrecto.\n"
                        + "Verifica el código e intenta de nuevo.");
                return;
            }
        }

        // Mapear etiqueta de rol a valor interno
        final String mappedRole;
        switch (roleLabel) {
            case "Doctor": mappedRole = "doctor";  break;
            case "Admin":  mappedRole = "admin";   break;
            default:       mappedRole = "patient"; break;
        }

        // Validar formato de altura
        Double parsedHeight = null;
        if (!heightText.isEmpty()) {
            try {
                parsedHeight = Double.parseDouble(heightText);
            } catch (NumberFormatException e) {
                showStatus("Altura inválida. Usa un número decimal (ej: 1.75).", false);
                return;
            }
        }
        final Double finalHeight = parsedHeight;

        btnRegister.setDisable(true);

        new Thread(() -> {
            try {

                // Crear cuenta en Firebase Auth (Admin SDK)
                UserRecord.CreateRequest authRequest = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password);
                UserRecord createdRecord = FirebaseAuth.getInstance().createUser(authRequest);
                String uid = createdRecord.getUid();
                System.out.println("[RegisterController] Auth OK — UID: " + uid);

                // Construir perfil para Firestore (sin contraseña)
                User profile = new User();
                profile.setUid(uid);
                profile.setEmail(email);
                profile.setFirstName(firstName);
                profile.setLastName(lastName);
                profile.setRole(mappedRole);  // rol validado por token
                profile.setGender(gender);
                if (birthDateStr != null) profile.setBirthDate(birthDateStr);
                if (finalHeight  != null) profile.setHeight(finalHeight);

                // Auto-asignar médico si el nuevo usuario es paciente
                if ("patient".equals(mappedRole)) {
                    List<User> doctors = userDAO.getByField("role", "doctor");
                    if (!doctors.isEmpty()) {
                        User assigned = doctors.get((int) (Math.random() * doctors.size()));
                        profile.setAssignedDoctorId(assigned.getUid());
                        System.out.println("[RegisterController] Doctor asignado: "
                                + assigned.getFirstName() + " " + assigned.getLastName());
                    }
                }

                // Guardar perfil en Firestore usando el UID como ID del documento
                userDAO.save(uid, profile);
                System.out.println("[RegisterController] Perfil guardado en Firestore — UID: " + uid
                        + ", rol: " + mappedRole);

                // Éxito — mostrar confirmación y redirigir al login
                Platform.runLater(() -> {
                    String displayRole;
                    if ("doctor".equals(mappedRole)) {
                        displayRole = "Doctor";
                    } else if ("admin".equals(mappedRole)) {
                        displayRole = "Administrador";
                    } else {
                        displayRole = "Paciente";
                    }
                    showStatus("¡Cuenta creada como " + displayRole + "! Redirigiendo al login...", true);
                    btnRegister.setDisable(false);
                    new Thread(() -> {
                        try { Thread.sleep(1500); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        Platform.runLater(() -> goToLogin(event));
                    }).start();
                });

            } catch (com.google.firebase.auth.FirebaseAuthException authEx) {
                String errorCode = resolveAuthErrorCode(authEx);
                System.err.println("[RegisterController] FirebaseAuthException — código: " + errorCode);
                authEx.printStackTrace();
                final String msg = parseAuthError(errorCode);
                Platform.runLater(() -> {
                    showStatus(msg, false);
                    btnRegister.setDisable(false);
                });

            } catch (Exception ex) {
                System.err.println("[RegisterController] Error inesperado: "
                        + ex.getClass().getSimpleName() + " — " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showStatus("Error al crear la cuenta: " + ex.getMessage(), false);
                    btnRegister.setDisable(false);
                });
            }
        }).start();
    }

    // Alerta de token inválido


    private void showTokenAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Código de Acceso Requerido");
        alert.setHeaderText("Verificación de seguridad fallida");
        alert.setContentText(message);
        DialogUtils.applyWhiteStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    // Helpers de Firebase Auth
    // obtiene el codigo de error de firebase con compatibilidad entre versiones del SDK
    private String resolveAuthErrorCode(com.google.firebase.auth.FirebaseAuthException ex) {
        try { if (ex.getAuthErrorCode() != null) return ex.getAuthErrorCode().name(); }
        catch (Exception ignored) {}
        try { if (ex.getErrorCode()     != null) return ex.getErrorCode().name();     }
        catch (Exception ignored) {}
        return ex.getMessage() != null ? ex.getMessage() : "UNKNOWN";
    }

    // convierte los codigos de error del registro a mensajes en espanol
    private String parseAuthError(String code) {
        if (code == null) return "Error al registrar la cuenta. Intenta de nuevo.";
        switch (code) {
            case "EMAIL_ALREADY_EXISTS":
            case "DUPLICATE_EMAIL":
                return "Este correo ya está registrado. Inicia sesión o usa otro correo.";
            case "INVALID_EMAIL":
                return "El formato del correo no es válido.";
            case "WEAK_PASSWORD":
                return "Contraseña débil. Usa al menos 6 caracteres variados.";
            default:
                System.err.println("[RegisterController] Código Firebase no reconocido: " + code);
                return "Error al registrar (" + code + "). Verifica los datos e intenta de nuevo.";
        }
    }

    // Navegación

    @FXML
    protected void onGoToLogin(ActionEvent event) {
        goToLogin(event);
    }

    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 960, 620);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            // Mantener pantalla completa al volver al login
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error al volver al login.", false);
        }
    }

    // UI helper

    private void showStatus(String message, boolean isSuccess) {
        lblStatus.setText(message);
        lblStatus.setTextFill(isSuccess ? Color.web("#4caf50") : Color.web("#ff5252"));
        lblStatus.setVisible(true);
    }
}
