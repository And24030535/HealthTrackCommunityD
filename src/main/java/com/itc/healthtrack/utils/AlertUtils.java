package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// Utilidad para construir el texto de alertas clínicas a partir del historial del paciente.
// Centraliza los umbrales que antes estaban duplicados en ReportsController
// y RecommendationsController, garantizando consistencia entre ambos reportes.
public class AlertUtils {

    // Constructor privado: clase de utilidad, no se instancia
    private AlertUtils() {}

    // Construye un texto con las alertas activas a partir de la última métrica del paciente.
    // Evalúa presión arterial, glucosa, frecuencia cardíaca e IMC con umbrales clínicos estándar.
    // La lista debe venir ordenada de más reciente a más antigua (usar MetricUtils.sortByTimestampDesc).
    public static String buildAlertsText(List<Metric> history) {
        if (history == null || history.isEmpty()) {
            return "Sin métricas registradas — no se pueden calcular alertas.";
        }

        Metric latest = history.get(0);
        StringBuilder sb = new StringBuilder();

        // Presión arterial
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

        // Glucosa
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

        // Frecuencia cardíaca
        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            if (hr > 120) {
                sb.append("• ALERTA — Taquicardia (").append(hr).append(" lpm)\n");
            } else if (hr < 50) {
                sb.append("• ALERTA — Bradicardia (").append(hr).append(" lpm)\n");
            }
        }

        // Índice de masa corporal
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
}
