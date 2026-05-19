package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Specialty;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Specialty
 * Gestiona todas las operaciones CRUD para especialidades médicas
 */
public class SpecialtyDAO {
    // Acceso centralizado a Firestore
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    // Nombre de la colección
    private static final String COLLECTION_NAME = "Specialty";

    /**
     * Crea una nueva especialidad médica
     * @param specialty - El objeto Specialty a guardar
     */
    public void createSpecialty(Specialty specialty) throws Exception {
        // Validamos que los datos esenciales no sean nulos
        if (specialty == null || specialty.getId() == null) {
            throw new IllegalArgumentException("La especialidad y su ID no pueden ser nulos");
        }
        // Guardamos la especialidad en Firestore
        firestore.collection(COLLECTION_NAME).document(specialty.getId()).set(specialty).get();
    }

    /**
     * Obtiene una especialidad por su ID
     * @param specialtyId - El ID de la especialidad a buscar
     * @return El objeto Specialty encontrado, o null si no existe
     */
    public Specialty getSpecialtyById(String specialtyId) throws Exception {
        // Validamos el ID
        if (specialtyId == null || specialtyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo");
        }
        // Obtenemos y convertimos el documento
        return firestore.collection(COLLECTION_NAME).document(specialtyId).get().get().toObject(Specialty.class);
    }

    /**
     * Obtiene todas las especialidades disponibles
     * @return Una lista de todos los objetos Specialty
     */
    public List<Specialty> getAllSpecialties() throws Exception {
        // Lista para los resultados
        List<Specialty> specialties = new ArrayList<>();
        // Ejecutamos la consulta
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre los resultados
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos cada documento a Specialty
            Specialty specialty = querySnapshot.getDocuments().get(i).toObject(Specialty.class);
            specialties.add(specialty);
        }
        return specialties;
    }

    /**
     * Actualiza una especialidad existente
     * @param specialty - El objeto Specialty con datos actualizados
     */
    public void updateSpecialty(Specialty specialty) throws Exception {
        // Validamos los datos
        if (specialty == null || specialty.getId() == null) {
            throw new IllegalArgumentException("La especialidad y su ID no pueden ser nulos");
        }
        // Actualizamos en Firestore
        firestore.collection(COLLECTION_NAME).document(specialty.getId()).set(specialty).get();
    }

    /**
     * Elimina una especialidad
     * @param specialtyId - El ID de la especialidad a eliminar
     */
    public void deleteSpecialty(String specialtyId) throws Exception {
        // Validamos el ID
        if (specialtyId == null || specialtyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo");
        }
        // Eliminamos de Firestore
        firestore.collection(COLLECTION_NAME).document(specialtyId).delete().get();
    }
}

