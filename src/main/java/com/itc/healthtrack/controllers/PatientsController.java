package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.dao.UserDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controlador para la gestion de pacientes. Permite registrar, editar,
 * visualizar y eliminar los pacientes asignados a un medico.
 */
public class PatientsController {

    // Componentes del formulario
    @FXML private TextField txtFirstName, txtLastName, txtEmail, txtBirthDate, txtHeight;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboGender;

    // Componentes de la tabla
    @FXML private TableView<User> tablePatients;
    @FXML private TableColumn<User, String> colFirstName, colLastName, colEmail, colGender;

    // Variables de logica de negocio
    private final PatientDAO patientDAO = new PatientDAO();
    private final UserDAO userDAO = new UserDAO();
    private ObservableList<User> patientsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private User selectedPatient = null;

    /**
     * Metodo de inicializacion que recibe el medico actualmente logueado.
     */
    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupTable();

        // Inicializar opciones del combo box
        comboGender.setItems(FXCollections.observableArrayList("M", "F", "Other"));

        loadPatients();
    }

    /**
     * Configura las columnas de la tabla para que coincidan con los atributos del modelo User.
     */
    private void setupTable() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));

        tablePatients.setItems(patientsObservableList);

        // Evento: Cuando se selecciona un paciente de la tabla, llena el formulario
        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedPatient = newSelection;
                fillForm(newSelection);
            }
        });
    }

    /**
     * Carga los pacientes del medico desde Firestore en un hilo secundario.
     */
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> list = patientDAO.getPatientsByDoctor(loggedInDoctor.getUid());
                Platform.runLater(() -> {
                    patientsObservableList.clear();
                    patientsObservableList.addAll(list);
                });
            } catch (Exception e) {
                System.out.println("Error al cargar pacientes.");
            }
        }).start();
    }

    @FXML
    protected void onSavePatient() {
        try {
            if (selectedPatient == null) {
                // Si no hay ninguno seleccionado, creamos uno nuevo
                User newPatient = new User();
                newPatient.setFirstName(txtFirstName.getText());
                newPatient.setLastName(txtLastName.getText());
                newPatient.setEmail(txtEmail.getText());
                newPatient.setBirthDate(txtBirthDate.getText());
                newPatient.setGender(comboGender.getValue());
                newPatient.setHeight(Double.parseDouble(txtHeight.getText()));

                // Valores fijos obligatorios para pacientes
                newPatient.setRole("patient");
                newPatient.setAssignedDoctorId(loggedInDoctor.getUid());
                if (!txtPassword.getText().isEmpty()) {
                    newPatient.setPassword(txtPassword.getText());
                }

                // Guardamos en un hilo nuevo
                new Thread(() -> {
                    try {
                        userDAO.saveUser(newPatient); // Usamos UserDAO porque inserta el UID
                        Platform.runLater(() -> {
                            onClearForm();
                            loadPatients();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                // Actualizar paciente existente
                selectedPatient.setFirstName(txtFirstName.getText());
                selectedPatient.setLastName(txtLastName.getText());
                selectedPatient.setEmail(txtEmail.getText());
                selectedPatient.setBirthDate(txtBirthDate.getText());
                selectedPatient.setGender(comboGender.getValue());
                selectedPatient.setHeight(Double.parseDouble(txtHeight.getText()));

                new Thread(() -> {
                    try {
                        patientDAO.updatePatient(selectedPatient);
                        Platform.runLater(() -> {
                            onClearForm();
                            loadPatients();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Asegurate de que la altura sea un numero.");
        }
    }

    @FXML
    protected void onDeletePatient() {
        if (selectedPatient != null) {
            new Thread(() -> {
                try {
                    patientDAO.deletePatient(selectedPatient.getUid());
                    Platform.runLater(() -> {
                        onClearForm();
                        loadPatients();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    protected void onClearForm() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtBirthDate.clear();
        comboGender.setValue(null);
        txtHeight.clear();
        txtPassword.clear();

        selectedPatient = null;
        tablePatients.getSelectionModel().clearSelection();
    }

    private void fillForm(User p) {
        txtFirstName.setText(p.getFirstName());
        txtLastName.setText(p.getLastName());
        txtEmail.setText(p.getEmail());
        txtBirthDate.setText(p.getBirthDate() != null ? p.getBirthDate() : "");
        comboGender.setValue(p.getGender());
        txtHeight.setText(p.getHeight() != null ? String.valueOf(p.getHeight()) : "");
        txtPassword.clear(); // Never pre-fill passwords
    }
}