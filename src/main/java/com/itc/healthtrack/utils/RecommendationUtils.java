package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Recommendation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Utilidad para operaciones comunes sobre listas de recomendaciones.
// Centraliza el ordenamiento por fecha que antes estaba duplicado en
// RecommendationsController y ReportsController.
public class RecommendationUtils {

    // Constructor privado: clase de utilidad, no se instancia
    private RecommendationUtils() {}

    // Ordena la lista de recomendaciones de la más reciente a la más antigua.
    // Las entradas sin fecha se colocan al final.
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
