package com.itc.healthtrack.dao;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.User;

import java.util.concurrent.ExecutionException;

/**
 * Gestiona las operaciones de lectura y escritura para la coleccion 'users'.
 */
public class UserDAO {

    private final Firestore db;

    public UserDAO() {
        this.db = FirebaseConnection.getInstance().getFirestore();
    }

    /**
     * Valida el acceso de un usuario. Para propositos academicos simples,
     * busca coincidencias directas por correo.
     */
    /**
     * Valida el acceso de un usuario verificando que el correo y la contraseña coincidan.
     */
    public User authenticateUser(String email, String password) throws ExecutionException, InterruptedException {
        CollectionReference usersRef = db.collection("users");

        // Exige que ambos campos sean exactamente iguales a los de la base de datos
        Query query = usersRef.whereEqualTo("email", email)
                .whereEqualTo("password", password);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        if (!querySnapshot.get().getDocuments().isEmpty()) {
            DocumentSnapshot document = querySnapshot.get().getDocuments().get(0);
            return document.toObject(User.class);
        }
        return null; // Retorna nulo si las credenciales son incorrectas
    }

    /**
     * Guarda un nuevo usuario en la base de datos.
     */
    public void saveUser(User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("users").document();
        user.setUid(docRef.getId());
        ApiFuture<WriteResult> result = docRef.set(user);
        result.get(); // Espera a que la operacion finalice
    }

}