package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

/**
 * Utilidad para operaciones comunes sobre listas de métricas.
 *
 * Antes, MetricsController, RecommendationsController y ReportsController
 * tenían cada uno su propia copia del mismo método sortMetricsByTimestamp().
 * Esta clase centraliza esa lógica para evitar duplicación.
 */
public class MetricUtils {

    // Constructor privado: clase de utilidad, no se instancia
    private MetricUtils() {}

    /**
     * Ordena una lista de métricas de más reciente a más antigua (descendente).
     * Las métricas sin timestamp se colocan al final de la lista.
     *
     * @param metrics lista de métricas a ordenar (se modifica en el lugar)
     */
    public static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            // Si ambas no tienen timestamp, son iguales
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            // Las que no tienen timestamp van al final
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            // Orden descendente: b antes que a
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }
}
