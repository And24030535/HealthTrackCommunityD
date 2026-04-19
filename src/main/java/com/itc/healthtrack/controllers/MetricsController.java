package com.itc.healthtrack.controllers;

import com.google.cloud.Timestamp;
import com.itc.healthtrack.dao.MetricDAO;
import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class MetricsController {

    // Componentes de la interfaz de usuario
    @FXML private ComboBox<User> comboPatients;
    @FXML private TextField txtSystolic, txtDiastolic, txtPulse, txtGlucose, txtWeight;
    @FXML private Button btnSave;
    @FXML private Label lblStatus;

    // Filtros y Promedios
    @FXML private ComboBox<String> comboTimeFilter;
    @FXML private Label lblAvgBP, lblAvgGlucose, lblAvgWeight;

    @FXML private TableView<Metric> tableMetrics;
    @FXML private TableColumn<Metric, String> colDate, colSysDia, colPulse, colGlucose, colWeight;
    @FXML private LineChart<String, Number> evolutionChart;

    // Acceso a la base de datos y variables de estado
    private final PatientDAO patientDAO = new PatientDAO();
    private final MetricDAO metricDAO = new MetricDAO();
    private final ObservableList<Metric> metricsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private Metric selectedMetric = null;
    private List<Metric> currentPatientHistory = new ArrayList<>(); // Respaldo del historial completo

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupTable();
        loadPatientsIntoCombo();
        setupFilters();

        // Escucha los cambios de selección de paciente
        comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onClearForm();
                loadMetricsForPatient(newVal.getUid());
            }
        });
    }

    private void setupFilters() {
        comboTimeFilter.setItems(FXCollections.observableArrayList("Histórico Completo", "Últimos 7 Días", "Últimos 30 Días"));
        comboTimeFilter.getSelectionModel().selectFirst();
        comboTimeFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyTimeFilter();
        });
    }

    private void setupTable() {
        // Configura el formato de las columnas
        colDate.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getTimestamp();
            return new SimpleStringProperty(ts != null ? ts.toDate().toString().substring(0, 19) : "N/A");
        });

        colSysDia.setCellValueFactory(cellData -> {
            Integer sys = cellData.getValue().getSystolic();
            Integer dia = cellData.getValue().getDiastolic();
            return new SimpleStringProperty((sys != null && dia != null) ? sys + "/" + dia : "-");
        });

        colPulse.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getPulse())));
        colGlucose.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getGlucoseLevel())));
        colWeight.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getWeight())));

        tableMetrics.setItems(metricsObservableList);

        // Llena el formulario al seleccionar una fila y cambia el contexto del botón
        tableMetrics.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedMetric = newSelection;
                txtSystolic.setText(newSelection.getSystolic() != null ? String.valueOf(newSelection.getSystolic()) : "");
                txtDiastolic.setText(newSelection.getDiastolic() != null ? String.valueOf(newSelection.getDiastolic()) : "");
                txtPulse.setText(newSelection.getPulse() != null ? String.valueOf(newSelection.getPulse()) : "");
                txtGlucose.setText(newSelection.getGlucoseLevel() != null ? String.valueOf(newSelection.getGlucoseLevel()) : "");
                txtWeight.setText(newSelection.getWeight() != null ? String.valueOf(newSelection.getWeight()) : "");

                btnSave.setText("Actualizar");
            }
        });
    }

    private void loadPatientsIntoCombo() {
        new Thread(() -> {
            try {
                List<User> patients = patientDAO.getPatientsByDoctor(loggedInDoctor.getUid());
                Platform.runLater(() -> comboPatients.setItems(FXCollections.observableArrayList(patients)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMetricsForPatient(String patientId) {
        new Thread(() -> {
            try {
                List<Metric> history = metricDAO.getMetricsByPatient(patientId);
                Platform.runLater(() -> {
                    currentPatientHistory = history; // Guardamos el respaldo
                    applyTimeFilter(); // Aplicamos el filtro que a su vez llena la tabla y grafica
                    lblStatus.setText("Historial cargado con éxito.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar el historial.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
            }
        }).start();
    }

    private void applyTimeFilter() {
        if (currentPatientHistory.isEmpty()) {
            metricsObservableList.clear();
            evolutionChart.getData().clear();
            calculateAverages(new ArrayList<>());
            return;
        }

        String filter = comboTimeFilter.getValue();
        long nowSeconds = System.currentTimeMillis() / 1000;
        long limitSeconds = 0;

        if ("Últimos 7 Días".equals(filter)) limitSeconds = 7 * 24 * 3600;
        else if ("Últimos 30 Días".equals(filter)) limitSeconds = 30 * 24 * 3600;

        List<Metric> filteredList = new ArrayList<>();
        for (Metric m : currentPatientHistory) {
            long metricTime = m.getTimestamp().getSeconds();
            if (limitSeconds == 0 || (nowSeconds - metricTime) <= limitSeconds) {
                filteredList.add(m);
            }
        }

        metricsObservableList.clear();
        metricsObservableList.addAll(filteredList);
        updateChart(filteredList);
        calculateAverages(filteredList);
    }

    private void calculateAverages(List<Metric> data) {
        if (data.isEmpty()) {
            lblAvgBP.setText("PA: --/-- mmHg");
            lblAvgGlucose.setText("Glucosa: -- mg/dL");
            lblAvgWeight.setText("Peso: -- kg");
            return;
        }

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

        lblAvgBP.setText(bpCount > 0 ? "PA: " + (sysTotal/bpCount) + "/" + (diaTotal/bpCount) + " mmHg" : "PA: --/-- mmHg");
        lblAvgGlucose.setText(glCount > 0 ? String.format("Glucosa: %.1f mg/dL", (glTotal/glCount)) : "Glucosa: -- mg/dL");
        lblAvgWeight.setText(weightCount > 0 ? String.format("Peso: %.1f kg", (weightTotal/weightCount)) : "Peso: -- kg");
    }

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

    @FXML
    protected void onSaveMetric() {
        User selectedPatient = comboPatients.getValue();
        if (selectedPatient == null) {
            lblStatus.setText("Error: Selecciona un paciente primero.");
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
            metricToProcess.setPulse(txtPulse.getText().isEmpty() ? null : Integer.parseInt(txtPulse.getText()));
            metricToProcess.setGlucoseLevel(txtGlucose.getText().isEmpty() ? null : Double.parseDouble(txtGlucose.getText()));
            metricToProcess.setWeight(txtWeight.getText().isEmpty() ? null : Double.parseDouble(txtWeight.getText()));

            lblStatus.setText(isNewRecord ? "Guardando..." : "Actualizando...");
            lblStatus.setTextFill(Color.web("#ffffff"));

            new Thread(() -> {
                try {
                    if (isNewRecord) {
                        metricDAO.saveMetric(metricToProcess);
                    } else {
                        metricDAO.updateMetric(metricToProcess);
                    }

                    Platform.runLater(() -> {
                        onClearForm();
                        loadMetricsForPatient(selectedPatient.getUid());
                        lblStatus.setText(isNewRecord ? "Métrica guardada." : "Métrica actualizada.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            lblStatus.setText("Error: Usa valores numéricos válidos.");
            lblStatus.setTextFill(Color.web("#ff5252"));
        }
    }

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
                metricDAO.deleteMetric(metricId);
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

    @FXML
    protected void onClearForm() {
        txtSystolic.clear();
        txtDiastolic.clear();
        txtPulse.clear();
        txtGlucose.clear();
        txtWeight.clear();

        selectedMetric = null;
        tableMetrics.getSelectionModel().clearSelection();

        btnSave.setText("Guardar");
    }
}