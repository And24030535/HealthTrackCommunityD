package com.itc.healthtrack.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

// clase que maneja la conexion con firebase usando el patron singleton
// solo se crea una conexion y se reutiliza en todo el proyecto
public class FirebaseConnection {

    private static volatile FirebaseConnection instance;
    private final Firestore db;

    private FirebaseConnection() {
        try {
            // cargamos el archivo de credenciales que firebase necesita para autenticarse
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-key.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Archivo firebase-key.json no encontrado.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // solo inicializamos la app si no habia sido inicializada antes
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            db = FirestoreClient.getFirestore();

        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Firebase: " + e.getMessage());
        }
    }

    // devuelve la unica instancia de la conexion creandola si es la primera vez
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

    // regresa el objeto firestore para poder hacer consultas a la base de datos
    public Firestore getFirestore() {
        return db;
    }
}
