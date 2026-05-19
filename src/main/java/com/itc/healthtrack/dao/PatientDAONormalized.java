package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Patient;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Patient
 * Gestiona todas las operaciones CRUD para pacientes en el sistema
 */
public class PatientDAONormalized {
    // Acceso centralizado a Firestore
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    // Nombre de la colección
    private static final String COLLECTION_NAME = "Patient";

    /**
     * Crea un nuevo registro de paciente
     * @param patient - El objeto Patient a guardar
     */
    public void createPatient(Patient patient) throws Exception {
        // Validamos que los datos esenciales no sean nulos
        if (patient == null || patient.getId() == null) {
            throw new IllegalArgumentException("El paciente y su ID no pueden ser nulos");
        }
        // Guardamos el documento en Firestore
        firestore.collection(COLLECTION_NAME).document(patient.getId()).set(patient).get();
    }

    /**
     * Obtiene un paciente por su ID
     * @param patientId - El ID del paciente a buscar
     * @return El objeto Patient encontrado, o null si no existe
     */
    public Patient getPatientById(String patientId) throws Exception {
        // Validamos el ID
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        // Obtenemos y convertimos el documento
        return firestore.collection(COLLECTION_NAME).document(patientId).get().get().toObject(Patient.class);
    }

    /**
     * Busca un paciente por su userProfileId
     * @param userProfileId - El ID del perfil de usuario del paciente
     * @return El objeto Patient encontrado, o null si no existe
     */
    public Patient getPatientByUserProfileId(String userProfileId) throws Exception {
        // Validamos el ID
        if (userProfileId == null || userProfileId.isEmpty()) {
            throw new IllegalArgumentException("El ID del perfil de usuario no puede ser nulo");
        }
        // Ejecutamos una consulta filtrando por userProfileId
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userProfileId", userProfileId)
                .get()
                .get();
        // Retornamos el primer resultado si existe
        if (querySnapshot.getDocuments().isEmpty()) {
            return null;
        }
        return querySnapshot.getDocuments().get(0).toObject(Patient.class);
    }

    /**
     * Obtiene todos los pacientes del sistema
     * @return Una lista de todos los objetos Patient
     */
    public List<Patient> getAllPatients() throws Exception {
        // Lista para los resultados
        List<Patient> patients = new ArrayList<>();
        // Ejecutamos la consulta para obtener todos los documentos
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre cada documento
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos el documento a Patient
            Patient patient = querySnapshot.getDocuments().get(i).toObject(Patient.class);
            patients.add(patient);
        }
        return patients;
    }

    /**
     * Obtiene todos los pacientes asignados a un doctor específico
     * @param doctorId - El ID del doctor
     * @return Una lista de pacientes del doctor
     */
    public List<Patient> getPatientsByDoctor(String doctorId) throws Exception {
        // Validamos el ID
        if (doctorId == null || doctorId.isEmpty()) {
            throw new IllegalArgumentException("El ID del doctor no puede ser nulo");
        }
        // Lista para los resultados
        List<Patient> patients = new ArrayList<>();
        // Ejecutamos consulta filtrando por primaryDoctorId
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("primaryDoctorId", doctorId)
                .get()
                .get();
        // Iteramos sobre los resultados
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos cada documento a Patient
            Patient patient = querySnapshot.getDocuments().get(i).toObject(Patient.class);
            patients.add(patient);
        }
        return patients;
    }

    /**
     * Actualiza un paciente existente
     * @param patient - El objeto Patient con datos actualizados
     */
    public void updatePatient(Patient patient) throws Exception {
        // Validamos los datos
        if (patient == null || patient.getId() == null) {
            throw new IllegalArgumentException("El paciente y su ID no pueden ser nulos");
        }
        // Actualizamos en Firestore
        firestore.collection(COLLECTION_NAME).document(patient.getId()).set(patient).get();
    }

    /**
     * Elimina un paciente
     * @param patientId - El ID del paciente a eliminar
     */
    public void deletePatient(String patientId) throws Exception {
        // Validamos el ID
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        // Eliminamos de Firestore
        firestore.collection(COLLECTION_NAME).document(patientId).delete().get();
    }
}

