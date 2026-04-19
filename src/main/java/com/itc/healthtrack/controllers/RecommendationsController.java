package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.MetricDAO;
import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.NotificationService;
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
    private final NotificationService notificationService = new NotificationService();
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
                Platform.runLater(() -> txtRecommendations.setText("Error al cargar la lista de pacientes."));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onAnalyzePatient() {
        User selected = comboPatients.getValue();
        if (selected == null) return;

        txtRecommendations.setText("Analizando datos del paciente...");
        txtWebService.setText("Consultando servicios externos...");

        new Thread(() -> {
            try {
                List<Metric> history = metricDAO.getMetricsByPatient(selected.getUid());
                String weatherData = fetchWeatherData();
                String analysis = generateAlgorithmicRecommendations(history, weatherData);

                // Check for risk progression and send notification if needed
                if (hasRiskProgression(history)) {
                    notificationService.notifyPatient(selected,
                            "Análisis de tendencias detectó una progresión de riesgo en tus métricas. Consulta a tu médico.");
                    notificationService.notifyDoctor(loggedInDoctor,
                            "ALERTA DE TENDENCIA: El paciente " + selected.getFirstName()
                                    + " " + selected.getLastName()
                                    + " presenta una progresión de riesgo en sus métricas recientes.");
                }

                fetchExternalMedicalData();

                Platform.runLater(() -> txtRecommendations.setText(analysis));
            } catch (Exception e) {
                Platform.runLater(() -> txtRecommendations.setText("Error al procesar el análisis."));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Fetches current weather conditions from the public wttr.in API.
     *
     * @return A short weather description string, or "unknown" on failure.
     */
    private String fetchWeatherData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wttr.in/?format=%C+%t+%h"))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Evaluates recent metrics for a worsening trend across consecutive readings.
     *
     * @param history Ordered list of patient metrics (newest first).
     * @return true if a risk progression pattern is detected.
     */
    private boolean hasRiskProgression(List<Metric> history) {
        if (history == null || history.size() < 3) return false;

        int hypertensiveCount = 0;
        int hyperglycemicCount = 0;

        for (int i = 0; i < Math.min(3, history.size()); i++) {
            Metric m = history.get(i);
            if (m.getSystolic() != null && m.getSystolic() >= 140) hypertensiveCount++;
            if (m.getGlucoseLevel() != null && m.getGlucoseLevel() > 125) hyperglycemicCount++;
        }

        return hypertensiveCount >= 3 || hyperglycemicCount >= 3;
    }

    /**
     * Rule-based recommendation engine that correlates patient metrics with
     * weather conditions and nutrition guidelines.
     *
     * @param history     Ordered patient metric history (newest first).
     * @param weatherData Current weather description from the external API.
     * @return A formatted clinical recommendation report.
     */
    private String generateAlgorithmicRecommendations(List<Metric> history, String weatherData) {
        if (history == null || history.isEmpty()) {
            return "No hay registros clínicos suficientes para generar un análisis.";
        }

        Metric latest = history.get(0);
        StringBuilder report = new StringBuilder();
        report.append("=== ANÁLISIS CLÍNICO — HealthTrack ===\n");
        report.append("Última evaluación: ").append(latest.getTimestamp().toDate().toString()).append("\n");
        report.append("Condición climática actual: ").append(weatherData).append("\n\n");

        boolean isRainy = weatherData.toLowerCase().contains("rain")
                || weatherData.toLowerCase().contains("drizzle")
                || weatherData.toLowerCase().contains("lluv");
        boolean isCold = weatherData.toLowerCase().contains("snow")
                || weatherData.toLowerCase().contains("cold")
                || weatherData.toLowerCase().contains("frio");

        // Blood pressure rules
        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            report.append("■ PRESIÓN ARTERIAL (").append(sys).append("/").append(dia).append(" mmHg):\n");
            if (sys < 120 && dia < 80) {
                report.append("  - Estado: Óptimo. Mantener estilo de vida actual.\n");
                if (isRainy) {
                    report.append("  - Clima lluvioso: Ideal para yoga o estiramientos en interiores.\n");
                }
            } else if (sys >= 180 || dia >= 120) {
                report.append("  - ALERTA CRÍTICA: Hipertensión en crisis. Atención médica urgente.\n");
                report.append("  - Nutrición: Dieta DASH, restringir sodio a <1500 mg/día.\n");
                report.append("  - Actividad: Reposo hasta evaluación médica.\n");
            } else if (sys >= 140 || dia >= 90) {
                report.append("  - ALERTA: Hipertensión. Monitoreo estricto y ajuste farmacológico.\n");
                report.append("  - Nutrición: Reducir sodio (<2000 mg/día), aumentar potasio.\n");
                if (isRainy) {
                    report.append("  - Clima lluvioso + PA elevada: Ejercicio en interiores de baja intensidad (caminata lenta, stretching).\n");
                } else if (isCold) {
                    report.append("  - Clima frío + PA elevada: Evitar exposición al frío. Actividad física cubierta.\n");
                } else {
                    report.append("  - Actividad: Caminata ligera 30 min/día al aire libre.\n");
                }
            } else {
                report.append("  - Estado: Prehipertensión. Reducir sodio y aumentar actividad física.\n");
                if (isRainy) {
                    report.append("  - Clima lluvioso: Rutina de ejercicio en casa (30 min de cardio suave).\n");
                }
            }
            report.append("\n");
        }

        // Glucose rules
        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            report.append("■ GLUCOSA (").append(gluc).append(" mg/dL):\n");
            if (gluc > 300) {
                report.append("  - ALERTA CRÍTICA: Glucosa muy elevada. Riesgo de cetoacidosis diabética.\n");
                report.append("  - Acción: Atención médica inmediata. No realizar ejercicio físico.\n");
            } else if (gluc > 125) {
                report.append("  - ALERTA: Posible estado diabético. Requerir prueba de HbA1c.\n");
                report.append("  - Nutrición: Dieta baja en carbohidratos simples, priorizar fibra.\n");
                if (isRainy) {
                    report.append("  - Clima lluvioso + glucosa alta: Actividad física en interiores (banda caminadora, bicicleta estática).\n");
                } else {
                    report.append("  - Actividad: Caminata 45 min post-comida para mejorar sensibilidad a insulina.\n");
                }
            } else if (gluc >= 100) {
                report.append("  - Estado: Pre-diabetes. Iniciar protocolo nutricional.\n");
                report.append("  - Nutrición: Limitar azúcares añadidos, incrementar verduras no almidonadas.\n");
            } else if (gluc < 70) {
                report.append("  - ALERTA: Hipoglucemia. Ingerir 15g de glucosa de rápida absorción.\n");
            } else {
                report.append("  - Estado: Normal.\n");
            }
            report.append("\n");
        }

        // Heart rate rules
        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            report.append("■ FRECUENCIA CARDÍACA (").append(hr).append(" lpm):\n");
            if (hr > 120) {
                report.append("  - ALERTA: Taquicardia. Evitar ejercicio intenso y cafeína.\n");
            } else if (hr > 100) {
                report.append("  - Frecuencia elevada. Revisar factores de estrés y consumo de estimulantes.\n");
            } else if (hr < 50) {
                report.append("  - ALERTA: Bradicardia. Evaluación cardiológica recomendada.\n");
            } else {
                report.append("  - Estado: Normal (60-100 lpm).\n");
            }
            report.append("\n");
        }

        // BMI rules
        if (latest.getBmi() != null) {
            double bmi = latest.getBmi();
            report.append("■ ÍNDICE DE MASA CORPORAL (IMC: ").append(bmi).append("):\n");
            if (bmi >= 40) {
                report.append("  - Obesidad mórbida (Clase III). Plan multidisciplinario urgente.\n");
            } else if (bmi >= 35) {
                report.append("  - Obesidad Clase II. Intervención nutricional y médica requerida.\n");
            } else if (bmi >= 30) {
                report.append("  - Obesidad Clase I. Plan nutricional supervisado recomendado.\n");
                if (isRainy) {
                    report.append("  - Clima lluvioso: Actividades en interiores de bajo impacto (natación cubierta, yoga).\n");
                }
            } else if (bmi >= 25) {
                report.append("  - Sobrepeso. Incrementar actividad física aeróbica 150 min/semana.\n");
            } else if (bmi >= 18.5) {
                report.append("  - Peso normal. Mantener hábitos actuales.\n");
            } else {
                report.append("  - Bajo peso. Evaluación nutricional recomendada.\n");
            }
            report.append("\n");
        }

        report.append("--- Generado por HealthTrack Community ---");
        return report.toString();
    }

    /**
     * Asynchronously fetches drug label data from the public openFDA API
     * as a demonstration of external web service integration.
     */
    private void fetchExternalMedicalData() {
        Platform.runLater(() -> txtWebService.setText("Conectando con servicio openFDA..."));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fda.gov/drug/label.json?limit=1"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    String snippet = responseBody.length() > 500
                            ? responseBody.substring(0, 500) + "...\n[Respuesta truncada]"
                            : responseBody;
                    Platform.runLater(() -> txtWebService.setText(
                            "Datos obtenidos exitosamente de openFDA:\n\n" + snippet));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> txtWebService.setText(
                            "Fallo en la conexión al Web Service externo.\n" + e.getMessage()));
                    return null;
                });
    }
}
