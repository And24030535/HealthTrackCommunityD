package com.itc.healthtrack.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Gestiona la conexión única con Firebase Firestore y Authentication.
 * Implementa el patrón Singleton para garantizar una sola instancia.
 */
public class FirebaseConnection {

    // Variable estática volátil para almacenar la instancia única (Singleton)
    private static volatile FirebaseConnection instance;
    // Referencia a la base de datos Firestore
    private final Firestore db;
    // Referencia a Firebase Authentication
    private final FirebaseAuth auth;

    /**
     * Constructor privado que inicializa Firebase (Firestore y Authentication)
     * Solo se ejecuta una vez gracias al patrón Singleton
     */
    private FirebaseConnection() {
        try {
            // Cargamos el archivo de credenciales desde el folder resources
            // Este archivo contiene las claves privadas para conectar con Firebase
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-key.json");

            // Verificamos que el archivo de credenciales exista
            if (serviceAccount == null) {
                throw new RuntimeException("Archivo firebase-key.json no encontrado en resources.");
            }

            // Configuramos las opciones de Firebase con las credenciales cargadas
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Inicializamos la aplicación Firebase solo si no ha sido inicializada antes
            // Esto evita errores si el constructor se llama múltiples veces
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            // Obtenemos la instancia de Firestore para acceder a la base de datos
            db = FirestoreClient.getFirestore();

            // Obtenemos la instancia de Firebase Authentication para gestionar usuarios
            auth = FirebaseAuth.getInstance();

        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Firebase: " + e.getMessage());
        }
    }

    /**
     * Obtiene la instancia única de la conexión a Firebase.
     * Implementa el patrón Singleton para garantizar una sola conexión.
     *
     * @return La instancia única de FirebaseConnection
     */
    public static FirebaseConnection getInstance() {
        // Verificamos si ya existe una instancia
        if (instance == null) {
            // Sincronizamos para evitar condiciones de carrera
            // Solo un hilo a la vez puede entrar a este bloque
            synchronized (FirebaseConnection.class) {
                // Verificamos de nuevo (double-check locking)
                if (instance == null) {
                    // Creamos la instancia única
                    instance = new FirebaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Proporciona acceso a la base de datos Firestore.
     * Se usa para realizar operaciones como consultas, escrituras y eliminaciones.
     *
     * @return Objeto Firestore para operaciones en la base de datos
     */
    public Firestore getFirestore() {
        return db;
    }

    /**
     * Proporciona acceso a Firebase Authentication.
     * Se usa para crear usuarios, validar credenciales y gestionar sesiones.
     *
     * @return Objeto FirebaseAuth para operaciones de autenticación
     */
    public FirebaseAuth getAuth() {
        return auth;
    }
}