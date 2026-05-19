package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad EmergencyContact
 * Gestiona operaciones CRUD para contactos de emergencia de pacientes
 */
public class EmergencyContactDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "EmergencyContact";

    // Crear contacto de emergencia
    public void createEmergencyContact(EmergencyContact contact) throws Exception {
        // Validamos datos esenciales
        if (contact == null || contact.getId() == null) {
            throw new IllegalArgumentException("El contacto y su ID no pueden ser nulos");
        }
        // Guardamos en Firestore
        firestore.collection(COLLECTION_NAME).document(contact.getId()).set(contact).get();
    }

    // Obtener contacto por ID
    public EmergencyContact getEmergencyContactById(String contactId) throws Exception {
        // Validamos ID
        if (contactId == null || contactId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        // Obtenemos y convertimos
        return firestore.collection(COLLECTION_NAME).document(contactId).get().get().toObject(EmergencyContact.class);
    }

    // Obtener contactos de un paciente
    public List<EmergencyContact> getContactsByPatient(String patientId) throws Exception {
        // Validamos ID del paciente
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        // Lista para resultados
        List<EmergencyContact> contacts = new ArrayList<>();
        // Ejecutamos consulta filtrando por patientId
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .get()
                .get();
        // Iteramos sobre resultados
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos documento a EmergencyContact
            EmergencyContact contact = querySnapshot.getDocuments().get(i).toObject(EmergencyContact.class);
            contacts.add(contact);
        }
        return contacts;
    }

    // Actualizar contacto
    public void updateEmergencyContact(EmergencyContact contact) throws Exception {
        // Validamos
        if (contact == null || contact.getId() == null) {
            throw new IllegalArgumentException("El contacto y su ID no pueden ser nulos");
        }
        // Actualizamos en Firestore
        firestore.collection(COLLECTION_NAME).document(contact.getId()).set(contact).get();
    }

    // Eliminar contacto
    public void deleteEmergencyContact(String contactId) throws Exception {
        // Validamos ID
        if (contactId == null || contactId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        // Eliminamos de Firestore
        firestore.collection(COLLECTION_NAME).document(contactId).delete().get();
    }
}

