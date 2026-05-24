package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// utilidad para construir el texto de alertas clinicas a partir del historial del paciente centraliza los umbrales para que ReportsController y RecommendationsController den el mismo resultado
public class AlertUtils {

    private AlertUtils() {}

    // arma el texto de alertas activas a partir de la metrica mas reciente evalua presion glucosa frecuencia cardiaca e imc la lista debe venir ordenada desc
    public static String buildAlertsText(List<Metric> history) {
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
}
