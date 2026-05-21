package com.itc.healthtrack.controllers;

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
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.List;

/**
 * Controlador encargado de la autenticacion de usuarios.
 */
public class LoginController {

    // Elementos de interfaz
    @FXML private TextField emailField;         // Campo de email
    @FXML private PasswordField passwordField;   // Campo de contraseña
    @FXML private Label errorLabel;              // Etiqueta para mostrar errores
    @FXML private Button loginButton;            // Botón de inicio de sesión

    //Acceso a datos
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    //Navega a la pantalla de crear nueva cuenta

    @FXML
    protected void onGoToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/register-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 700);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al cargar la pantalla de registro");
        }
    }

    // Procesa el intento de inicio de sesion
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
                // Consulta a Firestore con correo y contraseña
                User currentUser = findUserByCredentials(email, password);

                Platform.runLater(() -> {
                    if (currentUser != null) {
                        // Asignar doctor automáticamente si es paciente sin asignación
                        if ("patient".equals(currentUser.getRole()) &&
                            (currentUser.getAssignedDoctorId() == null || currentUser.getAssignedDoctorId().isEmpty())) {

                            new Thread(() -> {
                                try {
                                    // Busca todos los doctores disponibles
                                    List<User> doctors = userDao.getByField("role", "doctor");
                                    if (!doctors.isEmpty()) {
                                        // Selecciona un doctor aleatorio y lo asigna al paciente
                                        User randomDoctor = doctors.get((int) (Math.random() * doctors.size()));
                                        currentUser.setAssignedDoctorId(randomDoctor.getUid());
                                        userDao.save(currentUser.getUid(), currentUser);
                                        System.out.println("Paciente " + currentUser.getFirstName() + " asignado al doctor: " + randomDoctor.getFirstName() + " " + randomDoctor.getLastName());
                                    }
                                } catch (Exception e) {
                                    System.out.println("Error al asignar doctor automáticamente: " + e.getMessage());
                                }
                                // Después de intentar asignar, cargar el dashboard
                                Platform.runLater(() -> loadDashboard(event, currentUser));
                            }).start();
                        } else {
                            loadDashboard(event, currentUser);
                        }
                    } else {
                        showError("Credenciales inválidas");
                        loginButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error al conectar con la base de datos");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    //Muestra un mensaje de error en la interfaz.
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    //Configura y muestra la escena del Dashboard inyectando los estilos CSS
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

    // Busca un usuario por correo y contraseña usando una consulta simple
    private User findUserByCredentials(String email, String password) throws Exception {
        // Lista de usuarios que coinciden con el correo
        List<User> users = userDao.getByField("email", email);
        // Recorre cada usuario para validar la contraseña
        for (User user : users) {
            if (password.equals(user.getPassword())) {
                return user;
            }
        }
        // Si no hay coincidencias válidas, retorna null
        return null;
    }
}
