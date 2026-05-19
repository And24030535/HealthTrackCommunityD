package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.HealthMetric;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad HealthMetric
 * Gestiona operaciones CRUD para métricas de salud
 */
public class HealthMetricDAONormalized {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "HealthMetric";

    // Crear métrica
    public void createHealthMetric(HealthMetric metric) throws Exception {
        if (metric == null || metric.getId() == null) {
            throw new IllegalArgumentException("La métrica y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(metric.getId()).set(metric).get();
    }

    // Obtener por ID
    public HealthMetric getHealthMetricById(String metricId) throws Exception {
        if (metricId == null || metricId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(metricId).get().get().toObject(HealthMetric.class);
    }

    // Obtener métricas de un paciente
    public List<HealthMetric> getMetricsByPatient(String patientId) throws Exception {
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        List<HealthMetric> metrics = new ArrayList<>();
        // Ejecutamos consulta ordenada por timestamp descendente (más recientes primero)
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            HealthMetric metric = querySnapshot.getDocuments().get(i).toObject(HealthMetric.class);
            metrics.add(metric);
        }
        return metrics;
    }

    // Actualizar
    public void updateHealthMetric(HealthMetric metric) throws Exception {
        if (metric == null || metric.getId() == null) {
            throw new IllegalArgumentException("La métrica y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(metric.getId()).set(metric).get();
    }

    // Eliminar
    public void deleteHealthMetric(String metricId) throws Exception {
        if (metricId == null || metricId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(metricId).delete().get();
    }
}

