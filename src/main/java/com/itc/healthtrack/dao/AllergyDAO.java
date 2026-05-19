package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Allergy;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Allergy
 * Gestiona operaciones CRUD para alergias en el sistema
 */
public class AllergyDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Allergy";

    // Crear alergia
    public void createAllergy(Allergy allergy) throws Exception {
        // Validamos datos
        if (allergy == null || allergy.getId() == null) {
            throw new IllegalArgumentException("La alergia y su ID no pueden ser nulos");
        }
        // Guardamos en Firestore
        firestore.collection(COLLECTION_NAME).document(allergy.getId()).set(allergy).get();
    }

    // Obtener alergia por ID
    public Allergy getAllergyById(String allergyId) throws Exception {
        // Validamos ID
        if (allergyId == null || allergyId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        // Obtenemos y convertimos
        return firestore.collection(COLLECTION_NAME).document(allergyId).get().get().toObject(Allergy.class);
    }

    // Obtener todas las alergias
    public List<Allergy> getAllAllergies() throws Exception {
        // Lista para resultados
        List<Allergy> allergies = new ArrayList<>();
        // Ejecutamos consulta para obtener todos
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre resultados
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos documento a Allergy
            Allergy allergy = querySnapshot.getDocuments().get(i).toObject(Allergy.class);
            allergies.add(allergy);
        }
        return allergies;
    }

    // Actualizar alergia
    public void updateAllergy(Allergy allergy) throws Exception {
        // Validamos
        if (allergy == null || allergy.getId() == null) {
            throw new IllegalArgumentException("La alergia y su ID no pueden ser nulos");
        }
        // Actualizamos en Firestore
        firestore.collection(COLLECTION_NAME).document(allergy.getId()).set(allergy).get();
    }

    // Eliminar alergia
    public void deleteAllergy(String allergyId) throws Exception {
        // Validamos ID
        if (allergyId == null || allergyId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        // Eliminamos de Firestore
        firestore.collection(COLLECTION_NAME).document(allergyId).delete().get();
    }
}

