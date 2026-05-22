package com.itc.healthtrack.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Gestiona la conexión única con Firebase Firestore.
 */
public class FirebaseConnection {

    private static volatile FirebaseConnection instance;
    private final Firestore db;

    private FirebaseConnection() {
        try {
            // Carga el archivo de credenciales desde la carpeta resources
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-key.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Archivo firebase-key.json no encontrado.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Inicializa la app solo si no ha sido inicializada previamente
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            db = FirestoreClient.getFirestore();

        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Firebase: " + e.getMessage());
        }
    }

    /**
     * Obtiene la instancia única de la conexión.
     * @return Instancia de FirebaseConnection.
     */
    public static FirebaseConnection getInstance() {
        if (instance == null) {
            synchronized (FirebaseConnection.class) {
                if (instance == null) {
                    instance = new FirebaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Proporciona el acceso a la base de datos Firestore.
     * @return Objeto Firestore para realizar consultas.
     */
    public Firestore getFirestore() {
        return db;
    }
}