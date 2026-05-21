package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Controlador encargado de exportar el historial clinico a formatos de reporte (PDF)
public class ReportsController {

    // Elementos de interfaz
    @FXML private ComboBox<User> comboPatients;  // ComboBox para seleccionar el paciente
    @FXML private Label lblStatus;               // Etiqueta para mensajes de estado/progreso

    // Acceso a datos
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private final GenericDAO<Metric> metricDao = new GenericDAO<>(Metric.class, "metrics");
    private User loggedInDoctor;

    /*Inicializa el controlador con los datos del usuario logeado
     Si es un paciente, muestra solo sus propios datos
     Si es médico/admin, carga la lista de pacientes*/
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

    // Carga la lista de pacientes en el menu desplegable
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> patients = getPatientsForUser(loggedInDoctor);
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

        // Abre un cuadro de diálogo del sistema operativo para elegir dónde guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        // Nombre sugerido por defecto
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".pdf");

        // Obtener la ventana actual
        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // Si el usuario eligió una ruta y presionó "Guardar"
        if (file != null) {
            lblStatus.setText("Descargando métricas...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            new Thread(() -> {
                try {
                    // Descargar todo el historial del paciente
                    List<Metric> history = getMetricsByPatientId(selectedPatient.getUid());

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

    /*Utiliza la libreria iText para dibujar los elementos dentro del PDF
     Incluye una tabla con el historial y, si estan disponibles, graficos*/
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

    /*Crea un gráfico de línea para presión arterial y un gráfico de barras para promedios.
     Convierte los gráficos a imágenes PNG para incrustarlos en el PDF
     Debe ser llamado desde el hilo de aplicación de JavaFX*/
    private List<byte[]> buildChartImages(List<Metric> history) {
        List<byte[]> images = new ArrayList<>();

        // Gráfico presión arterial
        try {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Evolución de Presión Arterial");
            lineChart.setAnimated(false);
            lineChart.setPrefSize(620, 280);

            //Descripciones sistolica y diastolica
            XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
            systolicSeries.setName("Sistólica");
            XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
            diastolicSeries.setName("Diastólica");

            // Llenar las descripciones con datos (en orden inverso para mostrar antiguos a la izquierda)
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

        // Gráfico de barras para promedios
        try {
            CategoryAxis xAxis2 = new CategoryAxis();
            NumberAxis yAxis2 = new NumberAxis();
            BarChart<String, Number> barChart = new BarChart<>(xAxis2, yAxis2);
            barChart.setTitle("Promedios del Historial");
            barChart.setAnimated(false);
            barChart.setPrefSize(620, 280);

            XYChart.Series<String, Number> avgSeries = new XYChart.Series<>();
            avgSeries.setName("Promedio");

            // Calcular promedios de cada métrica
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

            // Agregar los promedios calculados al gráfico
            if (sysCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Sistólica",   sysTotal    / (double) sysCount));
            if (diaCount > 0)    avgSeries.getData().add(new XYChart.Data<>("Diastólica",  diaTotal    / (double) diaCount));
            if (hrCount > 0)     avgSeries.getData().add(new XYChart.Data<>("F.Cardíaca",  hrTotal     / (double) hrCount));
            if (glCount > 0)     avgSeries.getData().add(new XYChart.Data<>("Glucosa",     glTotal     / (double) glCount));
            if (weightCount > 0) avgSeries.getData().add(new XYChart.Data<>("Peso (kg)",   weightTotal / (double) weightCount));

            barChart.getData().add(avgSeries);
            byte[] barBytes = snapshotNodeToBytes(barChart, 620, 280);
            if (barBytes != null) images.add(barBytes);

        } catch (Exception e) {
            System.err.println("Error generando gráfico de barras: " + e.getMessage());
        }

        return images;
    }

    /*
     Renderiza un nodo de JavaFX en una escena temporal para aplicar CSS,
     toma una captura de pantalla y la retorna como un array de bytes PNG
     Debe ser llamado desde el hilo de aplicación de JavaFX*/
    private byte[] snapshotNodeToBytes(javafx.scene.Node node, double width, double height) {
        try {
            StackPane wrapper = new StackPane(node);
            // Colocar el nodo dentro de una escena activa la aplicación de CSS
            javafx.scene.Scene tempScene = new javafx.scene.Scene(wrapper, width, height);
            node.applyCss();
            wrapper.layout();

            SnapshotParameters params = new SnapshotParameters();
            WritableImage writableImage = node.snapshot(params, null);

            // Convertir la imagen de JavaFX a BufferedImage de Swing
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("Error capturando snapshot del gráfico: " + e.getMessage());
            return null;
        }
    }

    //Exporta el historial clínico a un archivo Excel (.xlsx) con formato
    @FXML
    protected void onExportExcel() {
        User selectedPatient = comboPatients.getValue();

        if (selectedPatient == null) {
            lblStatus.setText("Por favor, selecciona un paciente primero.");
            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        // Abrir cuadro de diálogo para elegir dónde guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Clínico en Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Historial_" + selectedPatient.getFirstName() + ".xlsx");

        Stage stage = (Stage) comboPatients.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            lblStatus.setText("Generando archivo Excel...");
            lblStatus.setTextFill(javafx.scene.paint.Color.WHITE);

            // Generar el archivo en hilo de fondo
            new Thread(() -> {
                try {
                    List<Metric> history = getMetricsByPatientId(selectedPatient.getUid());
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

    /*Utiliza la librería Apache POI para construir y exportar un archivo Excel
    Incluye información del paciente, médico y tabla con el historial de métricas  */
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

        // Información del médico si está disponible
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

    // Obtiene la lista de pacientes visibles para el usuario logeado
    private List<User> getPatientsForUser(User user) throws Exception {
        // Lista final de pacientes
        List<User> result = new ArrayList<>();
        // Consulta todos los usuarios con rol de paciente
        List<User> patients = userDao.getByField("role", "patient");
        // Filtra según el rol del usuario actual
        for (User patient : patients) {
            if ("admin".equals(user.getRole())) {
                result.add(patient);
            } else if (user.getUid() != null && user.getUid().equals(patient.getAssignedDoctorId())) {
                result.add(patient);
            }
        }
        // Devuelve la lista filtrada
        return result;
    }

    // Obtiene el historial de métricas de un paciente y lo ordena por fecha
    private List<Metric> getMetricsByPatientId(String patientId) throws Exception {
        // Consulta todas las métricas del paciente
        List<Metric> metrics = metricDao.getByField("patientId", patientId);
        // Ordena las métricas de más reciente a más antigua
        sortMetricsByTimestamp(metrics);
        // Devuelve la lista ordenada
        return metrics;
    }

    // Ordena las métricas por fecha descendente
    private void sortMetricsByTimestamp(List<Metric> metrics) {
        Collections.sort(metrics, new Comparator<Metric>() {
            @Override
            public int compare(Metric first, Metric second) {
                if (first.getTimestamp() == null && second.getTimestamp() == null) {
                    return 0;
                }
                if (first.getTimestamp() == null) {
                    return 1;
                }
                if (second.getTimestamp() == null) {
                    return -1;
                }
                return second.getTimestamp().compareTo(first.getTimestamp());
            }
        });
    }
}
