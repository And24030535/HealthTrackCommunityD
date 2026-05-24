package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.Recommendation;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.UserService;
import com.itc.healthtrack.utils.AlertUtils;
import com.itc.healthtrack.utils.MetricUtils;
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

// controlador que exporta el historial clinico a pdf y excel
public class ReportsController {

    @FXML private ComboBox<User> comboPatients;
    @FXML private Label lblStatus;

    private final GenericDAO<User>           userDao           = new GenericDAO<>(User.class, "users");
    private final GenericDAO<Metric>         metricDao         = new GenericDAO<>(Metric.class, "metrics");
    // tambien cargamos las notas para incluir la ultima recomendacion en el pdf
    private final GenericDAO<Recommendation> recommendationDao = new GenericDAO<>(Recommendation.class, "notas");
    private final UserService userService = new UserService();
    private User loggedInDoctor;

    // arranca el controlador con el usuario logeado si es paciente ve solo sus datos si es medico o admin ve la lista de pacientes
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

    // carga los pacientes en el desplegable
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> patients = userService.getPatientsForUser(loggedInDoctor);
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

        // abrimos el dialogo del so para elegir donde guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        // nombre sugerido por defecto
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".pdf");

        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // si el usuario eligio ruta y presiono Guardar
        if (file != null) {
            lblStatus.setText("Descargando métricas...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            new Thread(() -> {
                try {
                    // bajamos todo el historial del paciente
                    List<Metric> history = getMetricsByPatientId(selectedPatient.getUid());

                    // calculamos alertas y traemos la ultima recomendacion en el mismo hilo de fondo para no bloquear la ui
                    String alertsText          = AlertUtils.buildAlertsText(history);
                    String recommendationText  = fetchLatestRecommendation(selectedPatient.getUid());

                    Platform.runLater(() -> {
                        try {
                            lblStatus.setText("Generando gráficos...");
                            List<byte[]> chartImages = buildChartImages(history);

                            // construimos el archivo fisico en hilo secundario
                            new Thread(() -> {
                                try {
                                    generatePDF(file.getAbsolutePath(), selectedPatient, history,
                                            chartImages, alertsText, recommendationText);
                                    Platform.runLater(() -> {
                                        lblStatus.setText("¡PDF guardado exitosamente en tu computadora!");
                                        lblStatus.setTextFill(javafx.scene.paint.Color.GREEN);
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        lblStatus.setText("Error al generar el PDF");
                                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                                    });
                                    e.printStackTrace();
                                }
                            }).start();

                        } catch (Exception e) {
                            lblStatus.setText("Error al generar los gráficos");
                            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al generar el PDF");
                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // construye el pdf con iText con tabla de metricas graficos alertas y recomendaciones
    private void generatePDF(String destPath, User patient, List<Metric> history,
                              List<byte[]> chartImages,
                              String alertsText, String recommendationText) throws Exception {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // encabezado del documento
        document.add(new Paragraph("Reporte Clínico - HealthTrack Community").setBold().setFontSize(18));
        document.add(new Paragraph("Paciente: " + patient.getFirstName() + " " + patient.getLastName()));
        if (loggedInDoctor != null
                && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
            document.add(new Paragraph("Médico a cargo: " + loggedInDoctor.getFirstName() + " " + loggedInDoctor.getLastName()));
        }
        // salto de linea
        document.add(new Paragraph(" "));

        // tabla con 5 columnas
        float[] columnWidths = {130f, 100f, 60f, 80f, 80f};
        Table table = new Table(columnWidths);

        // encabezados de la tabla
        table.addHeaderCell("Fecha y Hora");
        table.addHeaderCell("Presión (Sis/Dia)");
        table.addHeaderCell("Pulso");
        table.addHeaderCell("Glucosa");
        table.addHeaderCell("Peso (kg)");

        // iteramos sobre las metricas y las metemos como filas
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

        document.add(table);

        // insertamos los graficos embebidos si estan disponibles
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

        // seccion alertas detectadas
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Alertas Detectadas")
                .setBold().setFontSize(14));
        document.add(new Paragraph(alertsText != null ? alertsText : "Sin alertas.")
                .setFontSize(11));

        // seccion recomendaciones clinicas
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Recomendaciones Clínicas")
                .setBold().setFontSize(14));
        document.add(new Paragraph(recommendationText != null
                ? recommendationText
                : "No se ha generado ningún análisis para este paciente.")
                .setFontSize(11));

        document.close();
    }

    // genera los graficos de presion y promedios como png para meter al pdf
    private List<byte[]> buildChartImages(List<Metric> history) {
        List<byte[]> images = new ArrayList<>();

        // grafico de presion arterial
        try {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Evolución de Presión Arterial");
            lineChart.setAnimated(false);
            lineChart.setPrefSize(620, 280);

            // series sistolica y diastolica
            XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
            systolicSeries.setName("Sistólica");
            XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
            diastolicSeries.setName("Diastólica");

            // llenamos las series al reves para que los antiguos queden a la izquierda
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
            System.err.println("Error generando gráfico de línea: " + e.getMessage());
        }

        // grafico de barras con los promedios
        try {
            CategoryAxis xAxis2 = new CategoryAxis();
            NumberAxis yAxis2 = new NumberAxis();
            BarChart<String, Number> barChart = new BarChart<>(xAxis2, yAxis2);
            barChart.setTitle("Promedios del Historial");
            barChart.setAnimated(false);
            barChart.setPrefSize(620, 280);

            XYChart.Series<String, Number> avgSeries = new XYChart.Series<>();
            avgSeries.setName("Promedio");

            // delegamos el conteo y suma a MetricUtils solo agregamos los promedios con dato
            MetricUtils.Averages avg = MetricUtils.computeAverages(history);
            if (avg.systolicAvg  != null) avgSeries.getData().add(new XYChart.Data<>("Sistólica",   avg.systolicAvg));
            if (avg.diastolicAvg != null) avgSeries.getData().add(new XYChart.Data<>("Diastólica",  avg.diastolicAvg));
            if (avg.heartRateAvg != null) avgSeries.getData().add(new XYChart.Data<>("F.Cardíaca",  avg.heartRateAvg));
            if (avg.glucoseAvg   != null) avgSeries.getData().add(new XYChart.Data<>("Glucosa",     avg.glucoseAvg));
            if (avg.weightAvg    != null) avgSeries.getData().add(new XYChart.Data<>("Peso (kg)",   avg.weightAvg));

            barChart.getData().add(avgSeries);
            byte[] barBytes = snapshotNodeToBytes(barChart, 620, 280);
            if (barBytes != null) images.add(barBytes);

        } catch (Exception e) {
            System.err.println("Error generando gráfico de barras: " + e.getMessage());
        }

        return images;
    }

    // convierte un nodo de javafx a png tomandole una captura
    private byte[] snapshotNodeToBytes(javafx.scene.Node node, double width, double height) {
        try {
            StackPane wrapper = new StackPane(node);
            // meter el nodo en una escena activa la aplicacion del css
            javafx.scene.Scene tempScene = new javafx.scene.Scene(wrapper, width, height);
            node.applyCss();
            wrapper.layout();

            SnapshotParameters params = new SnapshotParameters();
            WritableImage writableImage = node.snapshot(params, null);

            // pasamos la imagen de javafx a BufferedImage de swing
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("Error capturando snapshot del gráfico: " + e.getMessage());
            return null;
        }
    }

    // exporta el historial clinico a un xlsx con formato
    @FXML
    protected void onExportExcel() {
        User selectedPatient = comboPatients.getValue();

        if (selectedPatient == null) {
            lblStatus.setText("Por favor, selecciona un paciente primero");
            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        // abrimos el dialogo para elegir donde guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico en Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".xlsx");

        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            lblStatus.setText("Generando archivo Excel...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            // generamos el archivo en hilo de fondo
            new Thread(() -> {
                try {
                    List<Metric> history = getMetricsByPatientId(selectedPatient.getUid());
                    generateExcel(file.getAbsolutePath(), selectedPatient, history);

                    Platform.runLater(() -> {
                        lblStatus.setText("¡Excel guardado exitosamente en tu computadora!");
                        lblStatus.setTextFill(javafx.scene.paint.Color.GREEN);
                    });
                } catch (java.io.FileNotFoundException e) {
                    // Windows lanza FileNotFoundException si el archivo esta abierto en Excel
                    Platform.runLater(() -> {
                        lblStatus.setText("Cierra el archivo en Excel y vuelve a intentarlo");
                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                    });
                    e.printStackTrace();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error crítico al generar el Excel");
                        lblStatus.setTextFill(javafx.scene.paint.Color.RED);
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // construye el xlsx con Apache POI con info del paciente y sus metricas el try-with-resources cierra el workbook aunque truene la escritura
    private void generateExcel(String destPath, User patient, List<Metric> history) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historial Clínico");

            // estilo para los encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // info del paciente
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Reporte Clínico - HealthTrack Community");

            Row patientRow = sheet.createRow(1);
            patientRow.createCell(0).setCellValue("Paciente: " + patient.getFirstName() + " " + patient.getLastName());

            // info del medico si esta disponible
            if (loggedInDoctor != null
                    && ("doctor".equals(loggedInDoctor.getRole()) || "admin".equals(loggedInDoctor.getRole()))) {
                Row doctorRow = sheet.createRow(2);
                doctorRow.createCell(0).setCellValue("Médico a cargo: " + loggedInDoctor.getFirstName() + " " + loggedInDoctor.getLastName());
            }

            // encabezados de la tabla
            Row headerRow = sheet.createRow(4);
            String[] columns = {"Fecha y Hora", "Presión (Sis/Dia)", "Pulso", "Glucosa", "Peso (kg)"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // llenado de datos
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

            // ajuste automatico del ancho de las columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // escritura del archivo fisico
            try (FileOutputStream fileOut = new FileOutputStream(destPath)) {
                workbook.write(fileOut);
            }
        }
    }

    // trae el historial de metricas del paciente ordenado por fecha
    private List<Metric> getMetricsByPatientId(String patientId) throws Exception {
        List<Metric> metrics = metricDao.getByField("patientId", patientId);
        MetricUtils.sortByTimestampDesc(metrics);
        return metrics;
    }

    // trae el analisis clinico mas reciente guardado en firestore excluye notas manuales del medico solo trae analisis automaticos
    private String fetchLatestRecommendation(String patientId) {
        try {
            List<Recommendation> all = recommendationDao.getByField("patientId", patientId);
            Recommendation latest = null;
            for (Recommendation r : all) {
                // solo consideramos los analisis automaticos no las notas del medico
                if ("note".equals(r.getType())) continue;
                if (latest == null) {
                    latest = r;
                } else if (r.getGeneratedAt() != null && latest.getGeneratedAt() != null
                        && r.getGeneratedAt().compareTo(latest.getGeneratedAt()) > 0) {
                    latest = r;
                }
            }
            return (latest != null && latest.getMessage() != null)
                    ? latest.getMessage()
                    : "No se ha generado ningún análisis clínico para este paciente aún";
        } catch (Exception e) {
            System.err.println("[ReportsController] Error al cargar recomendación: " + e.getMessage());
            return "No disponible (error al conectar con la base de datos)";
        }
    }
}
