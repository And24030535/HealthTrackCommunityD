package com.itc.healthtrack.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itc.healthtrack.config.AppConfig;
import com.itc.healthtrack.dao.GenericDAO;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

// controlador del login
// tiene dos formularios uno para pacientes y admins y otro para medicos con token
public class LoginController {

    // credenciales rest api
    private static final String FIREBASE_WEB_API_KEY   = "AIzaSyBOlSDOZdQMwxy6Ev9t2hUbcV3PiB_4paI";
    private static final String FIREBASE_SIGN_IN_URL   =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
            + FIREBASE_WEB_API_KEY;

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private VBox          patientForm;
    @FXML private VBox          doctorPanel;
    @FXML private Button        btnToggleDoctor;
    @FXML private TextField     doctorEmailField;
    @FXML private PasswordField doctorPasswordField;
    @FXML private TextField     doctorTokenField;
    @FXML private Button        doctorLoginButton;

    private final GenericDAO<User> userDAO = new GenericDAO<>(User.class, "users");

    @FXML
    protected void onToggleDoctorPanel() {
        boolean showDoctor = !doctorPanel.isVisible();

        doctorPanel.setVisible(showDoctor);
        doctorPanel.setManaged(showDoctor);
        patientForm.setVisible(!showDoctor);
        patientForm.setManaged(!showDoctor);

        if (!showDoctor) {
            doctorEmailField.clear();
            doctorPasswordField.clear();
            doctorTokenField.clear();
            errorLabel.setVisible(false);
        }
    }

    @FXML
    protected void onGoToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/itc/healthtrack/views/register-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 700);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            // mantener pantalla completa al navegar al registro
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al cargar la pantalla de registro.");
        }
    }

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Por favor, ingresa correo y contraseña.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                // autenticamos con firebase auth rest api
                String localId = signInWithEmailAndPassword(email, password);

                // obtenemos el perfil de firestore
                User currentUser;
                try {
                    currentUser = userDAO.getById(localId);
                } catch (Exception firestoreEx) {
                    System.err.println("[LoginController] Error al leer perfil de Firestore ("
                            + firestoreEx.getClass().getSimpleName() + "): " + firestoreEx.getMessage());
                    firestoreEx.printStackTrace();
                    Platform.runLater(() -> {
                        showError("Autenticación correcta, pero no se pudo cargar el perfil. Intenta de nuevo.");
                        loginButton.setDisable(false);
                    });
                    return;
                }

                Platform.runLater(() -> {
                    loginButton.setDisable(false);

                    if (currentUser == null) {
                        showError("Perfil no encontrado. Contacta al administrador.");
                        return;
                    }

                    String role = currentUser.getRole() != null ? currentUser.getRole() : "patient";

                    if ("doctor".equals(role)) {
                        showError("Las cuentas de médico deben acceder desde el formulario \"Doctor\".");
                        return;
                    }

                    if ("patient".equals(role)) {
                        autoAssignDoctorIfNeeded(currentUser, event);
                    } else {
                        loadDashboard(event, currentUser);
                    }
                });

            } catch (FirebaseLoginException loginError) {
                Platform.runLater(() -> {
                    showError(loginError.getMessage());
                    loginButton.setDisable(false);
                });
            } catch (Exception generalError) {
                System.err.println("[LoginController] Error inesperado ("
                        + generalError.getClass().getSimpleName() + "): " + generalError.getMessage());
                generalError.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error de conexión. Verifica tu red e inténtalo de nuevo.");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    protected void onDoctorLoginButtonClick(ActionEvent event) {
        String email    = doctorEmailField.getText().trim();
        String password = doctorPasswordField.getText();
        String token    = doctorTokenField.getText().trim();

        if (email.isEmpty() || password.isEmpty() || token.isEmpty()) {
            showError("Por favor, ingresa correo, contraseña y código de acceso.");
            return;
        }

        if (!AppConfig.TOKEN_DOCTOR.equals(token)) {
            showError("Código de acceso médico incorrecto.");
            return;
        }

        doctorLoginButton.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                // autenticamos con firebase auth rest api
                String localId = signInWithEmailAndPassword(email, password);

                // obtenemos el perfil de firestore
                User currentUser;
                try {
                    currentUser = userDAO.getById(localId);
                } catch (Exception firestoreEx) {
                    System.err.println("[LoginController] Error al leer perfil de Firestore ("
                            + firestoreEx.getClass().getSimpleName() + "): " + firestoreEx.getMessage());
                    firestoreEx.printStackTrace();
                    Platform.runLater(() -> {
                        showError("Autenticación correcta, pero no se pudo cargar el perfil. Intenta de nuevo.");
                        doctorLoginButton.setDisable(false);
                    });
                    return;
                }

                Platform.runLater(() -> {
                    doctorLoginButton.setDisable(false);

                    if (currentUser == null) {
                        showError("Perfil no encontrado. Contacta al administrador.");
                        return;
                    }

                    if (!"doctor".equals(currentUser.getRole())) {
                        showError("Esta cuenta no tiene rol de médico en el sistema.");
                        return;
                    }

                    loadDashboard(event, currentUser);
                });

            } catch (FirebaseLoginException loginError) {
                Platform.runLater(() -> {
                    showError(loginError.getMessage());
                    doctorLoginButton.setDisable(false);
                });
            } catch (Exception generalError) {
                System.err.println("[LoginController] Error inesperado ("
                        + generalError.getClass().getSimpleName() + "): " + generalError.getMessage());
                generalError.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error de conexión. Verifica tu red e inténtalo de nuevo.");
                    doctorLoginButton.setDisable(false);
                });
            }
        }).start();
    }

    // auto asigna un medico al paciente si no tiene
    private void autoAssignDoctorIfNeeded(User patient, ActionEvent event) {
        boolean needsDoctor = patient.getAssignedDoctorId() == null
                || patient.getAssignedDoctorId().isEmpty();

        if (!needsDoctor) {
            loadDashboard(event, patient);
            return;
        }

        new Thread(() -> {
            try {
                List<User> doctors = userDAO.getByField("role", "doctor");
                if (!doctors.isEmpty()) {
                    User assigned = doctors.get((int) (Math.random() * doctors.size()));
                    patient.setAssignedDoctorId(assigned.getUid());
                    userDAO.save(patient.getUid(), patient);
                }
            } catch (Exception e) {
                System.err.println("[LoginController] Error al asignar doctor: " + e.getMessage());
            }
            Platform.runLater(() -> loadDashboard(event, patient));
        }).start();
    }

    // autenticacion con firebase identity toolkit rest api
    // usamos gson para armar el json y leer las respuestas de error de firebase
    private String signInWithEmailAndPassword(String email, String password) throws Exception {

        // armamos el body con gson para que escape los caracteres especiales
        JsonObject body = new JsonObject();
        body.addProperty("email",             email);
        body.addProperty("password",          password);
        body.addProperty("returnSecureToken", true);

        URL                conn  = new URL(FIREBASE_SIGN_IN_URL);
        HttpURLConnection  http  = (HttpURLConnection) conn.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.setConnectTimeout(10_000);
        http.setReadTimeout(10_000);
        http.setDoOutput(true);

        try (OutputStream os = http.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = http.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // exito sacamos el localId con gson
            String responseJson = readStream(http.getInputStream());
            try {
                String localId = JsonParser.parseString(responseJson)
                        .getAsJsonObject()
                        .get("localId")
                        .getAsString();
                if (localId == null || localId.isEmpty()) {
                    throw new Exception("Firebase no devolvió el UID del usuario.");
                }
                return localId;
            } catch (Exception e) {
                throw new Exception("Respuesta inesperada de Firebase: " + responseJson, e);
            }
        } else {
            // error parseamos el mensaje de firebase con gson
            String errorJson   = readStream(http.getErrorStream());
            System.err.println("[LoginController] Firebase error HTTP " + responseCode + ": " + errorJson);
            String errorCode   = extractFirebaseErrorCode(errorJson);
            throw new FirebaseLoginException(parseLoginError(errorCode));
        }
    }

    // lee el codigo de error que firebase manda dentro del json
    private String extractFirebaseErrorCode(String errorJson) {
        try {
            JsonObject root  = JsonParser.parseString(errorJson).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                // firebase a veces anade detalle tras un espacio como INVALID_EMAIL ...
                return error.get("message").getAsString().split("\\s")[0];
            }
        } catch (Exception e) {
            System.err.println("[LoginController] No se pudo parsear el error JSON: " + errorJson);
        }
        return null;
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) return "";
        StringBuilder sb  = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // convierte los codigos de error de firebase a mensajes entendibles en espanol
    private String parseLoginError(String errorCode) {
        if (errorCode == null) {
            return "Error al iniciar sesión. Verifica tu conexión e inténtalo de nuevo.";
        }
        switch (errorCode) {
            case "INVALID_PASSWORD":
            case "EMAIL_NOT_FOUND":
            case "INVALID_LOGIN_CREDENTIALS":
                return "Correo o contraseña incorrectos.";
            case "USER_DISABLED":
                return "Esta cuenta ha sido deshabilitada. Contacta al administrador.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER":
                return "Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.";
            case "OPERATION_NOT_ALLOWED":
                return "El inicio de sesión con correo y contraseña no está habilitado en este proyecto.";
            case "WEAK_PASSWORD":
                return "La contraseña es demasiado débil.";
            case "INVALID_EMAIL":
                return "El formato del correo electrónico no es válido.";
            default:
                System.err.println("[LoginController] Código Firebase no reconocido: " + errorCode);
                return "Error al iniciar sesión (" + errorCode + "). Contacta al administrador.";
        }
    }

    // navegacion al dashboard
    private void loadDashboard(ActionEvent event, User user) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/itc/healthtrack/views/dashboard-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            DashboardController controller = fxmlLoader.getController();
            controller.initData(user);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            // mantener pantalla completa al entrar al dashboard
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error crítico al cargar la interfaz principal.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    // excepcion interna para distinguir errores de auth de errores de red
    private static class FirebaseLoginException extends Exception {
        public FirebaseLoginException(String message) { super(message); }
    }
}
