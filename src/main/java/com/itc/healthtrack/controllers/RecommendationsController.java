package com.itc.healthtrack.controllers;

import com.google.cloud.Timestamp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itc.healthtrack.dao.MetricDAO;
import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.dao.RecommendationDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.Recommendation;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

//Controlador para la gestión de recomendaciones clínicas
public class RecommendationsController {

    //Elementos de interfaz
    @FXML private ComboBox<User> comboPatients;     // ComboBox para seleccionar paciente
    @FXML private TextArea txtRecommendations;      // Área de texto para mostrar análisis/recomendaciones
    @FXML private TextArea txtWebService;           // Área para mostrar datos de servicios web (FDA)
    @FXML private TextArea txtNutrition;            // Área para mostrar información nutricional (USDA)
    @FXML private ListView<Recommendation> listHistory;  // Lista de recomendaciones históricas

    // Acceso a datos
    private final PatientDAO patientDAO = new PatientDAO();
    private final MetricDAO metricDAO = new MetricDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();
    private final NotificationService notificationService = new NotificationService();
    private User loggedInDoctor;                     // Usuario médico/admin logeado
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();  // Cliente HTTP para APIs externas

    private ObservableList<Recommendation> historyItems;  // Lista observable para el historial

    //Inicializa el controlador con los datos del usuario logeado
    //pacientes ven solo sus datos, médicos/admin ven lista de pacientes

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupHistory();

        if ("patient".equals(doctor.getRole())) {
            comboPatients.getItems().add(doctor);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
            // Asegura que el UID no sea nulo
            String uid = doctor.getUid() != null ? doctor.getUid() : "";
            System.out.println("UID paciente para historial: " + uid); // temporal
            loadRecommendationHistory(uid);
        } else {
            loadPatients();
            // Recargar historial cuando cambia el paciente seleccionado
            comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    historyItems.clear();
                    loadRecommendationHistory(newVal.getUid());
                }
            });
        }
    }

    /*Configura el ListView que muestra recomendaciones guardadas anteriormente
     Al hacer clic en un elemento, restaura el texto completo*/
    private void setupHistory() {
        historyItems = FXCollections.observableArrayList();
        listHistory.setItems(historyItems);

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

        // Mostrar el texto almacenado del análisis cuando se selecciona una entrada del historial
        listHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getMessage() != null) {
                txtRecommendations.setText(newVal.getMessage());
            }
        });
    }

    /*Carga la lista de pacientes en el ComboBox.
     Si es admin, carga todos los pacientes; si es médico, solo sus pacientes asignados*/
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> patients = "admin".equals(loggedInDoctor.getRole())
                        ? patientDAO.getAllPatients()
                        : patientDAO.getPatientsByDoctor(loggedInDoctor.getUid());
                Platform.runLater(() -> comboPatients.setItems(FXCollections.observableArrayList(patients)));
            } catch (Exception e) {
                Platform.runLater(() -> txtRecommendations.setText("Error al cargar la lista de pacientes"));
                e.printStackTrace();
            }
        }).start();
    }

    //Carga recomendaciones pasadas para el paciente dado y las muestra en listHistory
    private void loadRecommendationHistory(String patientId) {

        // 1. Definimos la tarea pesada que se ejecutará en segundo plano
        Runnable backgroundTask = () -> {
            try {
                // 2. Consultamos la base de datos de Firebase (esto requiere conexión a internet y toma tiempo)
                List<Recommendation> history = recommendationDAO.getRecommendationsByPatient(patientId);

                // 3. Definimos la tarea visual que modificará los componentes de la pantalla
                Runnable screenUpdate = () -> {
                    historyItems.clear();         // Vaciamos el historial anterior de la interfaz
                    historyItems.addAll(history); // Agregamos los nuevos datos descargados
                };

                // 4. Entregamos la tarea visual al hilo principal de JavaFX para que la ejecute de forma segura
                Platform.runLater(screenUpdate);

            } catch (Exception e) {
                // Capturamos cualquier error de red o base de datos
                System.err.println("Error cargando recomendaciones: " + e.getMessage());
            }
        };

        // 5. Asignamos nuestra tarea de fondo a un hilo nuevo y comenzamos su ejecución
        Thread hiloSecundario = new Thread(backgroundTask);
        hiloSecundario.start();
    }
    /*Analiza las métricas del paciente seleccionado y genera recomendaciones automáticas.
     Consulta APIs externas (clima, FDA, nutrición) y guarda el análisis en Firestore */
    @FXML
    protected void onAnalyzePatient() {
        User selected = comboPatients.getValue();
        if (selected == null) return;

        txtRecommendations.setText("Analizando datos del paciente...");
        txtWebService.setText("Consultando servicios externos...");
        txtNutrition.setText("Consultando USDA FoodData Central...");

        new Thread(() -> {
            try {
                List<Metric> history = metricDAO.getMetricsByPatient(selected.getUid());
                String weatherData = fetchWeatherData();
                String analysis = generateAlgorithmicRecommendations(history, weatherData);

                // Verificar progresión de riesgo y enviar notificación si es necesario
                if (hasRiskProgression(history)) {
                    notificationService.notifyPatient(selected,
                            "Análisis de tendencias detectó una progresión de riesgo en tus métricas. Consulta a tu médico.");
                    // Solo enviar notificación al médico cuando el usuario logeado es médico o admin
                    if (loggedInDoctor != null
                            && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
                        notificationService.notifyDoctor(loggedInDoctor,
                                "ALERTA DE TENDENCIA: El paciente " + selected.getFirstName()
                                        + " " + selected.getLastName()
                                        + " presenta una progresión de riesgo en sus métricas recientes.");
                    }
                }

                // Persistir análisis en Firestore y refrescar lista de historial después de guardar
                persistRecommendation(selected.getUid(), analysis);

                // Obtener datos FDA
                fetchExternalMedicalData();

                // Obtener consejos nutricionales basados en las últimas métricas
                String foodQuery = determineFoodQuery(history);
                fetchNutritionalData(foodQuery);

                Platform.runLater(() -> txtRecommendations.setText(analysis));

            } catch (Exception e) {
                Platform.runLater(() -> txtRecommendations.setText("Error al procesar el análisis."));
                e.printStackTrace();
            }
        }).start();
    }

    //Obtiene las condiciones climáticas actuales desde la API pública wttr.in
    private String fetchWeatherData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.open-meteo.com/v1/forecast?latitude=21.0190&longitude=-101.2574&current_weather=true&timezone=America%2FMexico_City"))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Open-Meteo status: " + response.statusCode());
            System.out.println("Open-Meteo body: " + response.body());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject current = root.getAsJsonObject("current_weather");
            double temp = current.get("temperature").getAsDouble();
            int code = current.get("weathercode").getAsInt();
            String condicion = weatherCodeToSpanish(code);
            return condicion + " " + temp + "°C";
        } catch (Exception e) {
            System.out.println("ERROR CLIMA: " + e.getMessage());
            return "No disponible";
        }
    }

    //Convierte códigos de clima de Open-Meteo a descripciones en español
    private String weatherCodeToSpanish(int code) {
        if (code == 0) return "Despejado";
        if (code <= 3) return "Parcialmente nublado";
        if (code <= 48) return "Nublado";
        if (code <= 67) return "Lluvia";
        if (code <= 77) return "Nieve";
        if (code <= 82) return "Chubascos";
        if (code <= 99) return "Tormenta";
        return "Variable";
    }

    /* Evalúa si las últimas tres lecturas consecutivas del paciente muestran un patrón
    de riesgo persistente (ej: tres lecturas hipertensivas o hiperglucémicas seguidas).
    Se requieren al menos tres lecturas porque un valor anormal único puede ser una anomalía aislada,
    mientras que tres valores críticos consecutivos indican una tendencia genuina*/
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

    /*recomendaciones basado en reglas que relaciona métricas del paciente
     con condiciones climáticas y guías nutricionales*/
    private String generateAlgorithmicRecommendations(List<Metric> history, String weatherData) {
        if (history == null || history.isEmpty()) {
            return "No hay registros clínicos suficientes para generar un análisis.";
        }

        Metric latest = history.get(0);
        StringBuilder report = new StringBuilder();
        report.append("ANÁLISIS CLÍNICO — HealthTrack \n");
        report.append("Última evaluación: ").append(latest.getTimestamp().toDate().toString()).append("\n");
        report.append("Condición climática actual: ").append(weatherData).append("\n\n");

        String weatherLower = weatherData.toLowerCase();
        boolean isRainy = weatherLower.contains("rain")
                || weatherLower.contains("drizzle")
                || weatherLower.contains("lluv");
        boolean isCold = weatherLower.contains("snow")
                || weatherLower.contains("cold")
                || weatherLower.contains("frio");

        // Reglas para presión arterial
        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            report.append("+ PRESIÓN ARTERIAL (").append(sys).append("/").append(dia).append(" mmHg):\n");
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

        // Reglas para glucosa
        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            report.append("+ GLUCOSA (").append(gluc).append(" mg/dL):\n");
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

        // Reglas para frecuencia cardíaca
        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            report.append("+ FRECUENCIA CARDÍACA (").append(hr).append(" lpm):\n");
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

        // Reglas para IMC
        if (latest.getBmi() != null) {
            double bmi = latest.getBmi();
            report.append("+ ÍNDICE DE MASA CORPORAL (IMC: ").append(bmi).append("):\n");
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

        report.append("--- Generado por HealthTrack ---");
        return report.toString();
    }

    /*Obtiene de forma asíncrona datos de etiquetas de medicamentos desde la API pública openFDA
     como demostración de integración con servicios web externos*/
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
                        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray results = root.getAsJsonArray("results");
                        StringBuilder sb = new StringBuilder("FDA — Medicamentos\n\n");
                        for (int i = 0; i < Math.min(3, results.size()); i++) {
                            JsonObject item = results.get(i).getAsJsonObject();
                            if (item.has("openfda")) {
                                JsonObject openfda = item.getAsJsonObject("openfda");
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

    /*Guarda el análisis clínico generado como un documento Recommendation en Firestore
    Recarga la lista de historial después de confirmar el guardado */
    private void persistRecommendation(String patientId, String analysisText) {
        new Thread(() -> {
            try {
                Recommendation rec = new Recommendation();
                rec.setPatientId(patientId);
                rec.setGeneratedAt(Timestamp.now());
                rec.setType("suggestion");
                rec.setTitle("Análisis Clínico Automático");
                rec.setMessage(analysisText);
                rec.setIsRead(false);
                recommendationDAO.saveRecommendation(rec);
                // Refrescar la lista de historial una vez confirmado el guardado
                loadRecommendationHistory(patientId);
            } catch (Exception e) {
                System.err.println("Error guardando recomendación: " + e.getMessage());
            }
        }).start();
    }

    //Elige una consulta de búsqueda nutricional que coincida con los últimos valores clínicos del paciente
    private String determineFoodQuery(List<Metric> history) {
        if (history == null || history.isEmpty()) {
            return "mediterranean diet healthy foods";
        }
        Metric latest = history.get(0);

        if (latest.getGlucoseLevel() != null && latest.getGlucoseLevel() > 125) {
            return "low glycemic index vegetables";
        }
        if (latest.getSystolic() != null && latest.getSystolic() >= 140) {
            return "low sodium DASH diet foods";
        }
        if (latest.getBmi() != null && latest.getBmi() >= 30) {
            return "low calorie high fiber foods";
        }
        return "mediterranean diet healthy foods";
    }

    /*Consulta de forma asíncrona la API pública USDA FoodData Central para obtener
     información nutricional sobre alimentos relevantes para la condición del paciente*/
    private void fetchNutritionalData(String foodQuery) {
        Platform.runLater(() -> txtNutrition.setText("Consultando USDA FoodData Central..."));

        String encodedQuery = foodQuery.replace(" ", "%20");
        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?query="
                + encodedQuery + "&api_key=DEMO_KEY&pageSize=3";

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

    /*Parsea la respuesta JSON de la API USDA FoodData Central y formatea
    los mejores resultados con sus principales valores de macronutrientes */
    private String parseNutritionalResponse(String jsonBody, String query) {
        try {
            JsonObject root = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonArray foods = root.has("foods") ? root.getAsJsonArray("foods") : null;

            if (foods == null || foods.size() == 0) {
                return "No se encontraron resultados nutricionales para: " + query;
            }

            StringBuilder result = new StringBuilder();
            result.append("USDA FoodData Central\n");
            result.append("Búsqueda: ").append(query).append("\n\n");

            for (int i = 0; i < Math.min(3, foods.size()); i++) {
                JsonObject food = foods.get(i).getAsJsonObject();
                String description;
                if (food.has("description")) description = food.get("description").getAsString();
                else description = "N/A";
                result.append("• ").append(description).append("\n");

                if (food.has("foodNutrients")) {
                    JsonArray nutrients = food.getAsJsonArray("foodNutrients");
                    for (int j = 0; j < nutrients.size(); j++) {
                        JsonObject nutrient = nutrients.get(j).getAsJsonObject();
                        if (!nutrient.has("nutrientName") || !nutrient.has("value")) continue;
                        String name = nutrient.get("nutrientName").getAsString();
                        if (name.equals("Energy")
                                || name.equals("Protein")
                                || name.equals("Carbohydrate, by difference")
                                || name.equals("Total lipid (fat)")) {
                            double value = nutrient.get("value").getAsDouble();
                            String unit = nutrient.has("unitName")
                                    ? nutrient.get("unitName").getAsString() : "";
                            result.append("  - ").append(name)
                                    .append(": ").append(value)
                                    .append(" ").append(unit).append("\n");
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
}
