package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Medication;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Medication
 * Gestiona operaciones CRUD para medicamentos
 */
public class MedicationDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Medication";

    public void createMedication(Medication medication) throws Exception {
        if (medication == null || medication.getId() == null) {
            throw new IllegalArgumentException("El medicamento y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(medication.getId()).set(medication).get();
    }

    public Medication getMedicationById(String medicationId) throws Exception {
        if (medicationId == null || medicationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(medicationId).get().get().toObject(Medication.class);
    }

    public List<Medication> getAllMedications() throws Exception {
        List<Medication> medications = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Medication medication = querySnapshot.getDocuments().get(i).toObject(Medication.class);
            medications.add(medication);
        }
        return medications;
    }

    public void updateMedication(Medication medication) throws Exception {
        if (medication == null || medication.getId() == null) {
            throw new IllegalArgumentException("El medicamento y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(medication.getId()).set(medication).get();
    }

    public void deleteMedication(String medicationId) throws Exception {
        if (medicationId == null || medicationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(medicationId).delete().get();
    }
}

