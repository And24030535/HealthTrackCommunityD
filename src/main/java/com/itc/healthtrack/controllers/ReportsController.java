package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.MetricDAO;
import com.itc.healthtrack.dao.PatientDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador encargado de exportar el historial clinico a formatos de reporte (PDF).
 */
public class ReportsController {

    @FXML private ComboBox<User> comboPatients;
    @FXML private Label lblStatus;

    private final PatientDAO patientDAO = new PatientDAO();
    private final MetricDAO metricDAO = new MetricDAO();
    private User loggedInDoctor;

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        if ("patient".equals(doctor.getRole())) {
            comboPatients.getItems().add(doctor);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
        } else {
            loadPatients();
        }
    }

    /**
     * Carga la lista de pacientes en el menu desplegable.
     */
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> patients = patientDAO.getPatientsByDoctor(loggedInDoctor.getUid());
                Platform.runLater(() -> {
                    comboPatients.setItems(FXCollections.observableArrayList(patients));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onExportPDF() {
        User selectedPatient = comboPatients.getValue();

        if (selectedPatient == null) {
            lblStatus.setText("Por favor, selecciona un paciente primero.");
            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        // 1. Abrir la ventana del sistema operativo para elegir donde guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        // Nombre sugerido por defecto
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".pdf");

        // Obtener la ventana actual
        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // 2. Si el usuario eligio una ruta y le dio a "Guardar"
        if (file != null) {
            lblStatus.setText("Descargando métricas...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            new Thread(() -> {
                try {
                    // Descargar todo el historial del paciente
                    List<Metric> history = metricDAO.getMetricsByPatient(selectedPatient.getUid());

                    // Los snapshots de gráficos deben tomarse en el hilo FX
                    Platform.runLater(() -> {
                        try {
                            lblStatus.setText("Generando gráficos...");
                            List<byte[]> chartImages = buildChartImages(history);

                            // Construir el archivo fisico en hilo secundario
                            new Thread(() -> {
                                try {
                                    generatePDF(file.getAbsolutePath(), selectedPatient, history, chartImages);
                                    Platform.runLater(() -> {
                                        lblStatus.setText("¡PDF guardado exitosamente en tu computadora!");
                                        lblStatus.setTextFill(javafx.scene.paint.Color.GREEN);
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        lblStatus.setText("Error crítico al generar el PDF.");
                                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                                    });
                                    e.printStackTrace();
                                }
                            }).start();

                        } catch (Exception e) {
                            lblStatus.setText("Error al generar los gráficos.");
                            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error crítico al generar el PDF.");
                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Utiliza la libreria iText para dibujar los elementos dentro del PDF.
     * Incluye una tabla con el historial y, si estan disponibles, graficos embebidos.
     */
    private void generatePDF(String destPath, User patient, List<Metric> history,
                              List<byte[]> chartImages) throws Exception {
        // Inicializar el escritor de PDF
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Escribir el encabezado del documento
        document.add(new Paragraph("Reporte Clínico - HealthTrack Community").setBold().setFontSize(18));
        document.add(new Paragraph("Paciente: " + patient.getFirstName() + " " + patient.getLastName()));
        if (loggedInDoctor != null
                && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
            document.add(new Paragraph("Médico a cargo: " + loggedInDoctor.getFirstName() + " " + loggedInDoctor.getLastName()));
        }
        document.add(new Paragraph(" ")); // Salto de linea

        // Configurar una tabla con 5 columnas
        float[] columnWidths = {130f, 100f, 60f, 80f, 80f};
        Table table = new Table(columnWidths);

        // Dibujar los encabezados de la tabla
        table.addHeaderCell("Fecha y Hora");
        table.addHeaderCell("Presión (Sis/Dia)");
        table.addHeaderCell("Pulso");
        table.addHeaderCell("Glucosa");
        table.addHeaderCell("Peso (kg)");

        // Iterar sobre las metricas y agregarlas como filas a la tabla
        for (Metric m : history) {
            String date = m.getTimestamp() != null ? m.getTimestamp().toDate().toString() : "N/A";
            String bp = (m.getSystolic() != null && m.getDiastolic() != null) ? m.getSystolic() + "/" + m.getDiastolic() : "-";
            String pulse = m.getHeartRate() != null ? String.valueOf(m.getHeartRate()) : "-";
            String glucose = m.getGlucoseLevel() != null ? String.valueOf(m.getGlucoseLevel()) : "-";
            String weight = m.getWeight() != null ? String.valueOf(m.getWeight()) : "-";

            table.addCell(date);
            table.addCell(bp);
            table.addCell(pulse);
            table.addCell(glucose);
            table.addCell(weight);
        }

        // Insertar la tabla en el documento
        document.add(table);

        // Insertar los graficos embebidos si estan disponibles
        if (chartImages != null && !chartImages.isEmpty()) {
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Gráficos del Historial Clínico").setBold().setFontSize(14));
            for (byte[] imageData : chartImages) {
                ImageData imgData = ImageDataFactory.create(imageData);
                Image pdfImage = new Image(imgData);
                pdfImage.setAutoScale(true);
                document.add(pdfImage);
                document.add(new Paragraph(" "));
            }
        }

        document.close();
    }

    /**
     * Creates a blood-pressure LineChart and a metric-averages BarChart from the patient
     * history and returns them as PNG byte arrays ready to embed in iText.
     * Must be called on the JavaFX Application Thread.
     */
    private List<byte[]> buildChartImages(List<Metric> history) {
        List<byte[]> images = new ArrayList<>();

        // --- Blood Pressure Line Chart ---
        try {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Evolución de Presión Arterial");
            lineChart.setAnimated(false);
            lineChart.setPrefSize(620, 280);

            XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
            systolicSeries.setName("Sistólica");
            XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
            diastolicSeries.setName("Diastólica");

            for (int i = history.size() - 1; i >= 0; i--) {
                Metric m = history.get(i);
                if (m.getSystolic() != null && m.getDiastolic() != null && m.getTimestamp() != null) {
                    String label = m.getTimestamp().toDate().toString().substring(4, 10);
                    systolicSeries.getData().add(new XYChart.Data<>(label, m.getSystolic()));
                    diastolicSeries.getData().add(new XYChart.Data<>(label, m.getDiastolic()));
                }
            }

            lineChart.getData().addAll(systolicSeries, diastolicSeries);
            byte[] lineBytes = snapshotNodeToBytes(lineChart, 620, 280);
            if (lineBytes != null) images.add(lineBytes);

        } catch (Exception e) {
            System.err.println("Error generating line chart: " + e.getMessage());
        }

        // --- Metric Averages Bar Chart ---
        try {
            CategoryAxis xAxis2 = new CategoryAxis();
            NumberAxis yAxis2 = new NumberAxis();
            BarChart<String, Number> barChart = new BarChart<>(xAxis2, yAxis2);
            barChart.setTitle("Promedios del Historial");
            barChart.setAnimated(false);
            barChart.setPrefSize(620, 280);

            XYChart.Series<String, Number> avgSeries = new XYChart.Series<>();
            avgSeries.setName("Promedio");

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

            if (sysCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Sistólica",   sysTotal    / (double) sysCount));
            if (diaCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Diastólica",  diaTotal    / (double) diaCount));
            if (hrCount > 0)     avgSeries.getData().add(new XYChart.Data<>("F.Cardíaca",  hrTotal     / (double) hrCount));
            if (glCount > 0)     avgSeries.getData().add(new XYChart.Data<>("Glucosa",     glTotal     / (double) glCount));
            if (weightCount > 0) avgSeries.getData().add(new XYChart.Data<>("Peso (kg)",   weightTotal / (double) weightCount));

            barChart.getData().add(avgSeries);
            byte[] barBytes = snapshotNodeToBytes(barChart, 620, 280);
            if (barBytes != null) images.add(barBytes);

        } catch (Exception e) {
            System.err.println("Error generating bar chart: " + e.getMessage());
        }

        return images;
    }

    /**
     * Renders a JavaFX node into a temporary Scene so CSS is applied,
     * takes a snapshot, and returns the result as a PNG byte array.
     * Must be called on the JavaFX Application Thread.
     */
    private byte[] snapshotNodeToBytes(javafx.scene.Node node, double width, double height) {
        try {
            StackPane wrapper = new StackPane(node);
            // Placing node inside a Scene triggers CSS layout
            javafx.scene.Scene tempScene = new javafx.scene.Scene(wrapper, width, height);
            node.applyCss();
            wrapper.layout();

            SnapshotParameters params = new SnapshotParameters();
            WritableImage writableImage = node.snapshot(params, null);

            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("Error capturing chart snapshot: " + e.getMessage());
            return null;
        }
    }
    @FXML
    protected void onExportExcel() {
        User selectedPatient = comboPatients.getValue();

        if (selectedPatient == null) {
            lblStatus.setText("Por favor, selecciona un paciente primero.");
            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico en Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".xlsx");

        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            lblStatus.setText("Generando archivo Excel...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            new Thread(() -> {
                try {
                    List<Metric> history = metricDAO.getMetricsByPatient(selectedPatient.getUid());
                    generateExcel(file.getAbsolutePath(), selectedPatient, history);

                    Platform.runLater(() -> {
                        lblStatus.setText("¡Excel guardado exitosamente en tu computadora!");
                        lblStatus.setTextFill(javafx.scene.paint.Color.GREEN);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error crítico al generar el Excel.");
                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Utiliza la libreria Apache POI para construir y exportar la hoja de calculo.
     */
    private void generateExcel(String destPath, User patient, List<Metric> history) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Historial Clínico");

        // Estilo para los encabezados
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        // Informacion del paciente
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Reporte Clínico - HealthTrack Community");

        Row patientRow = sheet.createRow(1);
        patientRow.createCell(0).setCellValue("Paciente: " + patient.getFirstName() + " " + patient.getLastName());

        if (loggedInDoctor != null
                && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
            Row doctorRow = sheet.createRow(2);
            doctorRow.createCell(0).setCellValue("Médico a cargo: " + loggedInDoctor.getFirstName() + " " + loggedInDoctor.getLastName());
        }

        // Encabezados de la tabla
        Row headerRow = sheet.createRow(4);
        String[] columns = {"Fecha y Hora", "Presión (Sis/Dia)", "Pulso", "Glucosa", "Peso (kg)"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenado de datos
        int rowNum = 5;
        for (Metric m : history) {
            Row row = sheet.createRow(rowNum++);

            String date = m.getTimestamp() != null ? m.getTimestamp().toDate().toString() : "N/A";
            String bp = (m.getSystolic() != null && m.getDiastolic() != null) ? m.getSystolic() + "/" + m.getDiastolic() : "-";
            String pulse = m.getHeartRate() != null ? String.valueOf(m.getHeartRate()) : "-";
            String glucose = m.getGlucoseLevel() != null ? String.valueOf(m.getGlucoseLevel()) : "-";
            String weight = m.getWeight() != null ? String.valueOf(m.getWeight()) : "-";

            row.createCell(0).setCellValue(date);
            row.createCell(1).setCellValue(bp);
            row.createCell(2).setCellValue(pulse);
            row.createCell(3).setCellValue(glucose);
            row.createCell(4).setCellValue(weight);
        }

        // Ajuste automatico del ancho de las columnas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Escritura del archivo fisico
        try (FileOutputStream fileOut = new FileOutputStream(destPath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}