package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.PatientAllergy;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad PatientAllergy
 * Gestiona operaciones CRUD para la relación entre pacientes y alergias
 */
public class PatientAllergyDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "PatientAllergy";

    // Crear relación paciente-alergia
    public void createPatientAllergy(PatientAllergy patientAllergy) throws Exception {
        if (patientAllergy == null || patientAllergy.getId() == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(patientAllergy.getId()).set(patientAllergy).get();
    }

    // Obtener por ID
    public PatientAllergy getPatientAllergyById(String id) throws Exception {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(id).get().get().toObject(PatientAllergy.class);
    }

    // Obtener alergias de un paciente
    public List<PatientAllergy> getAllergiesByPatient(String patientId) throws Exception {
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        List<PatientAllergy> patientAllergies = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            PatientAllergy patientAllergy = querySnapshot.getDocuments().get(i).toObject(PatientAllergy.class);
            patientAllergies.add(patientAllergy);
        }
        return patientAllergies;
    }

    // Actualizar
    public void updatePatientAllergy(PatientAllergy patientAllergy) throws Exception {
        if (patientAllergy == null || patientAllergy.getId() == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(patientAllergy.getId()).set(patientAllergy).get();
    }

    // Eliminar
    public void deletePatientAllergy(String id) throws Exception {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(id).delete().get();
    }
}

