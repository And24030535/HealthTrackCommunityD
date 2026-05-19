package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Treatment;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Treatment
 * Gestiona operaciones CRUD para tratamientos médicos
 */
public class TreatmentDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Treatment";

    public void createTreatment(Treatment treatment) throws Exception {
        if (treatment == null || treatment.getId() == null) {
            throw new IllegalArgumentException("El tratamiento y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(treatment.getId()).set(treatment).get();
    }

    public Treatment getTreatmentById(String treatmentId) throws Exception {
        if (treatmentId == null || treatmentId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(treatmentId).get().get().toObject(Treatment.class);
    }

    public List<Treatment> getTreatmentsByPatient(String patientId) throws Exception {
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        List<Treatment> treatments = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Treatment treatment = querySnapshot.getDocuments().get(i).toObject(Treatment.class);
            treatments.add(treatment);
        }
        return treatments;
    }

    public List<Treatment> getTreatmentsByDoctor(String doctorId) throws Exception {
        if (doctorId == null || doctorId.isEmpty()) {
            throw new IllegalArgumentException("El ID del doctor no puede ser nulo");
        }
        List<Treatment> treatments = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Treatment treatment = querySnapshot.getDocuments().get(i).toObject(Treatment.class);
            treatments.add(treatment);
        }
        return treatments;
    }

    public void updateTreatment(Treatment treatment) throws Exception {
        if (treatment == null || treatment.getId() == null) {
            throw new IllegalArgumentException("El tratamiento y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(treatment.getId()).set(treatment).get();
    }

    public void deleteTreatment(String treatmentId) throws Exception {
        if (treatmentId == null || treatmentId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(treatmentId).delete().get();
    }
}

