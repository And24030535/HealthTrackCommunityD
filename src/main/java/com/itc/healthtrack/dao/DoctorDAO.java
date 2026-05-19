package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Doctor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Doctor
 * Gestiona todas las operaciones CRUD para médicos en el sistema
 */
public class DoctorDAO {
    // Acceso centralizado a Firestore
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    // Nombre de la colección
    private static final String COLLECTION_NAME = "Doctor";

    /**
     * Crea un nuevo registro de médico
     * @param doctor - El objeto Doctor a guardar
     */
    public void createDoctor(Doctor doctor) throws Exception {
        // Validamos que los datos esenciales no sean nulos
        if (doctor == null || doctor.getId() == null) {
            throw new IllegalArgumentException("El doctor y su ID no pueden ser nulos");
        }
        // Guardamos el documento en Firestore
        firestore.collection(COLLECTION_NAME).document(doctor.getId()).set(doctor).get();
    }

    /**
     * Obtiene un doctor por su ID
     * @param doctorId - El ID del doctor a buscar
     * @return El objeto Doctor encontrado, o null si no existe
     */
    public Doctor getDoctorById(String doctorId) throws Exception {
        // Validamos el ID
        if (doctorId == null || doctorId.isEmpty()) {
            throw new IllegalArgumentException("El ID del doctor no puede ser nulo");
        }
        // Obtenemos y convertimos el documento
        return firestore.collection(COLLECTION_NAME).document(doctorId).get().get().toObject(Doctor.class);
    }

    /**
     * Busca un doctor por su userProfileId
     * @param userProfileId - El ID del perfil de usuario del doctor
     * @return El objeto Doctor encontrado, o null si no existe
     */
    public Doctor getDoctorByUserProfileId(String userProfileId) throws Exception {
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
        return querySnapshot.getDocuments().get(0).toObject(Doctor.class);
    }

    /**
     * Obtiene todos los doctores del sistema
     * @return Una lista de todos los objetos Doctor
     */
    public List<Doctor> getAllDoctors() throws Exception {
        // Lista para los resultados
        List<Doctor> doctors = new ArrayList<>();
        // Ejecutamos la consulta para obtener todos los documentos
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre cada documento
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos el documento a Doctor
            Doctor doctor = querySnapshot.getDocuments().get(i).toObject(Doctor.class);
            doctors.add(doctor);
        }
        return doctors;
    }

    /**
     * Obtiene doctores por su especialidad
     * @param specialtyId - El ID de la especialidad
     * @return Una lista de doctores de esa especialidad
     */
    public List<Doctor> getDoctorsBySpecialty(String specialtyId) throws Exception {
        // Validamos el ID
        if (specialtyId == null || specialtyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo");
        }
        // Lista para los resultados
        List<Doctor> doctors = new ArrayList<>();
        // Ejecutamos consulta filtrando por specialtyId
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("specialtyId", specialtyId)
                .get()
                .get();
        // Iteramos sobre los resultados
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos cada documento a Doctor
            Doctor doctor = querySnapshot.getDocuments().get(i).toObject(Doctor.class);
            doctors.add(doctor);
        }
        return doctors;
    }

    /**
     * Actualiza un doctor existente
     * @param doctor - El objeto Doctor con datos actualizados
     */
    public void updateDoctor(Doctor doctor) throws Exception {
        // Validamos los datos
        if (doctor == null || doctor.getId() == null) {
            throw new IllegalArgumentException("El doctor y su ID no pueden ser nulos");
        }
        // Actualizamos en Firestore
        firestore.collection(COLLECTION_NAME).document(doctor.getId()).set(doctor).get();
    }

    /**
     * Elimina un doctor
     * @param doctorId - El ID del doctor a eliminar
     */
    public void deleteDoctor(String doctorId) throws Exception {
        // Validamos el ID
        if (doctorId == null || doctorId.isEmpty()) {
            throw new IllegalArgumentException("El ID del doctor no puede ser nulo");
        }
        // Eliminamos de Firestore
        firestore.collection(COLLECTION_NAME).document(doctorId).delete().get();
    }
}

