package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.UserDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Controller for the administrator panel.
 * Displays system-wide user statistics and allows the admin
 * to view and remove any registered user.
 */
public class AdminController {

    // Statistics labels
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalDoctors;
    @FXML private Label lblTotalPatients;
    @FXML private Label lblStatus;

    // User table and its columns
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colFirstName;
    @FXML private TableColumn<User, String> colLastName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> usersObservableList = FXCollections.observableArrayList();

    private User loggedInAdmin;
    private User selectedUser = null;

    /**
     * Receives the logged-in admin user and bootstraps the view.
     */
    public void initData(User admin) {
        this.loggedInAdmin = admin;
        setupTable();
        loadAllUsers();
    }

    /**
     * Wires up the TableView columns and the row-selection listener.
     */
    private void setupTable() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        tableUsers.setItems(usersObservableList);

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                lblStatus.setText("Usuario seleccionado: " + newVal.getFirstName() + " " + newVal.getLastName());
                lblStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    /**
     * Fetches all users from Firestore in a background thread and
     * updates the table and statistics labels on the FX thread.
     */
    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<User> allUsers = userDAO.getAllUsers();

                long doctors  = allUsers.stream().filter(u -> "doctor".equals(u.getRole())).count();
                long patients = allUsers.stream().filter(u -> "patient".equals(u.getRole())).count();

                Platform.runLater(() -> {
                    usersObservableList.clear();
                    usersObservableList.addAll(allUsers);
                    lblTotalUsers.setText(String.valueOf(allUsers.size()));
                    lblTotalDoctors.setText(String.valueOf(doctors));
                    lblTotalPatients.setText(String.valueOf(patients));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar la lista de usuarios.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Deletes the currently selected user from Firestore.
     * Prevents the admin from deleting their own account.
     */
    @FXML
    protected void onDeleteUser() {
        if (selectedUser == null) {
            lblStatus.setText("Selecciona un usuario de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        if (selectedUser.getUid() != null && selectedUser.getUid().equals(loggedInAdmin.getUid())) {
            lblStatus.setText("No puedes eliminar tu propia cuenta de administrador.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        String userId = selectedUser.getUid();
        String userName = selectedUser.getFirstName() + " " + selectedUser.getLastName();
        lblStatus.setText("Eliminando usuario: " + userName + "...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                userDAO.deleteUser(userId);
                Platform.runLater(() -> {
                    selectedUser = null;
                    tableUsers.getSelectionModel().clearSelection();
                    loadAllUsers();
                    lblStatus.setText("Usuario eliminado correctamente.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al eliminar el usuario. Intenta de nuevo.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Reloads the full user list from Firestore.
     */
    @FXML
    protected void onRefresh() {
        selectedUser = null;
        tableUsers.getSelectionModel().clearSelection();
        lblStatus.setText("Actualizando lista...");
        lblStatus.setTextFill(Color.web("#ffffff"));
        loadAllUsers();
    }
}
