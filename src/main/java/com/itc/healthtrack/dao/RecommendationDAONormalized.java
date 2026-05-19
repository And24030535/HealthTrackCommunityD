package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Recommendation
 * Gestiona operaciones CRUD para recomendaciones médicas
 */
public class RecommendationDAONormalized {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Recommendation";

    public void createRecommendation(Recommendation recommendation) throws Exception {
        if (recommendation == null || recommendation.getId() == null) {
            throw new IllegalArgumentException("La recomendación y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(recommendation.getId()).set(recommendation).get();
    }

    public Recommendation getRecommendationById(String recommendationId) throws Exception {
        if (recommendationId == null || recommendationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(recommendationId).get().get().toObject(Recommendation.class);
    }

    // Obtener recomendaciones de un paciente ordenadas por fecha descendente
    public List<Recommendation> getRecommendationsByPatient(String patientId) throws Exception {
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        List<Recommendation> recommendations = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Recommendation rec = querySnapshot.getDocuments().get(i).toObject(Recommendation.class);
            recommendations.add(rec);
        }
        return recommendations;
    }

    public void updateRecommendation(Recommendation recommendation) throws Exception {
        if (recommendation == null || recommendation.getId() == null) {
            throw new IllegalArgumentException("La recomendación y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(recommendation.getId()).set(recommendation).get();
    }

    public void deleteRecommendation(String recommendationId) throws Exception {
        if (recommendationId == null || recommendationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(recommendationId).delete().get();
    }
}

