package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Controlador para la gestión de pacientes (CRUD).
// Las notas médicas se manejan en RecommendationsController (sección Inteligencia Clínica).
public class PatientsController {

    // Formulario del paciente
    @FXML private TextField    txtFirstName, txtLastName, txtEmail, txtHeight;
    @FXML private ComboBox<String> comboGender;
    @FXML private DatePicker   dpBirthDate;

    // Tabla
    @FXML private TableView<User>               tablePatients;
    @FXML private TableColumn<User, String>     colFirstName, colLastName, colEmail, colGender;

    // DAOs
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    private final ObservableList<User> patientsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private User selectedPatient = null;

    // -------------------------------------------------------------------
    // Inicialización
    // -------------------------------------------------------------------

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupTable();
        comboGender.setItems(FXCollections.observableArrayList("M", "F", "Otro"));
        loadPatients();
    }

    // -------------------------------------------------------------------
    // Configuración de tabla
    // -------------------------------------------------------------------

    private void setupTable() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName .setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail    .setCellValueFactory(new PropertyValueFactory<>("email"));
        colGender   .setCellValueFactory(new PropertyValueFactory<>("gender"));

        tablePatients.setItems(patientsObservableList);

        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedPatient = newVal;
                fillForm(newVal);
            }
        });
    }

    // -------------------------------------------------------------------
    // CRUD de pacientes
    // -------------------------------------------------------------------

    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> all = userDao.getByField("role", "patient");
                List<User> visible = new ArrayList<>();
                for (User p : all) {
                    if ("admin".equals(loggedInDoctor.getRole())) {
                        visible.add(p);
                    } else if (loggedInDoctor.getUid() != null && loggedInDoctor.getUid().equals(p.getAssignedDoctorId())) {
                        visible.add(p);
                    }
                }
                Platform.runLater(() -> {
                    patientsObservableList.clear();
                    patientsObservableList.addAll(visible);
                });
            } catch (Exception e) {
                System.err.println("[PatientsController] Error al cargar pacientes: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onSavePatient() {
        try {
            if (selectedPatient == null) {
                User newPatient = new User();
                fillUserFromForm(newPatient);
                newPatient.setRole("patient");
                newPatient.setAssignedDoctorId(loggedInDoctor.getUid());

                new Thread(() -> {
                    try {
                        String newId = userDao.createDocumentId();
                        newPatient.setUid(newId);
                        userDao.save(newId, newPatient);
                        Platform.runLater(() -> { onClearForm(); loadPatients(); });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            } else {
                fillUserFromForm(selectedPatient);
                new Thread(() -> {
                    try {
                        userDao.save(selectedPatient.getUid(), selectedPatient);
                        Platform.runLater(() -> { onClearForm(); loadPatients(); });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: la altura debe ser un número.");
        }
    }

    @FXML
    protected void onDeletePatient() {
        if (selectedPatient == null) return;
        new Thread(() -> {
            try {
                userDao.delete(selectedPatient.getUid());
                Platform.runLater(() -> { onClearForm(); loadPatients(); });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    protected void onClearForm() {
        txtFirstName.clear();
        txtLastName .clear();
        txtEmail    .clear();
        dpBirthDate .setValue(null);
        comboGender .setValue(null);
        txtHeight   .clear();

        selectedPatient = null;
        tablePatients.getSelectionModel().clearSelection();
    }

    private void fillForm(User p) {
        txtFirstName.setText(p.getFirstName());
        txtLastName .setText(p.getLastName());
        txtEmail    .setText(p.getEmail());
        dpBirthDate .setValue(p.getBirthDate() != null ? LocalDate.parse(p.getBirthDate()) : null);
        comboGender .setValue(p.getGender());
        txtHeight   .setText(p.getHeight() != null ? String.valueOf(p.getHeight()) : "");
    }

    private void fillUserFromForm(User u) {
        u.setFirstName(txtFirstName.getText());
        u.setLastName (txtLastName.getText());
        u.setEmail    (txtEmail.getText());
        u.setBirthDate(dpBirthDate.getValue() != null ? dpBirthDate.getValue().toString() : null);
        u.setGender   (comboGender.getValue());
        u.setHeight   (txtHeight.getText().isEmpty() ? null : Double.parseDouble(txtHeight.getText()));
    }

}
