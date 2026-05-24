package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

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
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
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
            default:                     return role;
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

        // Establece la condición de filtro
        filteredList.setPredicate(user -> {
            // Verifica que el rol coincida (o si está marcado "Todos")
            boolean roleMatch = "Todos".equals(roleFilter) || roleValue.equals(user.getRole());

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
        DialogUtils.applyWhiteStyle(dp);

        dialog.showAndWait();
    }

    /*Metodo auxiliar que agrega una fila de detalle al GridPane
    Cada fila tiene una etiqueta (label) y un valor (value)*/
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

    // Punto de entrada para eliminación: separa lógica de médico vs. otros roles
    @FXML
    protected void onDeleteUser() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }
        // El admin no puede eliminarse a sí mismo
        if (selectedUser.getUid() != null && selectedUser.getUid().equals(loggedInAdmin.getUid())) {
            lblStatus.setText("No puedes eliminar tu propia cuenta de administrador.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        // Los médicos requieren reasignación obligatoria antes de poder eliminarse
        if ("doctor".equals(selectedUser.getRole())) {
            handleDoctorDeletion();
        } else {
            handleNonDoctorDeletion();
        }
    }

    // Verifica cuántos pacientes tiene el médico y decide el flujo de eliminación
    private void handleDoctorDeletion() {
        lblStatus.setText("Verificando pacientes asignados al médico...");
        lblStatus.setTextFill(Color.web("#aaaaaa"));

        final String doctorId   = selectedUser.getUid();
        final String doctorName = selectedUser.getFirstName() + " " + selectedUser.getLastName();

        new Thread(() -> {
            try {
                // Consultamos los pacientes del médico y los demás médicos disponibles
                List<User> linkedPatients = getPatientsByDoctorId(doctorId);
                List<User> otherDoctors   = userDao.getByField("role", "doctor");
                // Quitamos al médico que se va a eliminar de la lista de opciones
                otherDoctors.removeIf(d -> doctorId.equals(d.getUid()));

                Platform.runLater(() -> {
                    if (linkedPatients.isEmpty()) {
                        // Sin pacientes: confirmación simple
                        showSimpleDoctorDeleteDialog(doctorName, doctorId);
                    } else {
                        // Con pacientes: reasignación obligatoria antes de eliminar
                        showReassignmentDialog(doctorName, doctorId, linkedPatients, otherDoctors);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al verificar pacientes del médico.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Diálogo de reasignación: el admin elige el médico receptor antes de confirmar eliminación
    private void showReassignmentDialog(String doctorName, String doctorId,
                                        List<User> patients, List<User> otherDoctors) {
        // Si no hay otro médico disponible, bloqueamos la eliminación
        if (otherDoctors.isEmpty()) {
            Alert blocked = new Alert(Alert.AlertType.WARNING);
            blocked.setTitle("Eliminación bloqueada");
            blocked.setHeaderText("El Dr. " + doctorName + " tiene " + patients.size() + " paciente(s) asignado(s)");
            blocked.setContentText("No hay otros médicos disponibles para recibir los pacientes.\n"
                    + "Registra al menos un médico más antes de intentar esta eliminación.");
            DialogUtils.applyWhiteStyle(blocked.getDialogPane());
            blocked.showAndWait();
            lblStatus.setText("Eliminación cancelada: no hay médicos disponibles para reasignación.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        // ComboBox con los médicos que pueden recibir los pacientes
        ComboBox<User> comboNuevoDoc = new ComboBox<>();
        comboNuevoDoc.setMaxWidth(Double.MAX_VALUE);
        comboNuevoDoc.setItems(FXCollections.observableArrayList(otherDoctors));
        comboNuevoDoc.setCellFactory(lv -> new ListCell<User>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : "Dr. " + u.getFirstName() + " " + u.getLastName());
            }
        });
        comboNuevoDoc.setButtonCell(new ListCell<User>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : "Dr. " + u.getFirstName() + " " + u.getLastName());
            }
        });
        comboNuevoDoc.getSelectionModel().selectFirst();

        Label lblInfo = new Label("El Dr. " + doctorName + " tiene "
                + patients.size() + " paciente(s). Selecciona el médico que los recibirá:");
        lblInfo.setStyle("-fx-text-fill: #222222; -fx-font-weight: bold;");
        lblInfo.setWrapText(true);

        VBox content = new VBox(10, lblInfo, comboNuevoDoc);
        content.setStyle("-fx-padding: 10;");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reasignación Obligatoria");
        dialog.setHeaderText("Reasignar pacientes antes de eliminar al médico");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogUtils.applyWhiteStyle(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            User nuevoMedico = comboNuevoDoc.getValue();
            if (nuevoMedico == null) return;

            lblStatus.setText("Reasignando pacientes y eliminando médico...");
            lblStatus.setTextFill(Color.web("#ffffff"));

            new Thread(() -> {
                try {
                    // Recopilamos los IDs de los pacientes que se reasignarán
                    List<String> patientIds = new ArrayList<>();
                    for (User p : patients) patientIds.add(p.getUid());

                    // Preparamos los campos a actualizar en cada paciente
                    Map<String, Object> campos = new HashMap<>();
                    campos.put("assignedDoctorId",   nuevoMedico.getUid());
                    campos.put("assignedDoctorName",
                            nuevoMedico.getFirstName() + " " + nuevoMedico.getLastName());

                    // Batch Update atómico: todos los pacientes se actualizan en una sola operación
                    userDao.batchUpdateFields(patientIds, campos);

                    // Solo después de confirmar el batch, eliminamos al médico
                    userDao.delete(doctorId);

                    Platform.runLater(() -> {
                        // Actualizar la lista local sin recargar todo desde Firestore
                        for (User p : patients) {
                            p.setAssignedDoctorId(nuevoMedico.getUid());
                            p.setAssignedDoctorName(
                                    nuevoMedico.getFirstName() + " " + nuevoMedico.getLastName());
                        }
                        usersObservableList.removeIf(u -> doctorId.equals(u.getUid()));
                        tableUsers.refresh();
                        selectedUser = null;
                        tableUsers.getSelectionModel().clearSelection();
                        refreshStats();
                        applyFilter();
                        lblStatus.setText(patients.size() + " paciente(s) reasignado(s) al Dr. "
                                + nuevoMedico.getLastName() + ". Médico eliminado correctamente.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error durante la reasignación o eliminación. Intenta de nuevo.");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // Confirmación simple cuando el médico no tiene pacientes — solo pide confirmar
    private void showSimpleDoctorDeleteDialog(String doctorName, String doctorId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar al Dr. " + doctorName);
        confirm.setContentText("Este médico no tiene pacientes asignados.\n¿Confirmas la eliminación?");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    userDao.delete(doctorId);
                    Platform.runLater(() -> {
                        // Eliminar al médico de la lista local sin recargar Firestore
                        usersObservableList.removeIf(u -> doctorId.equals(u.getUid()));
                        tableUsers.refresh();
                        selectedUser = null;
                        tableUsers.getSelectionModel().clearSelection();
                        refreshStats();
                        applyFilter();
                        lblStatus.setText("Médico eliminado correctamente.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al eliminar el médico.");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // Confirmación simple para pacientes y admins — sin lógica de reasignación
    private void handleNonDoctorDeletion() {
        String userName = selectedUser.getFirstName() + " " + selectedUser.getLastName();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar usuario");
        confirm.setContentText("¿Eliminar al usuario " + userName + "?\n\nEsta acción no se puede deshacer.");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            String userId = selectedUser.getUid();
            lblStatus.setText("Eliminando...");
            lblStatus.setTextFill(Color.web("#ffffff"));
            new Thread(() -> {
                try {
                    userDao.delete(userId);
                    Platform.runLater(() -> {
                        // Eliminar al usuario de la lista local sin recargar Firestore
                        usersObservableList.removeIf(u -> userId.equals(u.getUid()));
                        tableUsers.refresh();
                        selectedUser = null;
                        tableUsers.getSelectionModel().clearSelection();
                        refreshStats();
                        applyFilter();
                        lblStatus.setText("Usuario eliminado correctamente.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al eliminar el usuario.");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
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
        DialogUtils.applyWhiteStyle(dialog.getDialogPane());

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
                        // Actualizar el rol en la lista local sin recargar Firestore
                        tableUsers.refresh();
                        refreshStats();
                        applyFilter();
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

    // Asigna un médico a un paciente seleccionado desde la tabla
    @FXML
    protected void onAssignDoctor() {
        if (selectedUser == null || !"patient".equals(selectedUser.getRole())) {
            lblStatus.setText("Selecciona un paciente de la tabla para asignar un médico.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        // Cargamos la lista de médicos en un hilo de fondo antes de mostrar el diálogo
        lblStatus.setText("Cargando médicos disponibles...");
        lblStatus.setTextFill(Color.web("#aaaaaa"));

        new Thread(() -> {
            try {
                List<User> doctors = userDao.getByField("role", "doctor");

                Platform.runLater(() -> {
                    if (doctors.isEmpty()) {
                        lblStatus.setText("No hay médicos registrados en el sistema.");
                        lblStatus.setTextFill(Color.web("#ff9800"));
                        return;
                    }

                    // ComboBox con todos los médicos disponibles
                    ComboBox<User> comboDoc = new ComboBox<>();
                    comboDoc.setMaxWidth(Double.MAX_VALUE);
                    comboDoc.setItems(FXCollections.observableArrayList(doctors));

                    // Muestra el nombre completo de cada médico en la lista
                    comboDoc.setCellFactory(lv -> new ListCell<User>() {
                        @Override protected void updateItem(User u, boolean empty) {
                            super.updateItem(u, empty);
                            setText(empty || u == null ? null : "Dr. " + u.getFirstName() + " " + u.getLastName());
                        }
                    });
                    comboDoc.setButtonCell(new ListCell<User>() {
                        @Override protected void updateItem(User u, boolean empty) {
                            super.updateItem(u, empty);
                            setText(empty || u == null ? null : "Dr. " + u.getFirstName() + " " + u.getLastName());
                        }
                    });

                    // Pre-seleccionamos al médico actual del paciente si ya tiene uno
                    if (selectedUser.getAssignedDoctorId() != null) {
                        for (User d : doctors) {
                            if (selectedUser.getAssignedDoctorId().equals(d.getUid())) {
                                comboDoc.setValue(d);
                                break;
                            }
                        }
                    }

                    VBox content = new VBox(10,
                        new Label("Paciente: " + selectedUser.getFirstName() + " " + selectedUser.getLastName()),
                        new Label("Selecciona el médico a asignar:"),
                        comboDoc
                    );
                    content.setStyle("-fx-padding: 10;");
                    content.getChildren().get(0).setStyle("-fx-text-fill: #222222; -fx-font-weight: bold;");
                    content.getChildren().get(1).setStyle("-fx-text-fill: #444444;");

                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Asignar Médico");
                    dialog.setHeaderText("Asignación de médico al paciente");
                    dialog.getDialogPane().setContent(content);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    DialogUtils.applyWhiteStyle(dialog.getDialogPane());

                    dialog.showAndWait().ifPresent(btn -> {
                        if (btn != ButtonType.OK) return;

                        User chosenDoctor = comboDoc.getValue();
                        if (chosenDoctor == null) return;

                        // Actualizamos el paciente en Firestore con el nuevo médico
                        selectedUser.setAssignedDoctorId(chosenDoctor.getUid());
                        selectedUser.setAssignedDoctorName(chosenDoctor.getFirstName() + " " + chosenDoctor.getLastName());

                        new Thread(() -> {
                            try {
                                userDao.save(selectedUser.getUid(), selectedUser);
                                Platform.runLater(() -> {
                                    // Actualizar el nombre del médico en la lista local sin recargar Firestore
                                    tableUsers.refresh();
                                    applyFilter();
                                    lblStatus.setText("Médico asignado correctamente: Dr. " + chosenDoctor.getLastName());
                                    lblStatus.setTextFill(Color.web("#4caf50"));
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    lblStatus.setText("Error al guardar la asignación.");
                                    lblStatus.setTextFill(Color.web("#ff5252"));
                                });
                                e.printStackTrace();
                            }
                        }).start();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar médicos.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Obtiene la lista de pacientes asignados a un médico específico.
    // Usa la consulta directa por campo en lugar de cargar todos y filtrar en memoria.
    private List<User> getPatientsByDoctorId(String doctorId) throws Exception {
        return userDao.getByField("assignedDoctorId", doctorId);
    }

    // Recalcula las etiquetas de estadísticas a partir de la lista observable actual,
    // sin hacer ninguna consulta adicional a Firestore.
    private void refreshStats() {
        int totalDoctors  = 0;
        int totalPatients = 0;
        for (User u : usersObservableList) {
            if ("doctor".equals(u.getRole()))  totalDoctors++;
            if ("patient".equals(u.getRole())) totalPatients++;
        }
        lblTotalUsers.setText(String.valueOf(usersObservableList.size()));
        lblTotalDoctors.setText(String.valueOf(totalDoctors));
        lblTotalPatients.setText(String.valueOf(totalPatients));
    }

}
