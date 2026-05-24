package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Recommendation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// utilidad para listas de recomendaciones centraliza el orden por fecha que antes estaba duplicado en RecommendationsController y ReportsController
public class RecommendationUtils {

    private RecommendationUtils() {}

    // ordena las recomendaciones de mas reciente a mas antigua las que no tienen fecha van al final
    public static void sortByDateDesc(List<Recommendation> recommendations) {
        Collections.sort(recommendations, new Comparator<Recommendation>() {
            @Override
            public int compare(Recommendation a, Recommendation b) {
                if (a.getGeneratedAt() == null && b.getGeneratedAt() == null) return 0;
                if (a.getGeneratedAt() == null) return 1;
                if (b.getGeneratedAt() == null) return -1;
                return b.getGeneratedAt().compareTo(a.getGeneratedAt());
            }
        });
    }
}
