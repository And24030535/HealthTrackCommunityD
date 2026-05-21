package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Controlador del Panel de Administración
// Gestiona la visualización de estadísticas de usuarios, búsqueda, eliminación y cambio de roles
public class AdminController {

    // Elementos de la interfaz
    @FXML private Label lblTotalUsers;      // Etiqueta para mostrar total de usuarios
    @FXML private Label lblTotalDoctors;    // Etiqueta para mostrar total de médicos
    @FXML private Label lblTotalPatients;   // Etiqueta para mostrar total de pacientes
    @FXML private Label lblStatus;          // Etiqueta para mensajes de estado

    // Tabla de columnas
    @FXML private TableView<User> tableUsers;                    // Tabla principal de usuarios
    @FXML private TableColumn<User, String> colFirstName;        // Columna: Nombre
    @FXML private TableColumn<User, String> colLastName;         // Columna: Apellido
    @FXML private TableColumn<User, String> colEmail;            // Columna: Correo
    @FXML private TableColumn<User, String> colRole;             // Columna: Rol
    @FXML private TableColumn<User, String> colAssignedDoctor;   // Columna: Doctor Asignado
    @FXML private TextField txtSearch;                           // Campo de búsqueda
    @FXML private ComboBox<String> cbRoleFilter;                 // Filtro por rol

    // Datos y controladores
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users"); // Acceso a datos de usuarios
    private final ObservableList<User> usersObservableList = FXCollections.observableArrayList();  // Lista observable de usuarios
    private FilteredList<User> filteredList;                     // Lista filtrada para búsqueda

    private User loggedInAdmin;             // Usuario administrador logeado
    private User selectedUser = null;       // Usuario seleccionado en la tabla

    // Metodo para traducir variable a vista del usuario
    private String translateRole(String role) {
        if (role == null) return "—";
        switch (role) {
            case "patient": return "Paciente";
            case "doctor":  return "Doctor";
            case "admin":   return "Admin";
            default:        return role;
        }
    }

    private String getRoleValue(String roleLabel) {
        if (roleLabel == null) return "patient";
        switch (roleLabel) {
            case "Paciente": return "patient";
            case "Doctor":   return "doctor";
            case "Admin":    return "admin";
            default:         return roleLabel;
        }
    }

    /*Inicializa el controlador con los datos del administrador logeado
     Configura la interfaz y carga la lista de usuarios  */
    public void initData(User admin) {
        this.loggedInAdmin = admin;
        setupSearchControls();   // Configura los controles de búsqueda
        setupTable();            // Configura las columnas de la tabla
        loadAllUsers();          // Carga todos los usuarios desde Firestore
    }

    /*Configura el filtro de roles (Todos, Doctor, Paciente, Admin)
    esto permite filtrar usuarios por su rol de forma rápida*/
    private void setupSearchControls() {
        cbRoleFilter.setItems(FXCollections.observableArrayList("Todos", "Paciente", "Doctor", "Admin"));
        cbRoleFilter.setValue("Todos");  // Por defecto muestra todos
    }

    /*Configura las columnas de la tabla para mostrar datos de los usuarios
    , también configura el escuchador para detectar cuando se selecciona un usuario*/
    private void setupTable() {
        // Vincula cada columna con su propiedad correspondiente del modelo User
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));      // obtiene el dato
        colRole.setCellFactory(column -> new TableCell<User, String>() { //traduce
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                } else {
                    setText(translateRole(role));  // Usa el método auxiliar
                }
            }
        });
        colAssignedDoctor.setCellValueFactory(new PropertyValueFactory<>("assignedDoctorName"));

        // Crea una lista filtrable a partir de la lista de usuarios
        filteredList = new FilteredList<>(usersObservableList, u -> true);
        tableUsers.setItems(filteredList);

        // cuando se selecciona un usuario en la tabla, se guarda la selección
        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                lblStatus.setText("Usuario seleccionado: " + newVal.getFirstName() + " " + newVal.getLastName());
                lblStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    /*Carga todos los usuarios desde la base de datos (Firestore) en un hilo de fondo.
     Luego actualiza la tabla y las estadísticas en el hilo principal de la interfaz.
     mapea el nombre de cada médico asignado a los pacientes para que aparezca de forma legible en la columna "Doctor Asignado" */
    private void loadAllUsers() {
        new Thread(() -> {
            try {
                // Obtiene todos los usuarios de Firestore
                List<User> allUsers = userDao.getAll();

                // Crea un mapa para convertir IDs de médicos a sus nombres
                Map<String, String> doctorNameMap = new HashMap<>();

                // Llena el mapa
                for (User user : allUsers) {
                    if ("doctor".equals(user.getRole())) {
                        doctorNameMap.put(user.getUid(), user.getFirstName() + " " + user.getLastName());
                    }
                }

                // Asigna el nombre del médico a cada paciente
                for (User user : allUsers) {
                    if ("patient".equals(user.getRole()) && user.getAssignedDoctorId() != null) {
                        String doctorName = doctorNameMap.get(user.getAssignedDoctorId());
                        user.setAssignedDoctorName(doctorName != null ? doctorName : "—");
                    } else if ("patient".equals(user.getRole())) {
                        user.setAssignedDoctorName("—");  // Sin médico asignado
                    }
                }

                // Cuenta cuántos médicos y pacientes hay
                int doctors = 0;
                int patients = 0;
                for (User user : allUsers) {
                    if ("doctor".equals(user.getRole())) {
                        doctors++;
                    }
                    if ("patient".equals(user.getRole())) {
                        patients++;
                    }
                }
                // Copias finales para usarlas dentro del hilo de la interfaz
                final int doctorCount = doctors;
                final int patientCount = patients;

                // Ejecuta en el hilo de la interfaz gráfica
                Platform.runLater(() -> {
                    usersObservableList.clear();
                    usersObservableList.addAll(allUsers);

                    // Actualiza las etiquetas de estadísticas
                    lblTotalUsers.setText(String.valueOf(allUsers.size()));
                    lblTotalDoctors.setText(String.valueOf(doctorCount));
                    lblTotalPatients.setText(String.valueOf(patientCount));

                    applyFilter();  // Aplica el filtro actual
                    lblStatus.setText("Lista actualizada\n" + allUsers.size() + " usuario(s) encontrado(s)");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar la lista de usuarios");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    /* Se ejecuta cuando el usuario hace clic en el botón "Buscar"
     Aplica los filtros de búsqueda y rol a la tabla*/
    @FXML
    protected void onSearch() {
        applyFilter();
    }

    // Aplica filtros a la tabla
    private void applyFilter() {
        // Obtiene el texto de búsqueda, limpiado y en minúsculas
        String keyword    = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        String roleFilter = cbRoleFilter.getValue();

        // Convierte el rol del ComboBox al valor de la BD
        String roleValue = getRoleValue(roleFilter);
        String finalRoleValue = roleValue;

        // Establece la condición de filtro
        filteredList.setPredicate(user -> {
            // Verifica que el rol coincida (o si está marcado "Todos")
            boolean roleMatch = "Todos".equals(roleFilter) || finalRoleValue.equals(user.getRole());

            // Verifica que el texto sea encontrado en nombre, apellido o correo
            boolean textMatch = keyword.isEmpty()
                    || (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(keyword))
                    || (user.getLastName()  != null && user.getLastName().toLowerCase().contains(keyword))
                    || (user.getEmail()     != null && user.getEmail().toLowerCase().contains(keyword));

            return roleMatch && textMatch;  // Ambos deben cumplirse
        });

        // Muestra cuántos usuarios se están mostrando
        lblStatus.setText("Mostrando " + filteredList.size() + " de " + usersObservableList.size() + " usuario(s).");
        lblStatus.setTextFill(Color.web("#aaaaaa"));
    }

    /* Se ejecuta cuando el usuario hace clic en el botón "Limpiar"
     Limpia el campo de búsqueda y resetea el filtro a "Todos"*/
    @FXML
    protected void onClearSearch() {
        txtSearch.clear();
        cbRoleFilter.setValue("Todos");
        applyFilter();
    }

    // ver detalles de un usuario

    /* Abre un diálogo que muestra todos los detalles del usuario seleccionado.
     Muestra información diferente según el rol (paciente, médico, admin)  */
    @FXML
    protected void onViewDetails() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario de la tabla para ver sus detalles.");
            lblStatus.setTextFill(Color.web("#ffffff"));
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalle del Usuario");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPrefWidth(440);
        grid.setStyle("-fx-background-color: #ffffff; -fx-padding: 20;");

        Label lblHeader = new Label(selectedUser.getFirstName() + " " + selectedUser.getLastName());
        lblHeader.setStyle("-fx-text-fill: #000000; -fx-font-size: 18px; -fx-font-weight: bold;");
        GridPane.setColumnSpan(lblHeader, 2);
        grid.add(lblHeader, 0, 0);

        int row = 1;
        row = addDetailRow(grid, row, "UID",      selectedUser.getUid());
        row = addDetailRow(grid, row, "Correo",   selectedUser.getEmail());
        row = addDetailRow(grid, row, "Nombre",   selectedUser.getFirstName());
        row = addDetailRow(grid, row, "Apellido", selectedUser.getLastName());
        row = addDetailRow(grid, row, "Rol",      translateRole(selectedUser.getRole())); // Traduce el rol

        if ("patient".equals(selectedUser.getRole())) {
            row = addDetailRow(grid, row, "Nacimiento", selectedUser.getBirthDate());
            row = addDetailRow(grid, row, "Género",     selectedUser.getGender());
            row = addDetailRow(grid, row, "Estatura",   selectedUser.getHeight() != null ? selectedUser.getHeight() + " m" : "—");
            row = addDetailRow(grid, row, "Médico ID",  selectedUser.getAssignedDoctorId());
        }

        if ("doctor".equals(selectedUser.getRole())) {
            final int finalRow = row;
            new Thread(() -> {
                try {
                    int count = getPatientsByDoctorId(selectedUser.getUid()).size();
                    Platform.runLater(() -> addDetailRow(grid, finalRow, "Pacientes", count + " paciente(s)"));
                } catch (Exception e) {
                    Platform.runLater(() -> addDetailRow(grid, finalRow, "Pacientes", "Error al cargar"));
                }
            }).start();
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(300);
        scrollPane.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff; -fx-border-color: rgb(255 255 255 / 0);");

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(scrollPane);
        dp.setHeader(null);
        dp.setGraphic(null);
        dp.getButtonTypes().add(ButtonType.CLOSE);
        dp.setStyle("-fx-background-color: #ffffff;");

        ((Button) dp.lookupButton(ButtonType.CLOSE))
                .setStyle("-fx-background-color: #6a6a6a; -fx-text-fill: #020000; -fx-cursor: hand; -fx-padding: 6 20;");

        dialog.showAndWait();
    }

    /*Metodo auxiliar que agrega una fila de detalle al GridPane.
    Cada fila tiene una etiqueta (label) y un valor (value).*/
    private int addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");

        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.setStyle("-fx-text-fill: #000000;");
        val.setWrapText(true);  // Permite que el texto se envuelva si es muy largo

        grid.add(lbl, 0, row);   // Etiqueta en columna 0
        grid.add(val, 1, row);   // Valor en columna 1
        return row + 1;          // Devuelve la siguiente fila disponible
    }

    // Eliminar usuario
    //- No permite eliminar si ningún usuario está seleccionado
    //- No permite que un admin se elimine a sí mismo
    //Si el usuario a eliminar es un médico, reasigna automáticamente sus pacientes a otro médico disponible
    @FXML
    protected void onDeleteUser() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario de la tabla para eliminar");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        // no permite que un admin se elimine a sí mismo
        if (selectedUser.getUid() != null && selectedUser.getUid().equals(loggedInAdmin.getUid())) {
            lblStatus.setText("No puedes eliminar tu propia cuenta de administrador");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        String userId = selectedUser.getUid();
        String userName = selectedUser.getFirstName() + " " + selectedUser.getLastName();
        lblStatus.setText("Eliminando usuario: " + userName + "...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        // Ejecuta la eliminación en un hilo de fondo para no bloquear la interfaz
        new Thread(() -> {
            try {
                userDao.delete(userId);

                // Si era un médico, reasigna sus pacientes a otro médico
                if ("doctor".equals(selectedUser.getRole())) {
                    reassignPatients(userId);
                }

                // Recarga la tabla en el hilo de la interfaz
                Platform.runLater(() -> {
                    selectedUser = null;
                    tableUsers.getSelectionModel().clearSelection();
                    loadAllUsers();  // Recarga para reflejar los cambios
                    lblStatus.setText("Usuario eliminado correctamente");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al eliminar el usuario, intenta de nuevo");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Cambiar rol de un usuario
    // Después de cambiar, recarga la lista de usuarios para reflejar el cambio.
    @FXML
    protected void onChangeRole() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario primero");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        // Crea un diálogo de opciones con los roles disponibles en español
        ChoiceDialog<String> dialog = new ChoiceDialog<>(translateRole(selectedUser.getRole()),
                "Paciente", "Doctor", "Admin");
        dialog.setTitle("Cambiar Rol");
        dialog.setHeaderText("Usuario: " + selectedUser.getFirstName() + " " + selectedUser.getLastName());
        dialog.setContentText("Selecciona el nuevo rol:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1e1e1e; -fx-font-size: 13px;");

        // Si el usuario elige un nuevo rol
        dialog.showAndWait().ifPresent(newRoleLabel -> {
            String newRole = getRoleValue(newRoleLabel);  // Convierte a inglés
            // Si seleccionó el mismo rol, no hace nada
            if (newRole.equals(selectedUser.getRole())) return;

            // Actualiza el rol en la base de datos
            new Thread(() -> {
                try {
                    selectedUser.setRole(newRole);
                    userDao.save(selectedUser.getUid(), selectedUser);
                    Platform.runLater(() -> {
                        loadAllUsers();  // Recarga la tabla
                        lblStatus.setText("Rol actualizado a: " + newRoleLabel); // Muestra en español
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al actualizar el rol");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // Obtiene la lista de pacientes asignados a un médico específico
    private List<User> getPatientsByDoctorId(String doctorId) throws Exception {
        // Lista final de pacientes asignados
        List<User> result = new ArrayList<>();
        // Consulta todos los usuarios con rol de paciente
        List<User> patients = userDao.getByField("role", "patient");
        // Recorre cada paciente para validar el médico asignado
        for (User patient : patients) {
            if (doctorId != null && doctorId.equals(patient.getAssignedDoctorId())) {
                result.add(patient);
            }
        }
        // Devuelve la lista filtrada
        return result;
    }

    // Reasigna o desasigna pacientes cuando se elimina un médico
    private void reassignPatients(String doctorId) throws Exception {
        // Obtiene los pacientes que pertenecen al médico eliminado
        List<User> patients = getPatientsByDoctorId(doctorId);
        // Obtiene todos los doctores registrados en el sistema
        List<User> doctors = userDao.getByField("role", "doctor");
        // Lista de doctores disponibles sin el médico eliminado
        List<User> availableDoctors = new ArrayList<>();
        for (User doctor : doctors) {
            if (doctorId == null || !doctorId.equals(doctor.getUid())) {
                availableDoctors.add(doctor);
            }
        }

        if (!availableDoctors.isEmpty()) {
            // Reasigna pacientes en forma circular cuando hay doctores disponibles
            int doctorIndex = 0;
            for (User patient : patients) {
                User newDoctor = availableDoctors.get(doctorIndex % availableDoctors.size());
                patient.setAssignedDoctorId(newDoctor.getUid());
                userDao.save(patient.getUid(), patient);
                doctorIndex++;
            }
        } else {
            // Si no hay doctores, se desasigna el médico de cada paciente
            for (User patient : patients) {
                patient.setAssignedDoctorId(null);
                userDao.save(patient.getUid(), patient);
            }
        }
    }
}
