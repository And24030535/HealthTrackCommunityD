package com.itc.healthtrack.dao;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Gestiona las operaciones de lectura y escritura para la coleccion 'recommendations'.
 */
public class RecommendationDAO {

    private final Firestore db;

    public RecommendationDAO() {
        this.db = FirebaseConnection.getInstance().getFirestore();
    }

    /**
     * Inserta una nueva alerta o recomendacion en la base de datos.
     */
    public void saveRecommendation(Recommendation recommendation) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("recommendations").document();
        recommendation.setId(docRef.getId());

        ApiFuture<WriteResult> result = docRef.set(recommendation);
        result.get();
    }

    /**
     * Obtiene las recomendaciones de un paciente, de la mas reciente a la mas antigua.
     */
    public List<Recommendation> getRecommendationsByPatient(String patientId) throws ExecutionException, InterruptedException {
        List<Recommendation> recList = new ArrayList<>();

        Query query = db.collection("recommendations")
                .whereEqualTo("patientId", patientId)
                .orderBy("generatedAt", Query.Direction.DESCENDING);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Recommendation rec = document.toObject(Recommendation.class);
            recList.add(rec);
        }

        return recList;
    }
}