package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// utilidad para operaciones comunes sobre listas de metricas
public class MetricUtils {

    private MetricUtils() {}

    // ordena de mas reciente a mas antigua y las que no tienen fecha quedan al final
    public static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }
}
