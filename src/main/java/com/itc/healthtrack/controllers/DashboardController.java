package com.itc.healthtrack.controllers;

import com.itc.healthtrack.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

/*Controlador principal que gestiona el menú lateral y el área de contenido dinámico.
 Adapta los módulos disponibles según el rol del usuario logeado*/
public class DashboardController {

    // Elementos deinterfaz
    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;          // Etiqueta con el rol del usuario
    @FXML private VBox contentArea;         // Área central donde se cargan los módulos
    @FXML private Button btnPatientsList;   // Botón para ir a la lista de pacientes
    @FXML private Button btnAdminPanel;     // Botón para ir al panel administrador

    //Datos
    private User loggedInUser;              // Usuario actualmente logeado

    /*Inicializa el panel con la información del usuario logeado.
     Ajusta la interfaz según el rol del usuario */
    public void initData(User user) {
        this.loggedInUser = user;
        System.out.println("ROL DEL USUARIO: " + user.getRole());

        // Muestra el prefijo correcto según el rol
        String role = user.getRole() != null ? user.getRole() : "patient";
        switch (role) {
            case "doctor":
                userNameLabel.setText("Dr. " + user.getLastName());
                roleLabel.setText("Médico");
                break;
            case "admin":
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Administrador");
                break;
            default:
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Paciente");
                break;
        }

        if ("patient".equals(role)) {
            btnPatientsList.setVisible(false);
            btnPatientsList.setManaged(false);
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            onShowMetrics();
        } else if ("admin".equals(role)) {
            onShowAdmin();    // Admins van al panel de administración
        } else {
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            onShowPatientsList();  // Médicos ven su lista de pacientes
        }
    }

    //Carga el módulo de lista de pacientes en el área central
    @FXML
    protected void onShowPatientsList() {
        changeModule("/com/itc/healthtrack/views/patients-view.fxml", "patients");
    }

    //Carga el módulo del panel de administración en el área central.
    @FXML
    protected void onShowAdmin() {
        changeModule("/com/itc/healthtrack/views/admin-view.fxml", "admin");
    }

    //Carga el módulo de métricas
    @FXML
    protected void onShowMetrics() {
        changeModule("/com/itc/healthtrack/views/metrics-view.fxml", "metrics");
    }

    //Carga el módulo de reportes

    @FXML
    protected void onShowReports() {
        changeModule("/com/itc/healthtrack/views/reports-view.fxml", "reports");
    }

    //Carga el módulo de recomendaciones
    @FXML
    protected void onShowRecommendations() {
        changeModule("/com/itc/healthtrack/views/recommendations-view.fxml", "recommendations");
    }

    /*Metodo generico para cargar vistas FXML
     Instancia el controlador correspondiente y pasa los datos del usuario.*/
    private void changeModule(String fxmlPath, String moduleType) {
        try {
            // Carga el archivo FXML desde los recursos
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            // Obtiene el controlador y le pasa los datos del usuario logeado
            switch (moduleType) {
                case "admin":
                    AdminController ac = loader.getController();
                    ac.initData(loggedInUser);
                    break;
                case "patients":
                    PatientsController pc = loader.getController();
                    pc.initData(loggedInUser);
                    break;
                case "metrics":
                    MetricsController mc = loader.getController();
                    mc.initData(loggedInUser);
                    break;
                case "reports":
                    ReportsController rc = loader.getController();
                    rc.initData(loggedInUser);
                    break;
                case "recommendations":
                    RecommendationsController rcc = loader.getController();
                    rcc.initData(loggedInUser);
                    break;
            }

            // Reemplaza el contenido anterior por el nuevo módulo
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar el módulo: " + fxmlPath);
        }
    }

    /* Cierra la sesión del usuario y vuelve a la pantalla de inicio de sesión.
     Restaura los estilos CSS para que la pantalla de login sea visible*/
    @FXML
    protected void onLogout(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene loginScene = new Scene(fxmlLoader.load(), 800, 600);

            loginScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            loginScene.getStylesheets().add(cssPath);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}