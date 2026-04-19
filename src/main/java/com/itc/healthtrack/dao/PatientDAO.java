package com.itc.healthtrack.dao;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Gestiona las operaciones CRUD exclusivas para los pacientes en la clinica.
 * Los pacientes se almacenan en la coleccion 'users' pero con el rol 'patient'.
 */
public class PatientDAO {

    private final Firestore db;

    public PatientDAO() {
        this.db = FirebaseConnection.getInstance().getFirestore();
    }

    /**
     * Obtiene la lista de pacientes asignados a un medico en especifico.
     */
    public List<User> getPatientsByDoctor(String doctorId) throws ExecutionException, InterruptedException {
        List<User> patientsList = new ArrayList<>();

        // Consulta: Buscar usuarios que sean pacientes y esten asignados a este medico
        Query query = db.collection("users")
                .whereEqualTo("role", "patient")
                .whereEqualTo("assignedDoctorId", doctorId);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            User patient = document.toObject(User.class);
            patientsList.add(patient);
        }

        return patientsList;
    }

    /**
     * Actualiza la informacion de un paciente existente.
     */
    public void updatePatient(User patient) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("users").document(patient.getUid());
        ApiFuture<WriteResult> result = docRef.set(patient);
        result.get();
    }

    /**
     * Elimina el registro de un paciente de la base de datos.
     */
    public void deletePatient(String patientId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("users").document(patientId);
        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
    }
}