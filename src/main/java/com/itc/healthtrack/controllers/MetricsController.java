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

    @FXML private ComboBox<User> comboPatients;
    @FXML private TextField txtSystolic, txtDiastolic, txtHeartRate, txtGlucose, txtWeight;
    @FXML private Button btnSave;
    @FXML private Label lblStatus;

    @FXML private ComboBox<String> comboTimeFilter;
    @FXML private Label lblAvgBP, lblAvgGlucose, lblAvgWeight;

    @FXML private TableView<Metric> tableMetrics;
    @FXML private TableColumn<Metric, String> colDate, colSysDia;
    @FXML private TableColumn<Metric, String> colHeartRate, colGlucose, colWeight;
    @FXML private LineChart<String, Number> evolutionChart;
    @FXML private BarChart<String, Number> averagesChart;

    private final GenericDAO<Metric> metricDao = new GenericDAO<>(Metric.class, "metrics");
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private final ObservableList<Metric> metricsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private Metric selectedMetric = null;
    private List<Metric> currentPatientHistory = new ArrayList<>();

    // inicializa el controlador y carga los pacientes segun el rol
    // pacientes ven solo sus metricas medicos ven sus asignados y admins todos
    public void initData(User user) {
        this.loggedInDoctor = user;
        setupTable();
        setupFilters();

        if ("patient".equals(user.getRole())) {
            // el paciente solo ve y registra sus propias metricas
            comboPatients.getItems().add(user);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
            loadMetricsForPatient(user.getUid());
        } else {
            // medicos y admins ven el desplegable de sus pacientes asignados
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
        // opciones del filtro de periodo
        comboTimeFilter.setItems(FXCollections.observableArrayList("Historial Completos", "Últimos 7 Días", "Últimos 30 Días"));
        comboTimeFilter.getSelectionModel().selectFirst();

        // al cambiar el filtro recargamos los datos
        comboTimeFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyTimeFilter();
        });
    }

    // configura las columnas de la tabla y los listeners
    private void setupTable() {
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

        // al seleccionar una fila llenamos el formulario y cambiamos el boton a Actualizar
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

    // carga la lista de pacientes visibles para el usuario en el combobox
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

    // carga el historial del paciente en hilo de fondo para no bloquear la interfaz
    private void loadMetricsForPatient(String patientId) {
        new Thread(() -> {
            try {
                List<Metric> history = getMetricsByPatientId(patientId);
                Platform.runLater(() -> {
                    currentPatientHistory = history;
                    applyTimeFilter();
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

    // aplica el filtro de periodo al historial y actualiza tabla graficos y promedios
    private void applyTimeFilter() {
        if (currentPatientHistory.isEmpty()) {
            metricsObservableList.clear();
            evolutionChart.getData().clear();
            averagesChart.getData().clear();
            calculateAverages(new ArrayList<>());
            return;
        }

        // obtenemos el filtro y calculamos el limite de tiempo
        String filter = comboTimeFilter.getValue();
        long nowSeconds = System.currentTimeMillis() / 1000;
        long limitSeconds = 0;

        if ("Últimos 7 Días".equals(filter)) limitSeconds = 7 * 24 * 3600;
        else if ("Últimos 30 Días".equals(filter)) limitSeconds = 30 * 24 * 3600;

        // filtramos las metricas segun el periodo
        List<Metric> filteredList = new ArrayList<>();
        for (Metric m : currentPatientHistory) {
            if (m.getTimestamp() == null) {
                if (limitSeconds == 0) filteredList.add(m); // sin fecha solo se incluye en historial completo
                continue;
            }
            long metricTime = m.getTimestamp().getSeconds();
            if (limitSeconds == 0 || (nowSeconds - metricTime) <= limitSeconds) {
                filteredList.add(m);
            }
        }

        // actualizamos tabla graficos y promedios
        metricsObservableList.clear();
        metricsObservableList.addAll(filteredList);
        updateChart(filteredList);
        updateBarChart(filteredList);
        calculateAverages(filteredList);
    }

    // calcula y muestra los promedios de presion glucosa y peso para el periodo visible
    private void calculateAverages(List<Metric> data) {
        if (data.isEmpty()) {
            lblAvgBP.setText("PA: --/-- mmHg");
            lblAvgGlucose.setText("Glucosa: -- mg/dL");
            lblAvgWeight.setText("Peso: -- kg");
            return;
        }

        // sumamos todos los valores
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

        // mostramos los promedios calculados
        lblAvgBP.setText(bpCount > 0 ? "PA: " + (sysTotal/bpCount) + "/" + (diaTotal/bpCount) + " mmHg" : "PA: --/-- mmHg");
        lblAvgGlucose.setText(glCount > 0 ? String.format("Glucosa: %.1f mg/dL", (glTotal/glCount)) : "Glucosa: -- mg/dL");
        lblAvgWeight.setText(weightCount > 0 ? String.format("Peso: %.1f kg", (weightTotal/weightCount)) : "Peso: -- kg");
    }

    // actualiza el grafico de linea con la evolucion de presion en series separadas
    private void updateChart(List<Metric> history) {
        evolutionChart.getData().clear();

        XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
        systolicSeries.setName("Sistólica");

        XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
        diastolicSeries.setName("Diastólica");

        // iteramos al reves para que los datos antiguos queden a la izquierda
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

    // actualiza el grafico de barras con los promedios de todas las metricas
    private void updateBarChart(List<Metric> history) {
        averagesChart.getData().clear();

        if (history.isEmpty()) return;

        XYChart.Series<String, Number> avgSeries = new XYChart.Series<>();
        avgSeries.setName("Promedio del período");

        // promedios de todas las metricas
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

        // metemos los promedios al grafico
        if (sysCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Sistólica",  sysTotal    / (double) sysCount));
        if (diaCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Diastólica", diaTotal    / (double) diaCount));
        if (hrCount > 0)     avgSeries.getData().add(new XYChart.Data<>("F. Cardíaca", hrTotal    / (double) hrCount));
        if (glCount > 0)     avgSeries.getData().add(new XYChart.Data<>("Glucosa",    glTotal     / (double) glCount));
        if (weightCount > 0) avgSeries.getData().add(new XYChart.Data<>("Peso (kg)",  weightTotal / (double) weightCount));

        averagesChart.getData().add(avgSeries);
    }

    // guarda o actualiza una metrica calcula el imc si hay peso y altura
    // checa los valores clinicos y muestra alertas si hay algo raro
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

            // calculamos el imc si hay peso y altura
            if (metricToProcess.getWeight() != null && selectedPatient.getHeight() != null
                    && selectedPatient.getHeight() > 0) {
                double heightM = selectedPatient.getHeight();
                // si la altura es mayor a 3.0 asumimos que vino en centimetros y la pasamos a metros
                if (heightM > 3.0) {
                    heightM = heightM / 100.0;
                }
                double bmi = metricToProcess.getWeight() / (heightM * heightM);
                metricToProcess.setBmi(Math.round(bmi * 10.0) / 10.0);
            }

            lblStatus.setText(isNewRecord ? "Guardando..." : "Actualizando...");
            lblStatus.setTextFill(Color.web("#ffffff"));

            // revisamos si las metricas del paciente tienen valores peligrosos
            String alert = evaluateClinicalThresholds(metricToProcess, selectedPatient);
            if (alert != null) {
                showClinicalAlert(alert);
            }

            new Thread(() -> {
                try {
                    if (isNewRecord) {
                        // generamos un id nuevo y lo guardamos en firestore
                        String newId = metricDao.createDocumentId();
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

    // evalua presion glucosa frecuencia e imc y devuelve un mensaje si hay valores criticos
    private String evaluateClinicalThresholds(Metric metric, User patient) {
        StringBuilder alert = new StringBuilder();

        // presion arterial
        if (metric.getSystolic() != null && metric.getDiastolic() != null) {
            int sys = metric.getSystolic();
            int dia = metric.getDiastolic();
            if (sys >= 180 || dia >= 120) {
                alert.append("ALERTA CRÍTICA: Hipertensión en crisis (").append(sys).append("/").append(dia).append(" mmHg)\nConsulta médica urgente\n\n");
            } else if (sys >= 140 || dia >= 90) {
                alert.append("ALERTA: Hipertensión arterial detectada (").append(sys).append("/").append(dia).append(" mmHg)\n\n");
            }
        }

        // glucosa
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

        // frecuencia cardiaca
        if (metric.getHeartRate() != null) {
            int hr = metric.getHeartRate();
            if (hr > 120) {
                alert.append("ALERTA: Frecuencia cardíaca elevada (").append(hr).append(" lpm)\n Taquicardia\n\n");
            } else if (hr < 50) {
                alert.append("ALERTA: Frecuencia cardíaca baja (").append(hr).append(" lpm)\nBradicardia\n\n");
            }
        }

        // imc
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

    // muestra el dialogo de alerta y avisa al paciente y al medico si aplica
    private void showClinicalAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Alerta Clínica — HealthTrack");
            alert.setHeaderText("Se detectaron valores fuera del rango clínico normal");
            alert.setContentText(message);
            DialogUtils.applyWhiteStyle(alert.getDialogPane());
            alert.showAndWait();

            // actualizamos la etiqueta de estado
            lblStatus.setText("Valores críticos detectados, revisa la alerta");
            lblStatus.setTextFill(Color.web("#ff9800"));

            // notificamos al paciente y al medico
            User patient = comboPatients.getValue();
            if (patient != null) {
                notificationService.notifyPatient(patient, "Valores clínicos críticos detectados en tu última medición, consulta a tu médico");

                if (loggedInDoctor != null
                        && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
                    // el medico o admin registro la metrica lo notificamos directo
                    notificationService.notifyDoctor(loggedInDoctor, "ALERTA: El paciente "
                            + patient.getFirstName() + " " + patient.getLastName()
                            + " tiene valores críticos registrados");
                } else if (patient.getAssignedDoctorId() != null && !patient.getAssignedDoctorId().isEmpty()) {
                    // el paciente registro su propia metrica buscamos su medico asignado
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

    // elimina la metrica seleccionada
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

    // limpia el formulario y deselecciona la metrica actual
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

    // trae el historial de metricas del paciente ordenado por fecha
    private List<Metric> getMetricsByPatientId(String patientId) throws Exception {
        List<Metric> metrics = metricDao.getByField("patientId", patientId);
        MetricUtils.sortByTimestampDesc(metrics);
        return metrics;
    }
}
