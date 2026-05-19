package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Role
 * Gestiona todas las operaciones CRUD (Create, Read, Update, Delete) para roles
 */
public class RoleDAO {
    // Obtenemos la instancia de Firestore desde la conexión centralizada
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    // Nombre de la colección en Firestore
    private static final String COLLECTION_NAME = "Role";

    /**
     * Crea un nuevo rol en la base de datos
     * @param role - El objeto Role a guardar
     */
    public void createRole(Role role) throws Exception {
        // Detenemos null si no hay datos en role
        if (role == null || role.getId() == null) {
            throw new IllegalArgumentException("El rol y su ID no pueden ser nulos");
        }
        // Guardamos el documento en Firestore con el ID como nombre del documento
        firestore.collection(COLLECTION_NAME).document(role.getId()).set(role).get();
    }

    /**
     * Obtiene un rol específico por su ID
     * @param roleId - El ID del rol a buscar
     * @return El objeto Role encontrado, o null si no existe
     */
    public Role getRoleById(String roleId) throws Exception {
        // Verificamos que el ID no sea nulo
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("El ID del rol no puede ser nulo o vacío");
        }
        // Obtenemos el documento de Firestore y lo convertimos a objeto Role
        return firestore.collection(COLLECTION_NAME).document(roleId).get().get().toObject(Role.class);
    }

    /**
     * Obtiene todos los roles disponibles en el sistema
     * @return Una lista de todos los objetos Role
     */
    public List<Role> getAllRoles() throws Exception {
        // Lista que almacenará todos los roles
        List<Role> roles = new ArrayList<>();
        // Ejecutamos una consulta para obtener todos los documentos de la colección
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();
        // Iteramos sobre cada documento obtenido
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            // Convertimos cada documento a un objeto Role y lo agregamos a la lista
            Role role = querySnapshot.getDocuments().get(i).toObject(Role.class);
            roles.add(role);
        }
        return roles;
    }

    /**
     * Actualiza un rol existente
     * @param role - El objeto Role con los datos actualizados
     */
    public void updateRole(Role role) throws Exception {
        // Verificamos que el rol no sea nulo y tenga un ID válido
        if (role == null || role.getId() == null) {
            throw new IllegalArgumentException("El rol y su ID no pueden ser nulos");
        }
        // Actualizamos el documento existente en Firestore
        firestore.collection(COLLECTION_NAME).document(role.getId()).set(role).get();
    }

    /**
     * Elimina un rol de la base de datos
     * @param roleId - El ID del rol a eliminar
     */
    public void deleteRole(String roleId) throws Exception {
        // Verificamos que el ID sea válido
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("El ID del rol no puede ser nulo o vacío");
        }
        // Eliminamos el documento de Firestore
        firestore.collection(COLLECTION_NAME).document(roleId).delete().get();
    }
}

