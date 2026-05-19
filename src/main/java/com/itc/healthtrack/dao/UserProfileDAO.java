package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.UserProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad UserProfile
 * Gestiona todas las operaciones CRUD para perfiles de usuario
 * Estos perfiles están vinculados con Firebase Authentication
 */
public class UserProfileDAO {
    // Obtenemos la instancia centralizada de Firestore
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    // Nombre de la colección en Firestore
    private static final String COLLECTION_NAME = "UserProfile";

    /**
     * Crea un nuevo perfil de usuario
     * @param userProfile - El objeto UserProfile a guardar
     */
    public void createUserProfile(UserProfile userProfile) throws Exception {
        // Validamos que los datos esenciales no sean nulos
        if (userProfile == null || userProfile.getId() == null) {
            throw new IllegalArgumentException("El perfil de usuario y su ID no pueden ser nulos");
        }
        // Guardamos el documento completo en Firestore
        firestore.collection(COLLECTION_NAME).document(userProfile.getId()).set(userProfile).get();
    }

    /**
     * Obtiene un perfil de usuario por su ID
     * @param userProfileId - El ID del perfil a buscar
     * @return El objeto UserProfile encontrado, o null si no existe
     */
    public UserProfile getUserProfileById(String userProfileId) throws Exception {
        // Validamos el ID del perfil
        if (userProfileId == null || userProfileId.isEmpty()) {
            throw new IllegalArgumentException("El ID del perfil no puede ser nulo o vacío");
        }
        // Obtenemos el documento y lo convertimos a UserProfile
        return firestore.collection(COLLECTION_NAME).document(userProfileId).get().get().toObject(UserProfile.class);
    }

    /**
     * Busca un perfil de usuario por su UID de Firebase Authentication
     * @param authUid - El UID del usuario en Firebase Auth
     * @return El objeto UserProfile encontrado, o null si no existe
     */
    public UserProfile getUserProfileByAuthUid(String authUid) throws Exception {
        // Validamos que el authUid no sea nulo
        if (authUid == null || authUid.isEmpty()) {
            throw new IllegalArgumentException("El authUid no puede ser nulo o vacío");
        }
        // Ejecutamos una consulta filtrando por el campo authUid
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("authUid", authUid)
                .get()
                .get();
        // Verificamos si obtuvimos al menos un resultado
        if (querySnapshot.getDocuments().isEmpty()) {
            return null; // No encontramos el perfil
        }
        // Convertimos el primer resultado a UserProfile y lo retornamos
        return querySnapshot.getDocuments().get(0).toObject(UserProfile.class);
    }

    /**
     * Obtiene todos los perfiles de usuario
     * @return Una lista de todos los objetos UserProfile
     */
    public List<UserProfile> getAllUserProfiles() throws Exception {
        // Lista para almacenar los resultados
        List<UserProfile> userProfiles = new ArrayList<>();
        // Ejecutamos la consulta para obtener todos los documentos
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre cada documento obtenido
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos el documento a UserProfile
            UserProfile userProfile = querySnapshot.getDocuments().get(i).toObject(UserProfile.class);
            userProfiles.add(userProfile);
        }
        return userProfiles;
    }

    /**
     * Actualiza un perfil de usuario existente
     * @param userProfile - El objeto UserProfile con los datos actualizados
     */
    public void updateUserProfile(UserProfile userProfile) throws Exception {
        // Validamos los datos
        if (userProfile == null || userProfile.getId() == null) {
            throw new IllegalArgumentException("El perfil y su ID no pueden ser nulos");
        }
        // Actualizamos el documento en Firestore
        firestore.collection(COLLECTION_NAME).document(userProfile.getId()).set(userProfile).get();
    }

    /**
     * Elimina un perfil de usuario
     * @param userProfileId - El ID del perfil a eliminar
     */
    public void deleteUserProfile(String userProfileId) throws Exception {
        // Validamos el ID
        if (userProfileId == null || userProfileId.isEmpty()) {
            throw new IllegalArgumentException("El ID del perfil no puede ser nulo o vacío");
        }
        // Eliminamos el documento de Firestore
        firestore.collection(COLLECTION_NAME).document(userProfileId).delete().get();
    }
}

