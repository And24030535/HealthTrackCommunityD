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

/**
 * Controlador para la gestion de pacientes. Permite registrar, editar,
 * visualizar y eliminar los pacientes asignados a un medico.
 */
public class PatientsController {

    // Componentes del formulario
    @FXML private TextField txtFirstName, txtLastName, txtEmail, txtHeight;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboGender;
    @FXML private DatePicker dpBirthDate;

    // Componentes de la tabla
    @FXML private TableView<User> tablePatients;
    @FXML private TableColumn<User, String> colFirstName, colLastName, colEmail, colGender;

    // Variables de logica de negocio
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private ObservableList<User> patientsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private User selectedPatient = null;

    // Metodo de inicializacion que recibe el medico actualmente logueado
    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupTable();

        // Inicializar opciones del combo box
        comboGender.setItems(FXCollections.observableArrayList("M", "F", "Other"));

        loadPatients();
    }

    //Carga los pacientes desde Firestore y llena el formulario al seleccionar uno
    private void setupTable() {
        // Vincula cada columna con su propiedad correspondiente del modelo User
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));

        tablePatients.setItems(patientsObservableList);

        // Cuando se selecciona un paciente de la tabla, llena el formulario
        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedPatient = newSelection;
                fillForm(newSelection);
            }
        });
    }

    //Carga los pacientes del medico desde Firestore en un hilo secundario
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> list = getPatientsForDoctor(loggedInDoctor);
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
                newPatient.setBirthDate(dpBirthDate.getValue() != null ? dpBirthDate.getValue().toString() : null);
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
                        // Genera un ID nuevo para el paciente en Firestore
                        String newId = userDao.createDocumentId();
                        // Asigna el ID al objeto y guarda el paciente completo
                        newPatient.setUid(newId);
                        userDao.save(newId, newPatient);
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
                selectedPatient.setBirthDate(dpBirthDate.getValue() != null ? dpBirthDate.getValue().toString() : null);
                selectedPatient.setGender(comboGender.getValue());
                selectedPatient.setHeight(Double.parseDouble(txtHeight.getText()));

                new Thread(() -> {
                    try {
                        userDao.save(selectedPatient.getUid(), selectedPatient);
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

    //Elimina el paciente seleccionado de la base de datos
    @FXML
    protected void onDeletePatient() {
        if (selectedPatient != null) {
            new Thread(() -> {
                try {
                    userDao.delete(selectedPatient.getUid());
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

    //Limpia todos los campos del formulario y deselecciona el paciente
    @FXML
    protected void onClearForm() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        dpBirthDate.setValue(null);
        comboGender.setValue(null);
        txtHeight.clear();
        txtPassword.clear();

        selectedPatient = null;
        tablePatients.getSelectionModel().clearSelection();
    }

    /*Llena los campos del formulario con los datos del paciente seleccionado
     Se usa para editar un paciente existente*/
    private void fillForm(User p) {
        txtFirstName.setText(p.getFirstName());
        txtLastName.setText(p.getLastName());
        txtEmail.setText(p.getEmail());
        dpBirthDate.setValue(p.getBirthDate() != null ? LocalDate.parse(p.getBirthDate()) : null);
        comboGender.setValue(p.getGender());
        txtHeight.setText(p.getHeight() != null ? String.valueOf(p.getHeight()) : "");
        txtPassword.clear();
    }

    // Obtiene la lista de pacientes visibles para el usuario logeado
    private List<User> getPatientsForDoctor(User doctor) throws Exception {
        // Lista que se devolverá al final
        List<User> result = new ArrayList<>();
        // Consulta todos los usuarios con rol de paciente
        List<User> patients = userDao.getByField("role", "patient");
        // Recorre cada paciente para filtrar según el rol del usuario logeado
        for (User patient : patients) {
            if ("admin".equals(doctor.getRole())) {
                // El administrador puede ver todos los pacientes
                result.add(patient);
            } else if (doctor.getUid() != null && doctor.getUid().equals(patient.getAssignedDoctorId())) {
                // El médico solo ve a sus pacientes asignados
                result.add(patient);
            }
        }
        // Retorna la lista final de pacientes filtrados
        return result;
    }
}
