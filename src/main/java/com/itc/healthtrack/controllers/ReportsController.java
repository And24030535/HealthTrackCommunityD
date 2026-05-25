package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.Recommendation;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.UserService;
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
                    String alertsText          = buildAlertsText(history);
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
            Averages avg = computeAverages(history);
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
        sortByTimestampDesc(metrics);
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

    // arma el texto de alertas activas a partir de la metrica mas reciente evalua presion glucosa frecuencia cardiaca e imc la lista debe venir ordenada desc
    private static String buildAlertsText(List<Metric> history) {
        if (history == null || history.isEmpty()) {
            return "Sin métricas registradas — no se pueden calcular alertas.";
        }

        Metric latest = history.get(0);
        StringBuilder sb = new StringBuilder();

        // presion arterial
        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            if (sys >= 180 || dia >= 120) {
                sb.append("• CRÍTICO — Hipertensión en crisis (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            } else if (sys >= 140 || dia >= 90) {
                sb.append("• ALERTA — Hipertensión (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            } else if (sys >= 130 || dia >= 80) {
                sb.append("• AVISO — Prehipertensión (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            }
        }

        // glucosa
        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            if (gluc > 300) {
                sb.append("• CRÍTICO — Glucosa extrema (").append(gluc)
                  .append(" mg/dL) — riesgo de cetoacidosis\n");
            } else if (gluc > 125) {
                sb.append("• ALERTA — Hiperglucemia (").append(gluc).append(" mg/dL)\n");
            } else if (gluc < 70) {
                sb.append("• ALERTA — Hipoglucemia (").append(gluc).append(" mg/dL)\n");
            }
        }

        // frecuencia cardiaca
        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            if (hr > 120) {
                sb.append("• ALERTA — Taquicardia (").append(hr).append(" lpm)\n");
            } else if (hr < 50) {
                sb.append("• ALERTA — Bradicardia (").append(hr).append(" lpm)\n");
            }
        }

        // indice de masa corporal
        if (latest.getBmi() != null) {
            double bmi = latest.getBmi();
            if (bmi >= 40) {
                sb.append("• ALERTA — Obesidad mórbida (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 35) {
                sb.append("• ALERTA — Obesidad severa (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 30) {
                sb.append("• AVISO — Obesidad clase I (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 25) {
                sb.append("• AVISO — Sobrepeso (IMC: ").append(bmi).append(")\n");
            } else if (bmi < 18.5) {
                sb.append("• AVISO — Bajo peso (IMC: ").append(bmi).append(")\n");
            }
        }

        if (sb.length() == 0) {
            return "No se detectaron valores fuera del rango clínico normal.";
        }
        return sb.toString().trim();
    }

    // ordena las metricas de mas reciente a mas antigua las que no tienen timestamp van al final
    private static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }

    // calcula los promedios de las cinco metricas principales devuelve null en los campos sin datos la presion solo cuenta cuando sistolica y diastolica estan presentes
    private static Averages computeAverages(List<Metric> data) {
        Averages result = new Averages();
        if (data == null || data.isEmpty()) return result;

        int sysTotal = 0, diaTotal = 0, hrTotal = 0;
        double glTotal = 0, weightTotal = 0;
        int bpCount = 0, hrCount = 0, glCount = 0, weightCount = 0;

        for (Metric m : data) {
            // sistolica y diastolica solo cuentan cuando ambas vienen juntas
            if (m.getSystolic() != null && m.getDiastolic() != null) {
                sysTotal += m.getSystolic();
                diaTotal += m.getDiastolic();
                bpCount++;
            }
            if (m.getHeartRate() != null) {
                hrTotal += m.getHeartRate();
                hrCount++;
            }
            if (m.getGlucoseLevel() != null) {
                glTotal += m.getGlucoseLevel();
                glCount++;
            }
            if (m.getWeight() != null) {
                weightTotal += m.getWeight();
                weightCount++;
            }
        }

        if (bpCount > 0) {
            result.systolicAvg  = sysTotal / (double) bpCount;
            result.diastolicAvg = diaTotal / (double) bpCount;
        }
        if (hrCount > 0)     result.heartRateAvg = hrTotal     / (double) hrCount;
        if (glCount > 0)     result.glucoseAvg   = glTotal     / glCount;
        if (weightCount > 0) result.weightAvg    = weightTotal / weightCount;
        return result;
    }

    // contenedor con los promedios calculados un campo null significa que no habia datos
    private static class Averages {
        public Double systolicAvg;
        public Double diastolicAvg;
        public Double heartRateAvg;
        public Double glucoseAvg;
        public Double weightAvg;
    }
}
