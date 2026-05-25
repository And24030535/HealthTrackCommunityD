package com.itc.healthtrack.controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.itc.healthtrack.config.AppConfig;
import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
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

// controlador para registrar nuevos usuarios
// segun el rol elegido pide un token de acceso para medicos y admins
public class RegisterController {

    // los tokens viven en AppConfig para evitar duplicacion

    @FXML private TextField        txtFirstName;
    @FXML private TextField        txtLastName;
    @FXML private TextField        txtEmail;
    @FXML private TextField        txtHeight;
    @FXML private DatePicker       dpBirthDate;
    @FXML private PasswordField    txtPassword;
    @FXML private PasswordField    txtConfirmPassword;
    @FXML private ComboBox<String> comboGender;
    @FXML private ComboBox<String> comboRole;

    @FXML private VBox      tokenSection;
    @FXML private Label     lblTokenLabel;
    @FXML private TextField txtToken;

    @FXML private Label  lblStatus;
    @FXML private Button btnRegister;

    // unico dao para guardar el nuevo perfil en firestore
    private final GenericDAO<User> userDAO = new GenericDAO<>(User.class, "users");

    @FXML
    public void initialize() {
        comboGender.getItems().addAll("M", "F", "Otro");
        comboRole.getItems().addAll("Paciente", "Doctor", "Admin");
        comboRole.setValue("Paciente");

        // mostramos u ocultamos el campo de token segun el rol seleccionado
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

    @FXML
    protected void onRegister(ActionEvent event) {

        // leemos todos los valores de la ui en el hilo fx antes de lanzar el hilo de fondo
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

        // validaciones basicas
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

        // checamos el token para los roles elevados
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

        // mapeamos la etiqueta de rol al valor interno
        final String mappedRole;
        switch (roleLabel) {
            case "Doctor": mappedRole = "doctor";  break;
            case "Admin":  mappedRole = "admin";   break;
            default:       mappedRole = "patient"; break;
        }

        // validamos el formato de la altura
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

                // creamos la cuenta en firebase auth admin sdk
                UserRecord.CreateRequest authRequest = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password);
                UserRecord createdRecord = FirebaseAuth.getInstance().createUser(authRequest);
                String uid = createdRecord.getUid();
                System.out.println("[RegisterController] Auth OK — UID: " + uid);

                // construimos el perfil para firestore sin password
                User profile = new User();
                profile.setUid(uid);
                profile.setEmail(email);
                profile.setFirstName(firstName);
                profile.setLastName(lastName);
                profile.setRole(mappedRole);
                profile.setGender(gender);
                if (birthDateStr != null) profile.setBirthDate(birthDateStr);
                if (finalHeight  != null) profile.setHeight(finalHeight);

                // auto asignamos un medico si el nuevo usuario es paciente
                if ("patient".equals(mappedRole)) {
                    List<User> doctors = userDAO.getByField("role", "doctor");
                    if (!doctors.isEmpty()) {
                        User assigned = doctors.get((int) (Math.random() * doctors.size()));
                        profile.setAssignedDoctorId(assigned.getUid());
                        System.out.println("[RegisterController] Doctor asignado: "
                                + assigned.getFirstName() + " " + assigned.getLastName());
                    }
                }

                // guardamos el perfil en firestore usando el uid como id del documento
                userDAO.save(uid, profile);
                System.out.println("[RegisterController] Perfil guardado en Firestore — UID: " + uid
                        + ", rol: " + mappedRole);

                // mostramos confirmacion y redirigimos al login
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

    // alerta de token invalido
    private void showTokenAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Código de Acceso Requerido");
        alert.setHeaderText("Verificación de seguridad fallida");
        alert.setContentText(message);
        applyWhiteStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    // obtiene el codigo de error de firebase con compatibilidad entre versiones del sdk
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
            // mantener pantalla completa al volver al login
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error al volver al login.", false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        lblStatus.setText(message);
        lblStatus.setTextFill(isSuccess ? Color.web("#4caf50") : Color.web("#ff5252"));
        lblStatus.setVisible(true);
    }

    // aplica el estilo blanco al panel del dialogo
    private static void applyWhiteStyle(DialogPane dp) {
        // fondo blanco
        dp.setStyle("-fx-background-color: #ffffff; -fx-font-size: 13px;");

        // texto del contenido en oscuro
        javafx.scene.Node content = dp.lookup(".content.label");
        if (content != null) {
            content.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px;");
        }

        // encabezado en gris claro
        javafx.scene.Node header = dp.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: #f5f5f5;");
        }

        // texto del encabezado en negro
        javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
        }

        // botones azul para confirmar y gris para cancelar
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
