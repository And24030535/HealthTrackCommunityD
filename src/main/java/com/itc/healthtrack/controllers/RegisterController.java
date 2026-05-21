package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
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

import java.util.List;

/*Controlador que gestiona el registro de nuevos usuarios
 Valida los datos, guarda en Firestore y asigna un doctor automáticamente para pacientes*/
public class RegisterController {

    // Elementos de formulario
    @FXML private TextField txtFirstName;           // Campo: Nombre
    @FXML private TextField txtLastName;            // Campo: Apellido
    @FXML private TextField txtEmail;               // Campo: Email
    @FXML private TextField txtHeight;              // Campo: Estatura
    @FXML private DatePicker dpBirthDate;           // Campo: Fecha de nacimiento
    @FXML private PasswordField txtPassword;         // Campo: Contraseña
    @FXML private PasswordField txtConfirmPassword;  // Campo: Confirmar contraseña
    @FXML private ComboBox<String> comboGender;     // ComboBox: Género
    @FXML private ComboBox<String> comboRole;       // ComboBox: Rol (Paciente/Doctor)
    @FXML private Label lblStatus;                  // Etiqueta de estado/errores
    @FXML private Button btnRegister;               // Botón de registro

    // Acdeso a datos
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    // Inicializa los ComboBox con opciones de género y rol
    @FXML
    public void initialize() {
        comboGender.getItems().addAll("M", "F", "Otro");
        comboRole.getItems().addAll("Paciente", "Doctor");
        comboRole.getSelectionModel().selectFirst();
    }

    /*Procesa el registro de un nuevo usuario.
     Valida todos los campos, guarda el usuario en Firestore y asigna un doctor automáticamente si es paciente*/
    @FXML
    protected void onRegister(ActionEvent event) {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = comboRole.getValue();

        // Mapear roles en español a inglés para consistencia con el sistema
        final String mappedRole;
        if ("Paciente".equals(role)) {
            mappedRole = "patient";
        } else if ("Doctor".equals(role)) {
            mappedRole = "doctor";
        } else if ("Admin".equals(role)) {
            mappedRole = "admin";
        } else {
            mappedRole = "patient"; // por defecto: paciente
        }

        // Validaciones básicas
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Por favor, completa todos los campos obligatorios", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showStatus("Las contraseñas no coinciden", false);
            return;
        }

        if (password.length() < 6) {
            showStatus("La contraseña debe tener al menos 6 caracteres", false);
            return;
        }

        btnRegister.setDisable(true);

        new Thread(() -> {
            try {
                // Crear nuevo usuario
                User newUser = new User();
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setEmail(email);
                newUser.setPassword(password);
                newUser.setRole(mappedRole);
                newUser.setGender(comboGender.getValue());

                if (dpBirthDate.getValue() != null) {
                    newUser.setBirthDate(dpBirthDate.getValue().toString());
                }

                // Validar y parsear la altura
                String heightText = txtHeight.getText().trim();
                if (!heightText.isEmpty()) {
                    try {
                        newUser.setHeight(Double.parseDouble(heightText));
                    } catch (NumberFormatException e) {
                        Platform.runLater(() -> {
                            showStatus("Altura inválida.\n Verifica que sea un número decimal (ej: 1.75).", false);
                            btnRegister.setDisable(false);
                        });
                        return;
                    }
                }

                // Genera un ID nuevo para el usuario y lo asigna al modelo
                String newId = userDao.createDocumentId();
                newUser.setUid(newId);
                // Selecciona un doctor si el nuevo usuario es paciente
                if ("patient".equals(mappedRole)) {
                    // Busca todos los doctores disponibles
                    List<User> doctors = getDoctors();
                    if (!doctors.isEmpty()) {
                        // Selecciona un doctor al azar para asignarlo al paciente
                        User randomDoctor = doctors.get((int) (Math.random() * doctors.size()));
                        newUser.setAssignedDoctorId(randomDoctor.getUid());
                        System.out.println("Paciente registrado y asignado al doctor: " + randomDoctor.getFirstName() + " " + randomDoctor.getLastName());
                    } else {
                        System.out.println("Paciente registrado pero no hay doctores disponibles para asignar");
                    }
                }
                // Guarda el usuario completo en Firestore con el ID generado
                userDao.save(newId, newUser);

                Platform.runLater(() -> {
                    showStatus("¡Cuenta creada exitosamente! \nRedirigiendo al login...", true);
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
                    showStatus("Error al crear la cuenta, intenta de nuevo", false);
                    btnRegister.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /*Navega a la pantalla de inicio de sesión
     Se llama cuando el usuario hace clic en "Ir a Login" */
    @FXML
    protected void onGoToLogin(ActionEvent event) {
        goToLogin(event);
    }

    //Carga la pantalla de login aplicando los estilos CSS
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

    /*Muestra un mensaje de estado en la interfaz.
     Cambia el color según si es un éxito (verde) o un error (rojo)*/
    private void showStatus(String message, boolean isSuccess) {
        lblStatus.setText(message);
        lblStatus.setTextFill(isSuccess ? Color.web("#4caf50") : Color.web("#ff5252"));
        lblStatus.setVisible(true);
    }

    // Obtiene todos los usuarios con rol de doctor usando un filtro simple
    private List<User> getDoctors() throws Exception {
        // Consulta todos los usuarios que tengan rol de doctor
        List<User> doctors = userDao.getByField("role", "doctor");
        // Retorna la lista de doctores encontrados
        return doctors;
    }
}
