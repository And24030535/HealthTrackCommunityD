package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.MetricDAO;
import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class RecommendationsController {

    @FXML private ComboBox<User> comboPatients;
    @FXML private TextArea txtRecommendations;
    @FXML private TextArea txtWebService;

    private final PatientDAO patientDAO = new PatientDAO();
    private final MetricDAO metricDAO = new MetricDAO();
    private User loggedInDoctor;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        loadPatients();
    }

    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> patients = patientDAO.getPatientsByDoctor(loggedInDoctor.getUid());
                Platform.runLater(() -> comboPatients.setItems(FXCollections.observableArrayList(patients)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onAnalyzePatient() {
        User selected = comboPatients.getValue();
        if (selected == null) return;

        txtRecommendations.setText("Analizando datos del paciente en xxxxxxxxx...");

        new Thread(() -> {
            try {
                List<Metric> history = metricDAO.getMetricsByPatient(selected.getUid());
                String analysis = generateAlgorithmicRecommendations(history);
                fetchExternalMedicalData();

                Platform.runLater(() -> txtRecommendations.setText(analysis));
            } catch (Exception e) {
                Platform.runLater(() -> txtRecommendations.setText("Error al procesar el análisis."));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Motor de reglas clinicas basicas para emitir recomendaciones.
     */
    private String generateAlgorithmicRecommendations(List<Metric> history) {
        if (history == null || history.isEmpty()) return "No hay registros clínicos suficientes para generar un análisis.";

        Metric latest = history.get(0);
        StringBuilder report = new StringBuilder();
        report.append("Última evaluación registrada: ").append(latest.getTimestamp().toDate().toString()).append("\n\n");

        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            report.append("■ PRESIÓN ARTERIAL (").append(sys).append("/").append(dia).append(" mmHg):\n");
            if (sys < 120 && dia < 80) report.append("  - Estado: Óptimo. Mantener estilo de vida actual.\n");
            else if (sys >= 140 || dia >= 90) report.append("  - ALERTA: Hipertensión detectada. Se recomienda monitoreo estricto y ajuste farmacológico.\n");
            else report.append("  - Estado: Prehipertensión. Sugerir reducción de sodio en la dieta.\n");
        }

        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            report.append("\n■ GLUCOSA (").append(gluc).append(" mg/dL):\n");
            if (gluc > 125) report.append("  - ALERTA: Posible estado diabético. Requerir prueba de HbA1c.\n");
            else if (gluc >= 100) report.append("  - Estado: Pre-diabetes. Iniciar protocolo nutricional.\n");
            else report.append("  - Estado: Normal.\n");
        }

        return report.toString();
    }

    /**
     * Consumo asincrono de una API REST externa.
     */
    private void fetchExternalMedicalData() {
        Platform.runLater(() -> txtWebService.setText("Conectando con servicio web externo..."));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fda.gov/drug/label.json?limit=1"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    String snippet = responseBody.length() > 500 ? responseBody.substring(0, 500) + "...\n[Respuesta truncada]" : responseBody;
                    Platform.runLater(() -> txtWebService.setText("Datos obtenidos exitosamente de openFDA:\n\n" + snippet));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> txtWebService.setText("Fallo en la conexión al Web Service externo.\n" + e.getMessage()));
                    return null;
                });
    }
}