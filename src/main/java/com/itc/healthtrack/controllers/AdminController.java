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
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// controlador del panel de admin maneja estadisticas de usuarios busqueda eliminacion y cambio de roles
public class AdminController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalDoctors;
    @FXML private Label lblTotalPatients;
    @FXML private Label lblStatus;

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colFirstName;
    @FXML private TableColumn<User, String> colLastName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colAssignedDoctor;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbRoleFilter;

    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private final ObservableList<User> usersObservableList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;

    private User loggedInAdmin;
    private User selectedUser = null;

    // traduce el rol interno al texto que ve el usuario
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

    // arranca el controlador con el admin logeado y carga la lista de usuarios
    public void initData(User admin) {
        this.loggedInAdmin = admin;
        setupSearchControls();
        setupTable();
        loadAllUsers();
    }

    // configura el filtro de roles para buscar rapido por tipo de usuario
    private void setupSearchControls() {
        cbRoleFilter.setItems(FXCollections.observableArrayList("Todos", "Paciente", "Doctor", "Admin"));
        cbRoleFilter.setValue("Todos");
    }

    // configura las columnas y el listener de seleccion de la tabla
    private void setupTable() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        // celda que traduce el rol al texto en espanol
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                } else {
                    setText(translateRole(role));
                }
            }
        });
        colAssignedDoctor.setCellValueFactory(new PropertyValueFactory<>("assignedDoctorName"));

        // lista filtrable para el buscador
        filteredList = new FilteredList<>(usersObservableList, u -> true);
        tableUsers.setItems(filteredList);

        // guardamos la seleccion cuando el admin clickea una fila
        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                lblStatus.setText("Usuario seleccionado: " + newVal.getFirstName() + " " + newVal.getLastName());
                lblStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    // carga todos los usuarios en un hilo de fondo y luego actualiza tabla y estadisticas en el hilo fx tambien arma el mapa id medico nombre para mostrarlo en la columna doctor asignado
    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<User> allUsers = userDao.getAll();

                // mapa de uid del medico a su nombre completo
                Map<String, String> doctorNameMap = new HashMap<>();

                for (User user : allUsers) {
                    if ("doctor".equals(user.getRole())) {
                        doctorNameMap.put(user.getUid(), user.getFirstName() + " " + user.getLastName());
                    }
                }

                // pegamos el nombre del medico a cada paciente
                for (User user : allUsers) {
                    if ("patient".equals(user.getRole()) && user.getAssignedDoctorId() != null) {
                        String doctorName = doctorNameMap.get(user.getAssignedDoctorId());
                        user.setAssignedDoctorName(doctorName != null ? doctorName : "—");
                    } else if ("patient".equals(user.getRole())) {
                        user.setAssignedDoctorName("—");
                    }
                }

                // contamos cuantos hay de cada rol
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
                // copias finales para usarlas dentro del lambda
                final int doctorCount = doctors;
                final int patientCount = patients;

                Platform.runLater(() -> {
                    usersObservableList.clear();
                    usersObservableList.addAll(allUsers);

                    lblTotalUsers.setText(String.valueOf(allUsers.size()));
                    lblTotalDoctors.setText(String.valueOf(doctorCount));
                    lblTotalPatients.setText(String.valueOf(patientCount));

                    applyFilter();
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

    @FXML
    protected void onSearch() {
        applyFilter();
    }

    // aplica los filtros de busqueda y rol a la tabla
    private void applyFilter() {
        String keyword    = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        String roleFilter = cbRoleFilter.getValue();

        // pasamos del rol del combobox al valor de la bd
        String roleValue = getRoleValue(roleFilter);

        filteredList.setPredicate(user -> {
            boolean roleMatch = "Todos".equals(roleFilter) || roleValue.equals(user.getRole());

            // checamos que el texto aparezca en nombre apellido o correo
            boolean textMatch = keyword.isEmpty()
                    || (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(keyword))
                    || (user.getLastName()  != null && user.getLastName().toLowerCase().contains(keyword))
                    || (user.getEmail()     != null && user.getEmail().toLowerCase().contains(keyword));

            return roleMatch && textMatch;
        });

        lblStatus.setText("Mostrando " + filteredList.size() + " de " + usersObservableList.size() + " usuario(s).");
        lblStatus.setTextFill(Color.web("#aaaaaa"));
    }

    @FXML
    protected void onClearSearch() {
        txtSearch.clear();
        cbRoleFilter.setValue("Todos");
        applyFilter();
    }

    // abre un dialogo con todos los datos del usuario seleccionado muestra campos distintos segun el rol
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
        row = addDetailRow(grid, row, "Rol",      translateRole(selectedUser.getRole()));

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
        applyWhiteStyle(dp);

        dialog.showAndWait();
    }

    // agrega una fila label valor al gridpane y regresa el indice de la siguiente fila
    private int addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");

        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.setStyle("-fx-text-fill: #000000;");
        val.setWrapText(true);

        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
        return row + 1;
    }

    // entrada para eliminar el medico tiene flujo distinto al resto
    @FXML
    protected void onDeleteUser() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }
        // el admin no puede borrarse a si mismo
        if (selectedUser.getUid() != null && selectedUser.getUid().equals(loggedInAdmin.getUid())) {
            lblStatus.setText("No puedes eliminar tu propia cuenta de administrador.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        // los medicos requieren reasignar pacientes antes de poder borrarse
        if ("doctor".equals(selectedUser.getRole())) {
            handleDoctorDeletion();
        } else {
            handleNonDoctorDeletion();
        }
    }

    // checa cuantos pacientes tiene el medico y decide el flujo
    private void handleDoctorDeletion() {
        lblStatus.setText("Verificando pacientes asignados al médico...");
        lblStatus.setTextFill(Color.web("#aaaaaa"));

        final String doctorId   = selectedUser.getUid();
        final String doctorName = selectedUser.getFirstName() + " " + selectedUser.getLastName();

        new Thread(() -> {
            try {
                // traemos los pacientes del medico y los otros medicos disponibles
                List<User> linkedPatients = getPatientsByDoctorId(doctorId);
                List<User> otherDoctors   = userDao.getByField("role", "doctor");
                // quitamos al medico que se va a borrar de la lista de opciones
                otherDoctors.removeIf(d -> doctorId.equals(d.getUid()));

                Platform.runLater(() -> {
                    if (linkedPatients.isEmpty()) {
                        // sin pacientes solo pedimos confirmacion
                        showSimpleDoctorDeleteDialog(doctorName, doctorId);
                    } else {
                        // con pacientes hay que reasignar antes de borrar
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

    // dialogo de reasignacion el admin elige al medico receptor antes de confirmar
    private void showReassignmentDialog(String doctorName, String doctorId,
                                        List<User> patients, List<User> otherDoctors) {
        // si no hay otro medico bloqueamos la eliminacion
        if (otherDoctors.isEmpty()) {
            Alert blocked = new Alert(Alert.AlertType.WARNING);
            blocked.setTitle("Eliminación bloqueada");
            blocked.setHeaderText("El Dr. " + doctorName + " tiene " + patients.size() + " paciente(s) asignado(s)");
            blocked.setContentText("No hay otros médicos disponibles para recibir los pacientes.\n"
                    + "Registra al menos un médico más antes de intentar esta eliminación.");
            applyWhiteStyle(blocked.getDialogPane());
            blocked.showAndWait();
            lblStatus.setText("Eliminación cancelada: no hay médicos disponibles para reasignación.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        // combobox con los medicos que pueden recibir los pacientes
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
        applyWhiteStyle(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            User nuevoMedico = comboNuevoDoc.getValue();
            if (nuevoMedico == null) return;

            lblStatus.setText("Reasignando pacientes y eliminando médico...");
            lblStatus.setTextFill(Color.web("#ffffff"));

            new Thread(() -> {
                try {
                    // juntamos los ids de los pacientes que se van a reasignar
                    List<String> patientIds = new ArrayList<>();
                    for (User p : patients) patientIds.add(p.getUid());

                    // campos que vamos a actualizar en cada paciente
                    Map<String, Object> campos = new HashMap<>();
                    campos.put("assignedDoctorId",   nuevoMedico.getUid());
                    campos.put("assignedDoctorName",
                            nuevoMedico.getFirstName() + " " + nuevoMedico.getLastName());

                    // batch update atomico todos los pacientes se actualizan en una sola operacion
                    userDao.batchUpdateFields(patientIds, campos);

                    // solo despues de confirmar el batch borramos al medico
                    userDao.delete(doctorId);

                    Platform.runLater(() -> {
                        // refrescamos la lista local sin recargar todo de firestore
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

    // confirmacion simple cuando el medico no tiene pacientes solo pide confirmar
    private void showSimpleDoctorDeleteDialog(String doctorName, String doctorId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar al Dr. " + doctorName);
        confirm.setContentText("Este médico no tiene pacientes asignados.\n¿Confirmas la eliminación?");
        applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    userDao.delete(doctorId);
                    Platform.runLater(() -> {
                        // quitamos al medico de la lista local sin recargar firestore
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

    // confirmacion simple para pacientes y admins sin reasignacion
    private void handleNonDoctorDeletion() {
        String userName = selectedUser.getFirstName() + " " + selectedUser.getLastName();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar usuario");
        confirm.setContentText("¿Eliminar al usuario " + userName + "?\n\nEsta acción no se puede deshacer.");
        applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            String userId = selectedUser.getUid();
            lblStatus.setText("Eliminando...");
            lblStatus.setTextFill(Color.web("#ffffff"));
            new Thread(() -> {
                try {
                    userDao.delete(userId);
                    Platform.runLater(() -> {
                        // quitamos al usuario de la lista local sin recargar firestore
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

    // cambia el rol de un usuario y refresca la lista
    @FXML
    protected void onChangeRole() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario primero");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        // dialogo con los roles disponibles en espanol
        ChoiceDialog<String> dialog = new ChoiceDialog<>(translateRole(selectedUser.getRole()),
                "Paciente", "Doctor", "Admin");
        dialog.setTitle("Cambiar Rol");
        dialog.setHeaderText("Usuario: " + selectedUser.getFirstName() + " " + selectedUser.getLastName());
        dialog.setContentText("Selecciona el nuevo rol:");
        applyWhiteStyle(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(newRoleLabel -> {
            String newRole = getRoleValue(newRoleLabel);
            // si eligio el mismo rol no hacemos nada
            if (newRole.equals(selectedUser.getRole())) return;

            new Thread(() -> {
                try {
                    selectedUser.setRole(newRole);
                    userDao.save(selectedUser.getUid(), selectedUser);
                    Platform.runLater(() -> {
                        // refrescamos la tabla local sin recargar firestore
                        tableUsers.refresh();
                        refreshStats();
                        applyFilter();
                        lblStatus.setText("Rol actualizado a: " + newRoleLabel);
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

    // asigna un medico al paciente seleccionado en la tabla
    @FXML
    protected void onAssignDoctor() {
        if (selectedUser == null || !"patient".equals(selectedUser.getRole())) {
            lblStatus.setText("Selecciona un paciente de la tabla para asignar un médico.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        // cargamos los medicos en un hilo de fondo antes de abrir el dialogo
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

                    // combobox con todos los medicos disponibles
                    ComboBox<User> comboDoc = new ComboBox<>();
                    comboDoc.setMaxWidth(Double.MAX_VALUE);
                    comboDoc.setItems(FXCollections.observableArrayList(doctors));

                    // muestra el nombre completo de cada medico en la lista
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

                    // preseleccionamos al medico actual del paciente si ya tiene uno
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
                    applyWhiteStyle(dialog.getDialogPane());

                    dialog.showAndWait().ifPresent(btn -> {
                        if (btn != ButtonType.OK) return;

                        User chosenDoctor = comboDoc.getValue();
                        if (chosenDoctor == null) return;

                        // actualizamos al paciente en firestore con el nuevo medico
                        selectedUser.setAssignedDoctorId(chosenDoctor.getUid());
                        selectedUser.setAssignedDoctorName(chosenDoctor.getFirstName() + " " + chosenDoctor.getLastName());

                        new Thread(() -> {
                            try {
                                userDao.save(selectedUser.getUid(), selectedUser);
                                Platform.runLater(() -> {
                                    // refrescamos el nombre del medico en la lista local sin recargar firestore
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

    // trae los pacientes asignados a un medico consultando directo por campo en vez de filtrar en memoria
    private List<User> getPatientsByDoctorId(String doctorId) throws Exception {
        return userDao.getByField("assignedDoctorId", doctorId);
    }

    // recalcula los contadores a partir de la lista local sin pegarle a firestore
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
