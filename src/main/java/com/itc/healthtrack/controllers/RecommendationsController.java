package com.itc.healthtrack.controllers;

import com.google.cloud.Timestamp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.Recommendation;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.NotificationService;
import com.itc.healthtrack.services.UserService;
import com.itc.healthtrack.utils.AlertUtils;
import com.itc.healthtrack.utils.MetricUtils;
import com.itc.healthtrack.utils.RecommendationUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Controlador para la gestión de recomendaciones clínicas
// Usa exclusivamente GenericDAO<T> para acceder a Firestore
// no depende de PatientDAO, MetricDAO ni RecommendationDAO
public class RecommendationsController {

    // Elementos de interfaz
    @FXML private ComboBox<User>           comboPatients;       // Selector de paciente
    @FXML private TextArea                 txtRecommendations;  // Análisis clínico generado
    @FXML private TextArea                 txtWebService;       // Datos FDA
    @FXML private TextArea                 txtNutrition;        // Datos USDA
    @FXML private ListView<Recommendation> listHistory;         // Historial de análisis guardados

    // Sección de notas / recomendaciones médicas
    @FXML private VBox      notesSection;
    @FXML private VBox      noteWriteSection;         // Solo visible para médicos
    @FXML private TextField txtRecommendationTitle;   // Título de la recomendación (compartido con la nota)
    @FXML private TextArea  txtNoteInput;             // Mensaje de la nota / recomendación
    @FXML private Label     lblRecommendationStatus;  // Resultado del último envío
    @FXML private VBox      vboxNotesList;            // Contenedor dinámico de notas guardadas


    // daos para leer usuarios, metricas y notas de firestore
    private final GenericDAO<User>           userDAO           = new GenericDAO<>(User.class, "users");
    private final GenericDAO<Metric>         metricDAO         = new GenericDAO<>(Metric.class, "metrics");
    private final GenericDAO<Recommendation> recommendationDAO = new GenericDAO<>(Recommendation.class, "notas");

    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private User loggedInDoctor;                          // Usuario médico/admin logeado
    private ObservableList<Recommendation> historyItems; // Lista observable del historial


    // Inicialización

    // Recibe el usuario logeado y configura la vista según su rol:
    //   - Paciente → ve solo sus propios datos, ComboBox deshabilitado
    //   - Médico / Admin → ve la lista de sus pacientes asignados
    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupHistory();

        if ("patient".equals(doctor.getRole())) {
            // El paciente ve sus propios datos — el ComboBox no es necesario
            comboPatients.getItems().add(doctor);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
            // El paciente nunca escribe notas — ocultamos el área de escritura explícitamente
            if (noteWriteSection != null) {
                noteWriteSection.setVisible(false);
                noteWriteSection.setManaged(false);
            }
            String uid = doctor.getUid() != null ? doctor.getUid() : "";
            loadAllRecommendationsForPatient(uid);

        } else {
            loadPatients();
            comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    historyItems.clear();
                    loadAllRecommendationsForPatient(newVal.getUid());

                    // El médico puede leer y escribir notas; el admin solo puede leer
                    boolean isDoctor = "doctor".equals(loggedInDoctor.getRole());
                    showNotesSection();
                    if (noteWriteSection != null) {
                        noteWriteSection.setVisible(isDoctor);
                        noteWriteSection.setManaged(isDoctor);
                    }
                }
            });
        }
    }

    // Configura el ListView de historial con su formato de celda y el listener de selección
    private void setupHistory() {
        historyItems = FXCollections.observableArrayList();
        listHistory.setItems(historyItems);

        // Cada celda muestra: título + fecha truncada al minuto
        listHistory.setCellFactory(lv -> new ListCell<Recommendation>() {
            @Override
            protected void updateItem(Recommendation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    String date = item.getGeneratedAt() != null
                            ? item.getGeneratedAt().toDate().toString().substring(0, 16) : "";
                    setText((item.getTitle() != null ? item.getTitle() : "Análisis") + "\n" + date);
                    setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 12px;");
                }
            }
        });

        // Al seleccionar una entrada del historial, muestra su texto completo en el área de análisis
        listHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getMessage() != null) {
                txtRecommendations.setText(newVal.getMessage());
            }
        });
    }


    // Muestra la sección de notas médicas
    private void showNotesSection() {
        if (notesSection != null) {
            notesSection.setVisible(true);
            notesSection.setManaged(true);
        }
    }

    // Carga de datos

    // Carga la lista de pacientes usando UserService para evitar duplicar la lógica de filtrado
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> visible = userService.getPatientsForUser(loggedInDoctor);
                Platform.runLater(() ->
                        comboPatients.setItems(FXCollections.observableArrayList(visible)));
            } catch (Exception e) {
                Platform.runLater(() ->
                        txtRecommendations.setText("Error al cargar la lista de pacientes."));
                e.printStackTrace();
            }
        }).start();
    }

    // Carga TODAS las recomendaciones del paciente en una sola consulta a Firestore,
    // luego separa en memoria: análisis al historial y notas al panel de notas.
    // Reemplaza las dos llamadas separadas (loadRecommendationHistory + loadDoctorNotes)
    // que antes hacían dos queries idénticas para el mismo paciente.
    private void loadAllRecommendationsForPatient(String patientId) {
        new Thread(() -> {
            try {
                // Una sola consulta a Firestore — luego separamos por tipo en memoria
                List<Recommendation> all = recommendationDAO.getByField("patientId", patientId);

                List<Recommendation> analyses = new ArrayList<>();
                List<Recommendation> notes    = new ArrayList<>();

                for (Recommendation r : all) {
                    // Las notas del médico y las recomendaciones enviadas por email van al panel de notas
                    if ("note".equals(r.getType()) || "doctor_recommendation".equals(r.getType())) {
                        notes.add(r);
                    } else {
                        analyses.add(r);
                    }
                }

                // Ordenamos ambas listas de la más reciente a la más antigua
                RecommendationUtils.sortByDateDesc(analyses);
                RecommendationUtils.sortByDateDesc(notes);

                Platform.runLater(() -> {
                    historyItems.clear();
                    historyItems.addAll(analyses);
                    renderNoteBlocks(notes);
                });

            } catch (Exception e) {
                System.err.println("[RecommendationsController] Error cargando datos del paciente: "
                        + e.getMessage());
            }
        }).start();
    }

    // Dibuja un bloque visual (tarjeta) por cada nota dentro del vboxNotesList.
    // Si no hay notas, muestra un mensaje vacío informativo.
    private void renderNoteBlocks(List<Recommendation> notes) {
        if (vboxNotesList == null) return;
        vboxNotesList.getChildren().clear();

        if (notes.isEmpty()) {
            // Mensaje cuando el médico aún no ha escrito notas para este paciente
            Label empty = new Label("No hay notas registradas para este paciente.");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-padding: 14;");
            vboxNotesList.getChildren().add(empty);
            return;
        }

        for (Recommendation nota : notes) {
            String titulo = nota.getTitle() != null ? nota.getTitle() : "Nota médica";
            String fecha  = nota.getGeneratedAt() != null
                    ? nota.getGeneratedAt().toDate().toString().substring(0, 16) : "";
            String msg    = nota.getMessage() != null ? nota.getMessage() : "";

            // Título de la nota en blanco resaltado
            Label lblTitle = new Label(titulo);
            lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #e8e8e8; -fx-font-size: 13px;");

            // Fecha en azul muted para no competir con el contenido
            Label lblDate = new Label(fecha);
            lblDate.setStyle("-fx-text-fill: #7a9cc8; -fx-font-size: 10px;");

            // Contenido de la nota en gris claro, con salto de línea
            Label lblMsg = new Label(msg);
            lblMsg.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
            lblMsg.setWrapText(true);
            lblMsg.setMaxWidth(Double.MAX_VALUE);

            // Tarjeta individual con borde azul oscuro y fondo profundo
            VBox card = new VBox(4, lblTitle, lblDate, lblMsg);
            card.setStyle("-fx-background-color: #1e2a45;"
                    + " -fx-border-color: #2a5298;"
                    + " -fx-border-radius: 6;"
                    + " -fx-background-radius: 6;"
                    + " -fx-padding: 12 14 12 14;");
            card.setMaxWidth(Double.MAX_VALUE);

            vboxNotesList.getChildren().add(card);
        }
    }

    // Análisis clínico principal

    // Se ejecuta al presionar "Generar Análisis Clínico".
    // 1. Obtiene las métricas del paciente usando GenericDAO<Metric>.
    // 2. Consulta el clima actual desde Open-Meteo.
    // 3. Genera el análisis algorítmico.
    // 4. Envía notificaciones si hay progresión de riesgo.
    // 5. Persiste el análisis en Firestore.
    // 6. Consulta FDA y USDA de forma asíncrona.
    @FXML
    protected void onAnalyzePatient() {
        User selected = comboPatients.getValue();
        if (selected == null) return;

        txtRecommendations.setText("Analizando datos del paciente...");
        txtWebService.setText("Consultando servicios externos...");
        txtNutrition.setText("Consultando USDA FoodData Central...");

        new Thread(() -> {
            try {
                // Obtiene las métricas del paciente desde la colección "metrics"
                // usando GenericDAO.getByField en lugar de MetricDAO
                List<Metric> history = getMetricsByPatient(selected.getUid());

                // Consulta las condiciones climáticas actuales
                String weatherData = fetchWeatherData();

                // Genera el análisis basado en reglas clínicas y clima
                String analysis = generateAlgorithmicRecommendations(history, weatherData);

                // Envía notificaciones si se detecta progresión de riesgo en las últimas 3 lecturas
                if (hasRiskProgression(history)) {
                    notificationService.notifyPatient(selected,
                            "Análisis de tendencias detectó una progresión de riesgo en tus métricas. Consulta a tu médico.");

                    if (loggedInDoctor != null
                            && ("doctor".equals(loggedInDoctor.getRole())
                            || "admin".equals(loggedInDoctor.getRole()))) {
                        // El médico o admin generó el análisis — notificarlo directamente
                        notificationService.notifyDoctor(loggedInDoctor,
                                "ALERTA DE TENDENCIA: El paciente " + selected.getFirstName()
                                        + " " + selected.getLastName()
                                        + " presenta una progresión de riesgo en sus métricas recientes.");
                    } else if (selected.getAssignedDoctorId() != null && !selected.getAssignedDoctorId().isEmpty()) {
                        // El paciente analizó sus propias métricas — buscar y notificar al médico asignado
                        final String alertMsg = "ALERTA DE TENDENCIA: El paciente " + selected.getFirstName()
                                + " " + selected.getLastName()
                                + " presenta una progresión de riesgo en sus métricas recientes.";
                        new Thread(() -> {
                            try {
                                User assignedDoctor = userDAO.getById(selected.getAssignedDoctorId());
                                if (assignedDoctor != null) {
                                    notificationService.notifyDoctor(assignedDoctor, alertMsg);
                                }
                            } catch (Exception e) {
                                System.err.println("[RecommendationsController] Error al notificar al médico asignado: " + e.getMessage());
                            }
                        }).start();
                    }
                }

                // Guarda el análisis en Firestore y recarga el historial
                persistRecommendation(selected.getUid(), analysis);

                // Consultas asíncronas a servicios externos (no bloquean el hilo actual)
                fetchExternalMedicalData();
                fetchNutritionalData(determineFoodQuery(history));

                Platform.runLater(() -> txtRecommendations.setText(analysis));

            } catch (Exception e) {
                Platform.runLater(() -> txtRecommendations.setText("Error al procesar el análisis."));
                e.printStackTrace();
            }
        }).start();
    }

    // Obtención de métricas GenericDAO (reemplaza MetricDAO)
    // Obtiene todas las métricas del paciente indicado y las ordena por fecha descendente.
    // Usa GenericDAO<Metric>.getByField("patientId", patientId) — sin MetricDAO.
    private List<Metric> getMetricsByPatient(String patientId) throws Exception {
        List<Metric> metrics = metricDAO.getByField("patientId", patientId);
        MetricUtils.sortByTimestampDesc(metrics);
        return metrics;
    }

    // Persistencia de recomendación via GenericDAO (reemplaza RecommendationDAO)

    // Guarda el análisis generado en la colección "notas" de Firestore.
    // Usa GenericDAO<Recommendation>.save() en lugar de RecommendationDAO.
    private void persistRecommendation(String patientId, String analysisText) {
        new Thread(() -> {
            try {
                // Construye el objeto Recommendation con todos sus campos
                Recommendation rec = new Recommendation();
                rec.setPatientId(patientId);
                rec.setGeneratedAt(Timestamp.now());
                rec.setType("suggestion");
                rec.setTitle("Análisis Clínico Automático");
                rec.setMessage(analysisText);
                rec.setIsRead(false);

                // Genera un ID nuevo para el documento en Firestore
                String newId = recommendationDAO.createDocumentId();
                rec.setId(newId);

                // Guarda el documento usando el ID generado
                recommendationDAO.save(newId, rec);

                // Recarga el historial para mostrar la nueva entrada (una sola consulta)
                loadAllRecommendationsForPatient(patientId);

            } catch (Exception e) {
                System.err.println("Error guardando recomendación: " + e.getMessage());
            }
        }).start();
    }

    // Lógica clínica (sin cambios respecto al original)

    // Consulta el clima actual desde Open-Meteo (Celaya, Guanajuato)
    private String fetchWeatherData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.open-meteo.com/v1/forecast?latitude=21.0190&longitude=-101.2574&current_weather=true&timezone=America%2FMexico_City"))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject root    = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject current = root.getAsJsonObject("current_weather");
            double temp = current.get("temperature").getAsDouble();
            int    code = current.get("weathercode").getAsInt();
            return weatherCodeToSpanish(code) + " " + temp + "°C";

        } catch (Exception e) {
            System.out.println("ERROR CLIMA: " + e.getMessage());
            return "No disponible";
        }
    }

    // Convierte códigos de clima Open-Meteo a descripciones en español
    private String weatherCodeToSpanish(int code) {
        if (code == 0)  return "Despejado";
        if (code <= 3)  return "Parcialmente nublado";
        if (code <= 48) return "Nublado";
        if (code <= 67) return "Lluvia";
        if (code <= 77) return "Nieve";
        if (code <= 82) return "Chubascos";
        if (code <= 99) return "Tormenta";
        return "Variable";
    }

    // Evalúa si las últimas 3 lecturas consecutivas muestran valores de riesgo persistente
    private boolean hasRiskProgression(List<Metric> history) {
        if (history == null || history.size() < 3) return false;
        int hypertensiveCount  = 0;
        int hyperglycemicCount = 0;
        for (int i = 0; i < Math.min(3, history.size()); i++) {
            Metric m = history.get(i);
            if (m.getSystolic()     != null && m.getSystolic()     >= 140) hypertensiveCount++;
            if (m.getGlucoseLevel() != null && m.getGlucoseLevel()  > 125) hyperglycemicCount++;
        }
        return hypertensiveCount >= 3 || hyperglycemicCount >= 3;
    }

    // Genera el análisis clínico texto basado en reglas: presión, glucosa, FC, IMC + clima
    private String generateAlgorithmicRecommendations(List<Metric> history, String weatherData) {
        if (history == null || history.isEmpty()) {
            return "No hay registros clínicos suficientes para generar un análisis.";
        }

        Metric latest = history.get(0);
        StringBuilder report = new StringBuilder();
        report.append("ANÁLISIS CLÍNICO — HealthTrack\n");
        String evalDate = latest.getTimestamp() != null ? latest.getTimestamp().toDate().toString() : "No disponible";
        report.append("Última evaluación: ").append(evalDate).append("\n");
        report.append("Condición climática actual: ").append(weatherData).append("\n\n");

        String weatherLower = weatherData.toLowerCase();
        boolean isRainy = weatherLower.contains("lluv")  || weatherLower.contains("rain")
                || weatherLower.contains("drizzle");
        boolean isCold  = weatherLower.contains("nieve") || weatherLower.contains("snow")
                || weatherLower.contains("frio");

        // Presión arterial
        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            report.append("+ PRESIÓN ARTERIAL (").append(sys).append("/").append(dia).append(" mmHg):\n");
            if (sys < 120 && dia < 80) {
                report.append("  - Estado: Óptimo. Mantener estilo de vida actual.\n");
                if (isRainy) report.append("  - Clima lluvioso: Ideal para yoga o estiramientos en interiores.\n");
            } else if (sys >= 180 || dia >= 120) {
                report.append("  - ALERTA CRÍTICA: Hipertensión en crisis. Atención médica urgente.\n");
                report.append("  - Nutrición: Dieta DASH, restringir sodio a <1500 mg/día.\n");
                report.append("  - Actividad: Reposo hasta evaluación médica.\n");
            } else if (sys >= 140 || dia >= 90) {
                report.append("  - ALERTA: Hipertensión. Monitoreo estricto y ajuste farmacológico.\n");
                report.append("  - Nutrición: Reducir sodio (<2000 mg/día), aumentar potasio.\n");
                if (isRainy)
                    report.append("  - Clima lluvioso + PA elevada: Ejercicio en interiores de baja intensidad.\n");
                else if (isCold)
                    report.append("  - Clima frío + PA elevada: Evitar exposición al frío.\n");
                else
                    report.append("  - Actividad: Caminata ligera 30 min/día al aire libre.\n");
            } else {
                report.append("  - Estado: Prehipertensión. Reducir sodio y aumentar actividad física.\n");
                if (isRainy) report.append("  - Clima lluvioso: Rutina de ejercicio en casa (30 min de cardio suave).\n");
            }
            report.append("\n");
        }

        // Glucosa
        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            report.append("+ GLUCOSA (").append(gluc).append(" mg/dL):\n");
            if (gluc > 300) {
                report.append("  - ALERTA CRÍTICA: Glucosa muy elevada. Riesgo de cetoacidosis diabética.\n");
                report.append("  - Acción: Atención médica inmediata. No realizar ejercicio físico.\n");
            } else if (gluc > 125) {
                report.append("  - ALERTA: Posible estado diabético. Requerir prueba de HbA1c.\n");
                report.append("  - Nutrición: Dieta baja en carbohidratos simples, priorizar fibra.\n");
                if (isRainy)
                    report.append("  - Clima lluvioso + glucosa alta: Actividad en interiores (bicicleta estática).\n");
                else
                    report.append("  - Actividad: Caminata 45 min post-comida para mejorar sensibilidad a insulina.\n");
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

        // Frecuencia cardíaca
        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            report.append("+ FRECUENCIA CARDÍACA (").append(hr).append(" lpm):\n");
            if      (hr > 120) report.append("  - ALERTA: Taquicardia. Evitar ejercicio intenso y cafeína.\n");
            else if (hr > 100) report.append("  - Frecuencia elevada. Revisar factores de estrés y estimulantes.\n");
            else if (hr < 50)  report.append("  - ALERTA: Bradicardia. Evaluación cardiológica recomendada.\n");
            else               report.append("  - Estado: Normal (60-100 lpm).\n");
            report.append("\n");
        }

        // IMC
        if (latest.getBmi() != null) {
            double bmi = latest.getBmi();
            report.append("+ ÍNDICE DE MASA CORPORAL (IMC: ").append(bmi).append("):\n");
            if      (bmi >= 40) report.append("  - Obesidad mórbida (Clase III). Plan multidisciplinario urgente.\n");
            else if (bmi >= 35) report.append("  - Obesidad Clase II. Intervención nutricional y médica requerida.\n");
            else if (bmi >= 30) {
                report.append("  - Obesidad Clase I. Plan nutricional supervisado recomendado.\n");
                if (isRainy) report.append("  - Clima lluvioso: Actividades en interiores de bajo impacto.\n");
            } else if (bmi >= 25) report.append("  - Sobrepeso. Incrementar actividad física aeróbica 150 min/semana.\n");
            else if (bmi >= 18.5) report.append("  - Peso normal. Mantener hábitos actuales.\n");
            else                  report.append("  - Bajo peso. Evaluación nutricional recomendada.\n");
            report.append("\n");
        }

        report.append("--- Generado por HealthTrack ---");
        return report.toString();
    }

    // Elige la consulta nutricional según la condición clínica dominante del paciente
    private String determineFoodQuery(List<Metric> history) {
        if (history == null || history.isEmpty()) return "mediterranean diet healthy foods";
        Metric latest = history.get(0);
        if (latest.getGlucoseLevel() != null && latest.getGlucoseLevel() > 125) return "low glycemic index vegetables";
        if (latest.getSystolic()     != null && latest.getSystolic()     >= 140) return "low sodium DASH diet foods";
        if (latest.getBmi()          != null && latest.getBmi()          >= 30)  return "low calorie high fiber foods";
        return "mediterranean diet healthy foods";
    }
    
    // Servicios web externos

    // Consulta la API openFDA para obtener datos de medicamentos relacionados
    private void fetchExternalMedicalData() {
        Platform.runLater(() -> txtWebService.setText("Conectando con servicio openFDA..."));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fda.gov/drug/label.json?search=indications_and_usage:hypertension+OR+diabetes+OR+obesity&limit=5"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    try {
                        JsonObject root    = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray  results = root.getAsJsonArray("results");
                        StringBuilder sb   = new StringBuilder("FDA — Medicamentos\n\n");
                        for (int i = 0; i < Math.min(3, results.size()); i++) {
                            JsonObject item    = results.get(i).getAsJsonObject();
                            JsonObject openfda = item.has("openfda") ? item.getAsJsonObject("openfda") : null;
                            if (openfda != null) {
                                if (openfda.has("brand_name"))
                                    sb.append("• ").append(openfda.getAsJsonArray("brand_name").get(0).getAsString()).append("\n");
                                if (openfda.has("generic_name"))
                                    sb.append("  Genérico: ").append(openfda.getAsJsonArray("generic_name").get(0).getAsString()).append("\n");
                                if (openfda.has("route"))
                                    sb.append("  Vía: ").append(openfda.getAsJsonArray("route").get(0).getAsString()).append("\n");
                                sb.append("\n");
                            }
                        }
                        sb.append("Fuente: openFDA (api.fda.gov)");
                        Platform.runLater(() -> txtWebService.setText(sb.toString()));
                    } catch (Exception e) {
                        Platform.runLater(() -> txtWebService.setText("Error al procesar datos FDA.\n" + e.getMessage()));
                    }
                });
    }

    // Consulta USDA FoodData Central para obtener información nutricional
    private void fetchNutritionalData(String foodQuery) {
        Platform.runLater(() -> txtNutrition.setText("Consultando USDA FoodData Central..."));

        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?query="
                + foodQuery.replace(" ", "%20") + "&api_key=DEMO_KEY&pageSize=3";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    String parsed = parseNutritionalResponse(responseBody, foodQuery);
                    Platform.runLater(() -> txtNutrition.setText(parsed));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> txtNutrition.setText(
                            "Error al conectar con USDA FoodData Central\n" + e.getMessage()));
                    return null;
                });
    }

    // Guarda la nota que el médico escribió en txtNoteInput para el paciente seleccionado
    @FXML
    protected void onSaveDoctorNote() {
        User paciente = comboPatients.getValue();
        if (paciente == null || txtNoteInput == null) return;
        String texto = txtNoteInput.getText().trim();
        if (texto.isEmpty()) return;

        // Usa el título del campo compartido si tiene contenido, si no genera uno automático
        String titulo = (txtRecommendationTitle != null && !txtRecommendationTitle.getText().trim().isEmpty())
                ? txtRecommendationTitle.getText().trim()
                : "Nota del Dr. " + loggedInDoctor.getLastName();

        Recommendation nota = new Recommendation();
        nota.setPatientId  (paciente.getUid());
        nota.setDoctorId   (loggedInDoctor.getUid());
        nota.setType       ("note");
        nota.setTitle      (titulo);
        nota.setMessage    (texto);
        nota.setGeneratedAt(Timestamp.now());
        nota.setIsRead     (false);

        new Thread(() -> {
            try {
                String nuevoId = recommendationDAO.createDocumentId();
                nota.setId(nuevoId);
                recommendationDAO.save(nuevoId, nota);
                Platform.runLater(() -> {
                    txtNoteInput.clear();
                    if (txtRecommendationTitle != null) txtRecommendationTitle.clear();
                    if (lblRecommendationStatus != null) {
                        lblRecommendationStatus.setText("Nota guardada correctamente.");
                        lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#4caf50"));
                    }
                    loadAllRecommendationsForPatient(paciente.getUid());
                });
            } catch (Exception e) {
                System.err.println("[RecommendationsController] Error al guardar nota: " + e.getMessage());
            }
        }).start();
    }

    //Guarda una recomendación formal en Firestore y la envía al paciente por correo electrónico
    @FXML
    protected void onSendRecommendation() {
        // Verificación de rol: solo médicos pueden enviar recomendaciones por email
        if (loggedInDoctor == null || !"doctor".equals(loggedInDoctor.getRole())) {
            if (lblRecommendationStatus != null) {
                lblRecommendationStatus.setText("Solo los médicos pueden enviar recomendaciones.");
                lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#ff5252"));
            }
            return;
        }

        User patient = comboPatients.getValue();
        if (patient == null) {
            if (lblRecommendationStatus != null) {
                lblRecommendationStatus.setText("Selecciona un paciente primero.");
                lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#ff5252"));
            }
            return;
        }

        String title   = (txtRecommendationTitle != null) ? txtRecommendationTitle.getText().trim() : "";
        String message = (txtNoteInput           != null) ? txtNoteInput.getText().trim()           : "";

        if (title.isEmpty() || message.isEmpty()) {
            if (lblRecommendationStatus != null) {
                lblRecommendationStatus.setText("Completa el título y el mensaje antes de enviar.");
                lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#ff5252"));
            }
            return;
        }

        String doctorName = "Dr. " + loggedInDoctor.getFirstName() + " " + loggedInDoctor.getLastName();

        if (lblRecommendationStatus != null) {
            lblRecommendationStatus.setText("Guardando y enviando correo...");
            lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#ffffff"));
        }

        // Construimos el objeto Recommendation antes de lanzar el hilo
        Recommendation rec = new Recommendation();
        rec.setPatientId  (patient.getUid());
        rec.setDoctorId   (loggedInDoctor.getUid());
        rec.setGeneratedAt(Timestamp.now());
        rec.setType       ("doctor_recommendation");
        rec.setTitle      (title);
        rec.setMessage    (message);
        rec.setIsRead     (false);

        new Thread(() -> {
            try {
                // guarda en Firestore
                String newId = recommendationDAO.createDocumentId();
                rec.setId(newId);
                recommendationDAO.save(newId, rec);

                // envia correo al paciente
                notificationService.sendRecommendationEmail(patient, doctorName, title, message);

                Platform.runLater(() -> {
                    if (txtNoteInput           != null) txtNoteInput.clear();
                    if (txtRecommendationTitle != null) txtRecommendationTitle.clear();
                    if (lblRecommendationStatus != null) {
                        lblRecommendationStatus.setText("Recomendación guardada y correo enviado");
                        lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#4caf50"));
                    }
                    loadAllRecommendationsForPatient(patient.getUid());
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblRecommendationStatus != null) {
                        lblRecommendationStatus.setText("Error al guardar o enviar: " + e.getMessage());
                        lblRecommendationStatus.setTextFill(javafx.scene.paint.Color.web("#ff5252"));
                    }
                });
                System.err.println("[RecommendationsController] Error en onSendRecommendation: " + e.getMessage());
            }
        }).start();
    }

    // Parsea la respuesta JSON de USDA y extrae macronutrientes de los primeros 3 alimentos
    private String parseNutritionalResponse(String jsonBody, String query) {
        try {
            JsonObject root  = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonArray  foods = root.has("foods") ? root.getAsJsonArray("foods") : null;

            if (foods == null || foods.size() == 0) {
                return "No se encontraron resultados nutricionales para: " + query;
            }

            StringBuilder result = new StringBuilder("USDA FoodData Central\n");
            result.append("Búsqueda: ").append(query).append("\n\n");

            for (int i = 0; i < Math.min(3, foods.size()); i++) {
                JsonObject food = foods.get(i).getAsJsonObject();
                String description = food.has("description") ? food.get("description").getAsString() : "N/A";
                result.append("• ").append(description).append("\n");

                if (food.has("foodNutrients")) {
                    JsonArray nutrients = food.getAsJsonArray("foodNutrients");
                    for (int j = 0; j < nutrients.size(); j++) {
                        JsonObject nutrient = nutrients.get(j).getAsJsonObject();
                        if (!nutrient.has("nutrientName") || !nutrient.has("value")) continue;
                        String name = nutrient.get("nutrientName").getAsString();
                        if (name.equals("Energy") || name.equals("Protein")
                                || name.equals("Carbohydrate, by difference")
                                || name.equals("Total lipid (fat)")) {
                            double value = nutrient.get("value").getAsDouble();
                            String unit  = nutrient.has("unitName")
                                    ? nutrient.get("unitName").getAsString() : "";
                            result.append("  - ").append(name).append(": ")
                                    .append(value).append(" ").append(unit).append("\n");
                        }
                    }
                }
                result.append("\n");
            }

            result.append("Fuente: USDA FoodData Central (api.nal.usda.gov)");
            return result.toString();

        } catch (Exception e) {
            return "Error al procesar la respuesta nutricional: " + e.getMessage();
        }
    }

    // Exportación a Excel

    // Se ejecuta al presionar "Exportar Excel".
    // 1. Verifica que haya un paciente seleccionado.
    // 2. Abre un FileChooser para elegir dónde guardar el .xlsx.
    // 3. En un hilo de fondo recopila métricas, recomendaciones y alertas.
    // 4. Llama a generateExcel() y muestra un aviso de éxito o error.
    @FXML
    protected void onExportExcel() {
        User selected = comboPatients.getValue();
        if (selected == null) {
            Alert aviso = new Alert(Alert.AlertType.WARNING);
            aviso.setTitle("Sin paciente seleccionado");
            aviso.setHeaderText(null);
            aviso.setContentText("Selecciona un paciente antes de exportar el reporte.");
            aviso.showAndWait();
            return;
        }

        // Abrir el diálogo de guardado con nombre predeterminado
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte Excel");
        fc.setInitialFileName("HealthTrack_"
                + selected.getLastName()
                + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + ".xlsx");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File archivo = fc.showSaveDialog(stage);
        if (archivo == null) return; // El usuario canceló el diálogo

        // Capturar el texto de diagnóstico actual en el hilo FX antes de entrar al hilo de fondo
        final String diagnostico = txtRecommendations.getText();

        new Thread(() -> {
            try {
                // Recopilar todos los datos necesarios para las tres hojas
                List<Metric>         metricas = getMetricsByPatient(selected.getUid());
                List<Recommendation> recs     = getRecommendationsByPatient(selected.getUid());
                String               alertas  = AlertUtils.buildAlertsText(metricas);

                // Generar y guardar el archivo Excel
                generateExcel(archivo, diagnostico, alertas, metricas, recs);

                Platform.runLater(() -> {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Exportación completada");
                    ok.setHeaderText(null);
                    ok.setContentText("Reporte guardado correctamente:\n" + archivo.getAbsolutePath());
                    ok.showAndWait();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error al exportar");
                    err.setHeaderText(null);
                    err.setContentText("No se pudo generar el Excel:\n" + e.getMessage());
                    err.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Obtiene las recomendaciones clínicas (excluye notas manuales del médico)
    // del paciente indicado, ordenadas de más reciente a más antigua.
    private List<Recommendation> getRecommendationsByPatient(String patientId) throws Exception {
        List<Recommendation> todas = recommendationDAO.getByField("patientId", patientId);
        List<Recommendation> analisis = new ArrayList<>();
        for (Recommendation r : todas) {
            if (!"note".equals(r.getType())) analisis.add(r);
        }
        RecommendationUtils.sortByDateDesc(analisis);
        return analisis;
    }

    // Genera el archivo Excel con tres hojas:
    //   Hoja 1 "Análisis del Paciente" — resumen diagnóstico + alertas activas
    //   Hoja 2 "Historial de Métricas" — tabla con todas las lecturas registradas
    //   Hoja 3 "Recomendaciones"       — historial de análisis clínicos guardados en Firestore
    private void generateExcel(File archivo, String diagnostico, String alertas,
                                List<Metric> metricas, List<Recommendation> recs) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // ── Estilo compartido para encabezados de columna ──────────────
            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            fuenteEncabezado.setFontHeightInPoints((short) 11);
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setBorderBottom(BorderStyle.THIN);

            // ── Estilo para el título principal de cada hoja ───────────────
            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            estiloTitulo.setFont(fuenteTitulo);

            // HOJA 1: Análisis del Paciente
            Sheet hoja1 = workbook.createSheet("Análisis del Paciente");
            int fila = 0;

            // Título de la hoja
            Row filaTitulo = hoja1.createRow(fila++);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("REPORTE CLÍNICO — HealthTrack");
            celdaTitulo.setCellStyle(estiloTitulo);

            // Fecha de generación del reporte
            hoja1.createRow(fila++).createCell(0).setCellValue(
                    "Generado el: " + LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            fila++; // Fila en blanco separadora

            // Sección: Diagnóstico y Análisis Clínico
            Cell celdaDiagHeader = hoja1.createRow(fila++).createCell(0);
            celdaDiagHeader.setCellValue("DIAGNÓSTICO Y ANÁLISIS CLÍNICO");
            celdaDiagHeader.setCellStyle(estiloEncabezado);

            String textoAnalisis = (diagnostico != null && !diagnostico.isEmpty())
                    ? diagnostico : "No se ha generado ningún análisis para este paciente.";
            for (String linea : textoAnalisis.split("\n")) {
                hoja1.createRow(fila++).createCell(0).setCellValue(linea);
            }

            fila++; // Fila en blanco separadora

            // Sección: Alertas Detectadas
            Cell celdaAlertasHeader = hoja1.createRow(fila++).createCell(0);
            celdaAlertasHeader.setCellValue("ALERTAS DETECTADAS");
            celdaAlertasHeader.setCellStyle(estiloEncabezado);

            for (String linea : alertas.split("\n")) {
                hoja1.createRow(fila++).createCell(0).setCellValue(linea);
            }

            hoja1.setColumnWidth(0, 90 * 256); // Columna ancha para texto largo

            // HOJA 2: Historial de Métricas
            Sheet hoja2 = workbook.createSheet("Historial de Métricas");
            String[] columnasMetricas = {
                "Fecha",
                "Sistólica (mmHg)",
                "Diastólica (mmHg)",
                "Frec. Cardíaca (lpm)",
                "Glucosa (mg/dL)",
                "Peso (kg)",
                "IMC"
            };

            // Fila de encabezados
            Row filaEncMetrica = hoja2.createRow(0);
            for (int i = 0; i < columnasMetricas.length; i++) {
                Cell c = filaEncMetrica.createCell(i);
                c.setCellValue(columnasMetricas[i]);
                c.setCellStyle(estiloEncabezado);
            }

            // Filas de datos
            int filaDato = 1;
            for (Metric m : metricas) {
                Row fila2 = hoja2.createRow(filaDato++);
                String fecha = m.getTimestamp() != null
                        ? m.getTimestamp().toDate().toString().substring(0, 16) : "";
                fila2.createCell(0).setCellValue(fecha);
                fila2.createCell(1).setCellValue(m.getSystolic()     != null ? m.getSystolic()     : 0);
                fila2.createCell(2).setCellValue(m.getDiastolic()    != null ? m.getDiastolic()    : 0);
                fila2.createCell(3).setCellValue(m.getHeartRate()    != null ? m.getHeartRate()    : 0);
                fila2.createCell(4).setCellValue(m.getGlucoseLevel() != null ? m.getGlucoseLevel() : 0.0);
                fila2.createCell(5).setCellValue(m.getWeight()       != null ? m.getWeight()       : 0.0);
                fila2.createCell(6).setCellValue(m.getBmi()          != null ? m.getBmi()          : 0.0);
            }

            // Ajustar ancho automático de todas las columnas
            for (int i = 0; i < columnasMetricas.length; i++) hoja2.autoSizeColumn(i);

            // HOJA 3: Recomendaciones
            Sheet hoja3 = workbook.createSheet("Recomendaciones");
            String[] columnasRec = { "Fecha", "Tipo", "Título", "Mensaje" };

            // Fila de encabezados
            Row filaEncRec = hoja3.createRow(0);
            for (int i = 0; i < columnasRec.length; i++) {
                Cell c = filaEncRec.createCell(i);
                c.setCellValue(columnasRec[i]);
                c.setCellStyle(estiloEncabezado);
            }

            // Filas de datos
            int filaRec = 1;
            for (Recommendation rec : recs) {
                Row filaR = hoja3.createRow(filaRec++);
                String fecha = rec.getGeneratedAt() != null
                        ? rec.getGeneratedAt().toDate().toString().substring(0, 16) : "";
                filaR.createCell(0).setCellValue(fecha);
                filaR.createCell(1).setCellValue(rec.getType()  != null ? rec.getType()  : "");
                filaR.createCell(2).setCellValue(rec.getTitle() != null ? rec.getTitle() : "");

                // Truncar mensajes muy largos para no saturar la celda de Excel
                String msg = rec.getMessage() != null ? rec.getMessage() : "";
                filaR.createCell(3).setCellValue(
                        msg.length() > 500 ? msg.substring(0, 500) + "..." : msg);
            }

            // Autoajustar las primeras tres columnas; la cuarta (Mensaje) queda fija
            for (int i = 0; i < 3; i++) hoja3.autoSizeColumn(i);
            hoja3.setColumnWidth(3, 60 * 256);

            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                workbook.write(salida);
            }
        }
    }
}
