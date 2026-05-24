package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// Utilidad para operaciones comunes sobre listas de métricas.
// Centraliza el ordenamiento y el cálculo de promedios que antes estaban
// duplicados en MetricsController y ReportsController.
public class MetricUtils {

    // Constructor privado: clase de utilidad, no se instancia
    private MetricUtils() {}

    // Ordena una lista de métricas de más reciente a más antigua (descendente).
    // Las métricas sin timestamp se colocan al final de la lista.
    public static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }

    // Calcula los promedios de las cinco métricas principales del historial recibido.
    // Devuelve un objeto Averages con cada promedio como Double (null cuando no hubo datos).
    // Una lectura de presión arterial sólo cuenta cuando sistólica y diastólica están presentes.
    public static Averages computeAverages(List<Metric> data) {
        Averages result = new Averages();
        if (data == null || data.isEmpty()) return result;

        int sysTotal = 0, diaTotal = 0, hrTotal = 0;
        double glTotal = 0, weightTotal = 0;
        int bpCount = 0, hrCount = 0, glCount = 0, weightCount = 0;

        for (Metric m : data) {
            // Sistólica + Diastólica: sólo cuenta como lectura cuando ambas están presentes
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

    // Contenedor simple con los promedios calculados. Un campo en null indica
    // que no había datos suficientes para calcular ese promedio.
    public static class Averages {
        public Double systolicAvg;
        public Double diastolicAvg;
        public Double heartRateAvg;
        public Double glucoseAvg;
        public Double weightAvg;
    }
}
