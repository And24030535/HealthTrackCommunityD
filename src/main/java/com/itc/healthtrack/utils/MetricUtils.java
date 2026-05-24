package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// utilidad con operaciones comunes sobre listas de metricas centraliza el orden y los promedios que antes estaban duplicados en MetricsController y ReportsController
public class MetricUtils {

    private MetricUtils() {}

    // ordena las metricas de mas reciente a mas antigua las que no tienen timestamp van al final
    public static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }

    // calcula los promedios de las cinco metricas principales devuelve null en los campos sin datos la presion solo cuenta cuando sistolica y diastolica estan presentes
    public static Averages computeAverages(List<Metric> data) {
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
    public static class Averages {
        public Double systolicAvg;
        public Double diastolicAvg;
        public Double heartRateAvg;
        public Double glucoseAvg;
        public Double weightAvg;
    }
}
