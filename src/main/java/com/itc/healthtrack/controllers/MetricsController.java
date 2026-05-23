package com.itc.healthtrack.controllers;

import com.google.cloud.Timestamp;
import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.NotificationService;
import com.itc.healthtrack.services.UserService;
import com.itc.healthtrack.utils.DialogUtils;
import com.itc.healthtrack.utils.MetricUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class MetricsController {

    // Componentes de la interfaz de usuario
    @FXML private ComboBox<User> comboPatients;
    @FXML private TextField txtSystolic, txtDiastolic, txtHeartRate, txtGlucose, txtWeight;
    @FXML private Button btnSave;
    @FXML private Label lblStatus;

    // Filtros y promedios
    @FXML private ComboBox<String> comboTimeFilter;
    @FXML private Label lblAvgBP, lblAvgGlucose, lblAvgWeight;

    // Tabla y gráficos
    @FXML private TableView<Metric> tableMetrics;                 // Tabla de historial de métricas
    @FXML private TableColumn<Metric, String> colDate, colSysDia;  // Columnas: Fecha y Presión (Sis/Dia)
    @FXML private TableColumn<Metric, String> colHeartRate, colGlucose, colWeight;
    @FXML private LineChart<String, Number> evolutionChart;       // Gráfico de línea: Evolución de presión
    @FXML private BarChart<String, Number> averagesChart;         // Gráfico de barras: Promedios

    // Acceso a la base de datos y servicios
    private final GenericDAO<Metric> metricDao = new GenericDAO<>(Metric.class, "metrics");
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private final ObservableList<Metric> metricsObservableList = FXCollections.observableArrayList();

    // Estado del controlador
    private User loggedInDoctor;                                   // Usuario actualmente logeado
    private Metric selectedMetric = null;                          // Métrica seleccionada en la tabla
    private List<Metric> currentPatientHistory = new ArrayList<>();  // Respaldo del historial completo

    /*
     Inicializa el controlador con los datos del usuario logeado
     Configura la tabla y carga los pacientes según el rol
     - Pacientes solo ven sus propias métricas
     - Médicos ven sus pacientes asignados
     - Admins ven todos los pacientes
     */
    public void initData(User user) {
        this.loggedInDoctor = user;
        setupTable();
        setupFilters();

        if ("patient".equals(user.getRole())) {
            // El paciente solo puede ver y registrar sus propias métricas
            comboPatients.getItems().add(user);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
            loadMetricsForPatient(user.getUid());
        } else {
            // Médicos y admins ven la lista desplegable de sus pacientes asignados
            loadPatientsIntoCombo();
            comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    onClearForm();
                    loadMetricsForPatient(newVal.getUid());
                }
            });
        }
    }

    private void setupFilters() {
        // Configurar opciones de filtro de período
        comboTimeFilter.setItems(FXCollections.observableArrayList("Historial Completos", "Últimos 7 Días", "Últimos 30 Días"));
        comboTimeFilter.getSelectionModel().selectFirst();

        // Escuchador: cuando cambia el filtro, recargar datos
        comboTimeFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyTimeFilter();
        });
    }

    // Configura las columnas de la tabla y establece escuchadores de evento
    // También inicializa los gráficos de línea y barras
    private void setupTable() {
        // Configurar el formato de las columnas de la tabla
        colDate.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getTimestamp();
            return new SimpleStringProperty(ts != null ? ts.toDate().toString().substring(0, 19) : "N/A");
        });

        colSysDia.setCellValueFactory(cellData -> {
            Integer sys = cellData.getValue().getSystolic();
            Integer dia = cellData.getValue().getDiastolic();
            return new SimpleStringProperty((sys != null && dia != null) ? sys + "/" + dia : "-");
        });

        colHeartRate.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getHeartRate() != null ? String.valueOf(cellData.getValue().getHeartRate()) : "-"));
        colGlucose.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGlucoseLevel() != null ? String.valueOf(cellData.getValue().getGlucoseLevel()) : "-"));
        colWeight.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getWeight() != null ? String.valueOf(cellData.getValue().getWeight()) : "-"));

        tableMetrics.setItems(metricsObservableList);

        // Cuando se selecciona una fila, llenar el formulario y cambiar botón a "Actualizar"
        tableMetrics.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedMetric = newSelection;
                txtSystolic.setText(newSelection.getSystolic() != null ? String.valueOf(newSelection.getSystolic()) : "");
                txtDiastolic.setText(newSelection.getDiastolic() != null ? String.valueOf(newSelection.getDiastolic()) : "");
                txtHeartRate.setText(newSelection.getHeartRate() != null ? String.valueOf(newSelection.getHeartRate()) : "");
                txtGlucose.setText(newSelection.getGlucoseLevel() != null ? String.valueOf(newSelection.getGlucoseLevel()) : "");
                txtWeight.setText(newSelection.getWeight() != null ? String.valueOf(newSelection.getWeight()) : "");

                btnSave.setText("Actualizar");
            }
        });
    }

    // Carga la lista de pacientes visibles para el usuario logeado en el ComboBox
    private void loadPatientsIntoCombo() {
        new Thread(() -> {
            try {
                List<User> patients = userService.getPatientsForUser(loggedInDoctor);
                Platform.runLater(() -> comboPatients.setItems(FXCollections.observableArrayList(patients)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /*Carga el historial de métricas de un paciente desde Firestore
     Se ejecuta en un hilo secundario para no bloquear la interfaz*/
    private void loadMetricsForPatient(String patientId) {
        new Thread(() -> {
            try {
                List<Metric> history = getMetricsByPatientId(patientId);
                Platform.runLater(() -> {
                    currentPatientHistory = history; // Guardar respaldo
                    applyTimeFilter(); // Aplicar el filtro que a su vez llena la tabla y gráficas
                    lblStatus.setText("Historial cargado con éxito");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar el historial");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
            }
        }).start();
    }

    /*Aplica el filtro de período al historial completo de métricas
     Actualiza la tabla, gráficos y calcula promedios según el filtro seleccionado*/
    private void applyTimeFilter() {
        if (currentPatientHistory.isEmpty()) {
            metricsObservableList.clear();
            evolutionChart.getData().clear();
            averagesChart.getData().clear();
            calculateAverages(new ArrayList<>());
            return;
        }

        // Obtener el filtro seleccionado y calcular el límite de tiempo
        String filter = comboTimeFilter.getValue();
        long nowSeconds = System.currentTimeMillis() / 1000;
        long limitSeconds = 0;

        if ("Últimos 7 Días".equals(filter)) limitSeconds = 7 * 24 * 3600;
        else if ("Últimos 30 Días".equals(filter)) limitSeconds = 30 * 24 * 3600;

        // Filtrar las métricas según el período seleccionado
        List<Metric> filteredList = new ArrayList<>();
        for (Metric m : currentPatientHistory) {
            if (m.getTimestamp() == null) {
                if (limitSeconds == 0) filteredList.add(m); // sin fecha: incluir solo en historial completo
                continue;
            }
            long metricTime = m.getTimestamp().getSeconds();
            if (limitSeconds == 0 || (nowSeconds - metricTime) <= limitSeconds) {
                filteredList.add(m);
            }
        }

        // Actualizar tabla, gráficos y promedios
        metricsObservableList.clear();
        metricsObservableList.addAll(filteredList);
        updateChart(filteredList);
        updateBarChart(filteredList);
        calculateAverages(filteredList);
    }

    //Calcula y muestra los promedios de presión arterial, glucosa y peso para el período de datos actualmente visible
    private void calculateAverages(List<Metric> data) {
        if (data.isEmpty()) {
            lblAvgBP.setText("PA: --/-- mmHg");
            lblAvgGlucose.setText("Glucosa: -- mg/dL");
            lblAvgWeight.setText("Peso: -- kg");
            return;
        }

        // Sumar todos los valores
        int sysTotal = 0, diaTotal = 0, bpCount = 0;
        double glTotal = 0, weightTotal = 0;
        int glCount = 0, weightCount = 0;

        for (Metric m : data) {
            if (m.getSystolic() != null && m.getDiastolic() != null) {
                sysTotal += m.getSystolic();
                diaTotal += m.getDiastolic();
                bpCount++;
            }
            if (m.getGlucoseLevel() != null) { glTotal += m.getGlucoseLevel(); glCount++; }
            if (m.getWeight() != null) { weightTotal += m.getWeight(); weightCount++; }
        }

        // Mostrar promedios calculados
        lblAvgBP.setText(bpCount > 0 ? "PA: " + (sysTotal/bpCount) + "/" + (diaTotal/bpCount) + " mmHg" : "PA: --/-- mmHg");
        lblAvgGlucose.setText(glCount > 0 ? String.format("Glucosa: %.1f mg/dL", (glTotal/glCount)) : "Glucosa: -- mg/dL");
        lblAvgWeight.setText(weightCount > 0 ? String.format("Peso: %.1f kg", (weightTotal/weightCount)) : "Peso: -- kg");
    }

    /*Actualiza el gráfico de línea con la evolución de presión arterial.
     Muestra sistólica y diastólica en series separadas  */
    private void updateChart(List<Metric> history) {
        evolutionChart.getData().clear();

        XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
        systolicSeries.setName("Sistólica");

        XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
        diastolicSeries.setName("Diastólica");

        // Iteración inversa para mostrar los datos más antiguos a la izquierda
        for (int i = history.size() - 1; i >= 0; i--) {
            Metric m = history.get(i);
            if (m.getSystolic() != null && m.getDiastolic() != null) {
                String label = m.getTimestamp().toDate().toString().substring(4, 10);
                systolicSeries.getData().add(new XYChart.Data<>(label, m.getSystolic()));
                diastolicSeries.getData().add(new XYChart.Data<>(label, m.getDiastolic()));
            }
        }

        evolutionChart.getData().addAll(systolicSeries, diastolicSeries);
    }

    /*Actualiza el gráfico de barras con los promedios de todas las métricas.
     Muestra sistólica, diastólica, frecuencia cardíaca, glucosa y peso */
    private void updateBarChart(List<Metric> history) {
        averagesChart.getData().clear();

        if (history.isEmpty()) return;

        XYChart.Series<String, Number> avgSeries = new XYChart.Series<>();
        avgSeries.setName("Promedio del período");

        // Calcular promedios de todas las métricas
        int sysTotal = 0, diaTotal = 0, hrTotal = 0;
        double glTotal = 0, weightTotal = 0;
        int sysCount = 0, diaCount = 0, hrCount = 0, glCount = 0, weightCount = 0;

        for (Metric m : history) {
            if (m.getSystolic() != null)     { sysTotal    += m.getSystolic();    sysCount++;    }
            if (m.getDiastolic() != null)    { diaTotal    += m.getDiastolic();   diaCount++;    }
            if (m.getHeartRate() != null)    { hrTotal     += m.getHeartRate();   hrCount++;     }
            if (m.getGlucoseLevel() != null) { glTotal     += m.getGlucoseLevel(); glCount++;   }
            if (m.getWeight() != null)       { weightTotal += m.getWeight();      weightCount++; }
        }

        // Agregar al gráfico los promedios calculados
        if (sysCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Sistólica",  sysTotal    / (double) sysCount));
        if (diaCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Diastólica", diaTotal    / (double) diaCount));
        if (hrCount > 0)     avgSeries.getData().add(new XYChart.Data<>("F. Cardíaca", hrTotal    / (double) hrCount));
        if (glCount > 0)     avgSeries.getData().add(new XYChart.Data<>("Glucosa",    glTotal     / (double) glCount));
        if (weightCount > 0) avgSeries.getData().add(new XYChart.Data<>("Peso (kg)",  weightTotal / (double) weightCount));

        averagesChart.getData().add(avgSeries);
    }

    /*Guarda una nueva métrica o actualiza una existente
      Calcula automáticamente el IMC si están disponibles peso y altura
    Evalúa valores clínicos y muestra alertas si se detectan anomalías*/
    @FXML
    protected void onSaveMetric() {
        User selectedPatient = comboPatients.getValue();
        if (selectedPatient == null) {
            lblStatus.setText("Error: Selecciona un paciente primero");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        try {
            boolean isNewRecord = (selectedMetric == null);
            Metric metricToProcess = isNewRecord ? new Metric() : selectedMetric;

            if (isNewRecord) {
                metricToProcess.setPatientId(selectedPatient.getUid());
                metricToProcess.setTimestamp(Timestamp.now());
            }

            metricToProcess.setSystolic(txtSystolic.getText().isEmpty() ? null : Integer.parseInt(txtSystolic.getText()));
            metricToProcess.setDiastolic(txtDiastolic.getText().isEmpty() ? null : Integer.parseInt(txtDiastolic.getText()));
            metricToProcess.setHeartRate(txtHeartRate.getText().isEmpty() ? null : Integer.parseInt(txtHeartRate.getText()));
            metricToProcess.setGlucoseLevel(txtGlucose.getText().isEmpty() ? null : Double.parseDouble(txtGlucose.getText()));
            metricToProcess.setWeight(txtWeight.getText().isEmpty() ? null : Double.parseDouble(txtWeight.getText()));

            // Calcular el IMC si están disponibles el peso y la altura
            if (metricToProcess.getWeight() != null && selectedPatient.getHeight() != null
                    && selectedPatient.getHeight() > 0) {
                double heightM = selectedPatient.getHeight();
                // Si la altura es mayor a 3.0, se asume que fue ingresada en centímetros (ej: 175)
                // y se convierte a metros automáticamente (175 → 1.75)
                if (heightM > 3.0) {
                    heightM = heightM / 100.0;
                }
                double bmi = metricToProcess.getWeight() / (heightM * heightM);
                metricToProcess.setBmi(Math.round(bmi * 10.0) / 10.0);
            }

            lblStatus.setText(isNewRecord ? "Guardando..." : "Actualizando...");
            lblStatus.setTextFill(Color.web("#ffffff"));

            // Llamamos a una función que revisa si las métricas del paciente tienen valores peligrosos
            String alert = evaluateClinicalThresholds(metricToProcess, selectedPatient);
            if (alert != null) {
                showClinicalAlert(alert);
            }

            new Thread(() -> {
                try {
                    if (isNewRecord) {
                        // Genera un ID nuevo para la métrica
                        String newId = metricDao.createDocumentId();
                        // Asigna el ID al objeto y lo guarda en Firestore
                        metricToProcess.setId(newId);
                        metricDao.save(newId, metricToProcess);
                    } else {
                        metricDao.save(metricToProcess.getId(), metricToProcess);
                    }

                    Platform.runLater(() -> {
                        onClearForm();
                        loadMetricsForPatient(selectedPatient.getUid());
                        lblStatus.setText(isNewRecord ? "Métrica guardada" : "Métrica actualizada");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al guardar en la base de datos");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            lblStatus.setText("Error: Usa valores numéricos válidos");
            lblStatus.setTextFill(Color.web("#ff5252"));
        }
    }

    /*Evalúa datos y retorna un mensaje de alerta si se detectan valores críticos
     Verifica presión arterial, glucosa, frecuencia cardíaca e índice de masa corporal    */
    private String evaluateClinicalThresholds(Metric metric, User patient) {
        StringBuilder alert = new StringBuilder();

        // Evaluar presión arterial
        if (metric.getSystolic() != null && metric.getDiastolic() != null) {
            int sys = metric.getSystolic();
            int dia = metric.getDiastolic();
            if (sys >= 180 || dia >= 120) {
                alert.append("ALERTA CRÍTICA: Hipertensión en crisis (").append(sys).append("/").append(dia).append(" mmHg)\nConsulta médica urgente\n\n");
            } else if (sys >= 140 || dia >= 90) {
                alert.append("ALERTA: Hipertensión arterial detectada (").append(sys).append("/").append(dia).append(" mmHg)\n\n");
            }
        }

        // Evaluar glucosa
        if (metric.getGlucoseLevel() != null) {
            double gluc = metric.getGlucoseLevel();
            if (gluc > 300) {
                alert.append("ALERTA CRÍTICA: Glucosa muy elevada (").append(gluc).append(" mg/dL)\n Riesgo de cetoacidosis\n\n");
            } else if (gluc > 125) {
                alert.append("ALERTA: Glucosa elevada (").append(gluc).append(" mg/dL)\n Posible diabetes\n\n");
            } else if (gluc < 70) {
                alert.append("ALERTA: Hipoglucemia detectada (").append(gluc).append(" mg/dL)\n\n");
            }
        }

        // Evaluar frecuencia cardíaca
        if (metric.getHeartRate() != null) {
            int hr = metric.getHeartRate();
            if (hr > 120) {
                alert.append("ALERTA: Frecuencia cardíaca elevada (").append(hr).append(" lpm)\n Taquicardia\n\n");
            } else if (hr < 50) {
                alert.append("ALERTA: Frecuencia cardíaca baja (").append(hr).append(" lpm)\nBradicardia\n\n");
            }
        }

        // Evaluar IMC
        if (metric.getBmi() != null) {
            double bmi = metric.getBmi();
            if (bmi >= 40) {
                alert.append("ALERTA: Obesidad mórbida (IMC ").append(bmi).append(")\n Riesgo cardiovascular alto\n\n");
            } else if (bmi >= 30) {
                alert.append("Obesidad detectada (IMC ").append(bmi).append(")\n Se recomienda plan nutricional\n\n");
            }
        }

        return alert.length() > 0 ? alert.toString().trim() : null;
    }

    /* Muestra un diálogo de alerta clínica en la interfaz.
     También notifica al paciente y al médico si es aplicable. */
    private void showClinicalAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Alerta Clínica — HealthTrack");
            alert.setHeaderText("Se detectaron valores fuera del rango clínico normal");
            alert.setContentText(message);
            DialogUtils.applyWhiteStyle(alert.getDialogPane());
            alert.showAndWait();

            // Actualizar etiqueta de estado
            lblStatus.setText("Valores críticos detectados, revisa la alerta");
            lblStatus.setTextFill(Color.web("#ff9800"));

            // Notificar al paciente y al médico
            User patient = comboPatients.getValue();
            if (patient != null) {
                notificationService.notifyPatient(patient, "Valores clínicos críticos detectados en tu última medición, consulta a tu médico");

                if (loggedInDoctor != null
                        && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
                    // El médico o admin registró la métrica — se notifica directamente
                    notificationService.notifyDoctor(loggedInDoctor, "ALERTA: El paciente "
                            + patient.getFirstName() + " " + patient.getLastName()
                            + " tiene valores críticos registrados");
                } else if (patient.getAssignedDoctorId() != null && !patient.getAssignedDoctorId().isEmpty()) {
                    // El paciente registró su propia métrica — buscar al médico asignado y notificarlo
                    final String alertMsg = "ALERTA: El paciente "
                            + patient.getFirstName() + " " + patient.getLastName()
                            + " tiene valores críticos registrados";
                    new Thread(() -> {
                        try {
                            User assignedDoctor = userDao.getById(patient.getAssignedDoctorId());
                            if (assignedDoctor != null) {
                                notificationService.notifyDoctor(assignedDoctor, alertMsg);
                            }
                        } catch (Exception e) {
                            System.err.println("[MetricsController] Error al notificar al médico asignado: " + e.getMessage());
                        }
                    }).start();
                }
            }
        });
    }

    //Elimina la métrica seleccionada de la base de datos
    @FXML
    protected void onDeleteMetric() {
        if (selectedMetric == null) {
            lblStatus.setText("Selecciona una fila de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        lblStatus.setText("Eliminando...");
        lblStatus.setTextFill(Color.web("#ffffff"));
        String metricId = selectedMetric.getId();
        String pId = selectedMetric.getPatientId();

        new Thread(() -> {
            try {
                metricDao.delete(metricId);
                Platform.runLater(() -> {
                    onClearForm();
                    loadMetricsForPatient(pId);
                    lblStatus.setText("Métrica eliminada.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Limpia todos los campos del formulario y deselecciona la métrica actual
    @FXML
    protected void onClearForm() {
        txtSystolic.clear();
        txtDiastolic.clear();
        txtHeartRate.clear();
        txtGlucose.clear();
        txtWeight.clear();

        selectedMetric = null;
        tableMetrics.getSelectionModel().clearSelection();

        btnSave.setText("Guardar");
    }

    // Obtiene el historial de métricas de un paciente y lo ordena por fecha
    private List<Metric> getMetricsByPatientId(String patientId) throws Exception {
        List<Metric> metrics = metricDao.getByField("patientId", patientId);
        MetricUtils.sortByTimestampDesc(metrics);
        return metrics;
    }
}
