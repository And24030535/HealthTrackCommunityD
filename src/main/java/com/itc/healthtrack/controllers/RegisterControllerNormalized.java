package com.itc.healthtrack.controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.dao.RoleDAO;
import com.itc.healthtrack.dao.UserProfileDAO;
import com.itc.healthtrack.dao.PatientDAONormalized;
import com.itc.healthtrack.dao.DoctorDAO;
import com.itc.healthtrack.models.Role;
import com.itc.healthtrack.models.UserProfile;
import com.itc.healthtrack.models.Patient;
import com.itc.healthtrack.models.Doctor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.UUID;

/**
 * Controlador para el registro de nuevos usuarios con autenticación Firebase
 * Implementa un flujo de dos pasos normalizado:
 * 1. Crear usuario en Firebase Authentication
 * 2. Crear UserProfile y Patient/Doctor en Firestore
 */
public class RegisterControllerNormalized {

    // Elementos de interfaz
    @FXML private TextField txtFirstName;           // Campo para el nombre
    @FXML private TextField txtLastName;            // Campo para el apellido
    @FXML private TextField txtEmail;               // Campo para el correo
    @FXML private PasswordField txtPassword;        // Campo para la contraseña
    @FXML private PasswordField txtConfirmPassword; // Campo para confirmar la contraseña
    @FXML private ComboBox<String> cbRole;          // ComboBox para seleccionar el rol
    @FXML private Label lblStatus;                  // Etiqueta para mostrar mensajes de estado
    @FXML private DatePicker dtBirthDate;           // Selector de fecha de nacimiento
    @FXML private ComboBox<String> cbGender;        // ComboBox para seleccionar género
    @FXML private TextField txtHeight;              // Campo para la estatura

    // DAOs para acceder a la base de datos
    private final RoleDAO roleDAO = new RoleDAO();
    private final UserProfileDAO userProfileDAO = new UserProfileDAO();
    private final PatientDAONormalized patientDAO = new PatientDAONormalized();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    // Instancia de Firebase Auth para gestionar autenticación
    private final FirebaseAuth firebaseAuth = FirebaseConnection.getInstance().getAuth();

    /**
     * Inicializa el formulario de registro
     * Se ejecuta automáticamente cuando se carga el FXML
     */
    @FXML
    public void initialize() {
        // Configuramos las opciones disponibles en el ComboBox de roles
        cbRole.getItems().addAll("Paciente", "Doctor");
        cbRole.setValue("Paciente"); // Rol por defecto

        // Configuramos el género
        cbGender.getItems().addAll("M", "F", "Otro");
        cbGender.setValue("M");

        // Escuchador para mostrar/ocultar campos según el rol seleccionado
        cbRole.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Si selecciona "Paciente", mostramos campos de paciente
            if ("Paciente".equals(newVal)) {
                dtBirthDate.setVisible(true);
                cbGender.setVisible(true);
                txtHeight.setVisible(true);
            } else {
                // Si selecciona "Doctor", ocultamos campos específicos del paciente
                dtBirthDate.setVisible(false);
                cbGender.setVisible(false);
                txtHeight.setVisible(false);
            }
        });
    }

    /**
     * Maneja el evento de clic en el botón "Registrar"
     * Ejecuta el flujo completo de registro en un hilo separado
     */
    @FXML
    protected void onRegister() {
        // Validamos que todos los campos esenciales estén completos
        if (!validateInput()) {
            return;
        }

        // Mostramos mensaje indicando que se está procesando el registro
        lblStatus.setText("Registrando usuario...");

        // Ejecutamos el registro en un hilo separado para no bloquear la interfaz
        new Thread(() -> {
            try {
                // PASO 1: Crear el usuario en Firebase Authentication
                // Esto genera un UID único que vinculamos con el perfil en Firestore
                UserRecord userRecord = firebaseAuth.createUser(new com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(txtEmail.getText().trim())
                        .setPassword(txtPassword.getText())
                        .setDisplayName(txtFirstName.getText() + " " + txtLastName.getText()));

                // Obtenemos el UID generado por Firebase Auth
                String authUid = userRecord.getUid();

                // PASO 2: Crear el UserProfile en Firestore con el authUid
                String userProfileId = UUID.randomUUID().toString(); // Generamos un ID único

                // Determinamos el roleId basado en la selección
                String roleId = "Paciente".equals(cbRole.getValue()) ? "patient" : "doctor";

                // Creamos el perfil de usuario
                UserProfile userProfile = new UserProfile(
                        userProfileId,           // ID del documento
                        authUid,                 // UID de Firebase Auth (linkage crucial)
                        roleId,                  // ID del rol
                        txtEmail.getText().trim(), // Correo
                        System.currentTimeMillis() // Timestamp de registro
                );

                // Guardamos el UserProfile en Firestore
                userProfileDAO.createUserProfile(userProfile);

                // PASO 3: Crear Patient o Doctor según el rol seleccionado
                if ("Paciente".equals(cbRole.getValue())) {
                    // Creamos un documento Patient
                    String patientId = UUID.randomUUID().toString();
                    Patient patient = new Patient(
                            patientId,                              // ID único
                            userProfileId,                          // Referencia a UserProfile
                            null,                                   // Sin doctor asignado inicialmente
                            txtFirstName.getText().trim(),          // Nombre
                            txtLastName.getText().trim(),           // Apellido
                            dtBirthDate.getValue() != null ? dtBirthDate.getValue().toString() : null,
                            cbGender.getValue(),
                            txtHeight.getText().isEmpty() ? null : Double.parseDouble(txtHeight.getText())
                    );
                    patientDAO.createPatient(patient);
                } else {
                    // Creamos un documento Doctor
                    String doctorId = UUID.randomUUID().toString();
                    Doctor doctor = new Doctor(
                            doctorId,              // ID único
                            userProfileId,         // Referencia a UserProfile
                            null,                  // Sin especialidad asignada inicialmente
                            "",                    // Sin licencia específica
                            txtFirstName.getText().trim(), // Nombre
                            txtLastName.getText().trim()   // Apellido
                    );
                    doctorDAO.createDoctor(doctor);
                }

                // Mostramos mensaje de éxito en el hilo de la interfaz
                Platform.runLater(() -> {
                    lblStatus.setText("Registro exitoso. Redirigiendo al login...");
                    // Después de 2 segundos, redirigimos al login
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(this::goToLogin);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });

            } catch (FirebaseAuthException e) {
                // Si hay error en Firebase Auth (email duplicado, contraseña débil, etc.)
                Platform.runLater(() -> {
                    lblStatus.setText("Error en autenticación: " + e.getMessage());
                });
            } catch (Exception e) {
                // Si hay error en la creación de documentos Firestore
                Platform.runLater(() -> {
                    lblStatus.setText("Error al registrar: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * Valida que todos los campos obligatorios estén completos
     * @return true si la validación es exitosa, false en caso contrario
     */
    private boolean validateInput() {
        // Validamos que el nombre no esté vacío
        if (txtFirstName.getText().trim().isEmpty()) {
            lblStatus.setText("El nombre es obligatorio");
            return false;
        }

        // Validamos que el apellido no esté vacío
        if (txtLastName.getText().trim().isEmpty()) {
            lblStatus.setText("El apellido es obligatorio");
            return false;
        }

        // Validamos que el correo no esté vacío
        if (txtEmail.getText().trim().isEmpty()) {
            lblStatus.setText("El correo es obligatorio");
            return false;
        }

        // Validamos que la contraseña no esté vacía
        if (txtPassword.getText().isEmpty()) {
            lblStatus.setText("La contraseña es obligatoria");
            return false;
        }

        // Validamos que las contraseñas coincidan
        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            lblStatus.setText("Las contraseñas no coinciden");
            return false;
        }

        // Validamos que la contraseña tenga al menos 6 caracteres
        if (txtPassword.getText().length() < 6) {
            lblStatus.setText("La contraseña debe tener al menos 6 caracteres");
            return false;
        }

        return true;
    }

    /**
     * Regresa a la pantalla de login
     */
    private void goToLogin() {
        try {
            // Cargamos el FXML de la ventana de login
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene loginScene = new Scene(fxmlLoader.load(), 800, 600);

            // Aplicamos el tema de BootstrapFX
            loginScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

            // Cargamos el CSS personalizado
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            loginScene.getStylesheets().add(cssPath);

            // Obtenemos la ventana actual y establecemos la nueva escena
            Stage stage = (Stage) txtFirstName.getScene().getWindow();
            stage.setScene(loginScene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Regresa a la pantalla de login sin registrarse
     */
    @FXML
    protected void onBackToLogin() {
        goToLogin();
    }
}

