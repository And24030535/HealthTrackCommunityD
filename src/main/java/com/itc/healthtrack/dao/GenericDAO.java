package com.itc.healthtrack.dao;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.WriteBatch;
import com.itc.healthtrack.config.FirebaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

// Clase genérica que centraliza operaciones básicas de Firestore para cualquier entidad
public class GenericDAO<T> {

    // Referencia a la base de datos Firestore
    private final Firestore db;

    // Nombre de la colección donde se guardan los documentos
    private final String collectionName;

    // Tipo de clase que se usará para convertir documentos a objetos Java
    private final Class<T> entityClass;

    // Constructor que recibe el tipo de entidad y el nombre de la colección
    public GenericDAO(Class<T> entityClass, String collectionName) {
        // Guarda el tipo de entidad para usarlo al convertir documentos
        this.entityClass = entityClass;
        // Guarda el nombre de la colección para usarlo en las consultas
        this.collectionName = collectionName;
        // Obtiene la conexión única a Firestore
        this.db = FirebaseConnection.getInstance().getFirestore();
    }

    // Genera un ID nuevo para un documento dentro de la colección
    public String createDocumentId() {
        // Crea una referencia con ID autogenerado
        DocumentReference docRef = db.collection(collectionName).document();
        // Retorna el ID generado por Firestore
        return docRef.getId();
    }

    // Guarda o actualiza una entidad usando el ID proporcionado
    public void save(String documentId, T entity) throws ExecutionException, InterruptedException {
        // Crea una referencia directa al documento con el ID recibido
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        // Envía el objeto completo a Firestore
        ApiFuture<WriteResult> result = docRef.set(entity);
        // Espera a que la operación termine antes de continuar
        result.get();
    }

    // Obtiene una entidad específica por su ID
    public T getById(String documentId) throws ExecutionException, InterruptedException {
        // Crea una referencia al documento solicitado
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        // Ejecuta la consulta en Firestore
        ApiFuture<DocumentSnapshot> future = docRef.get();
        // Espera a que Firestore responda
        DocumentSnapshot document = future.get();
        // Si el documento existe, lo convierte a objeto y lo devuelve
        if (document.exists()) {
            return document.toObject(entityClass);
        }
        // Si no existe, retorna null
        return null;
    }

    // Obtiene todos los documentos de la colección y los convierte a objetos
    public List<T> getAll() throws ExecutionException, InterruptedException {
        // Lista que almacenará los resultados convertidos
        List<T> results = new ArrayList<>();
        // Ejecuta una consulta para traer todos los documentos
        ApiFuture<QuerySnapshot> querySnapshot = db.collection(collectionName).get();
        // Recorre cada documento recibido
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            // Convierte el documento a la clase solicitada
            T entity = document.toObject(entityClass);
            // Agrega el objeto a la lista solo si no es nulo
            if (entity != null) {
                results.add(entity);
            }
        }
        // Devuelve la lista completa de resultados
        return results;
    }

    // Obtiene documentos donde un campo específico coincide con un valor
    public List<T> getByField(String fieldName, Object value) throws ExecutionException, InterruptedException {
        // Lista donde se guardarán los resultados filtrados
        List<T> results = new ArrayList<>();
        // Crea la consulta con la condición indicada
        Query query = db.collection(collectionName).whereEqualTo(fieldName, value);
        // Ejecuta la consulta en Firestore
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        // Recorre los documentos encontrados
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            // Convierte el documento en objeto Java
            T entity = document.toObject(entityClass);
            // Agrega el objeto a la lista si es válido
            if (entity != null) {
                results.add(entity);
            }
        }
        // Retorna la lista de coincidencias
        return results;
    }

    // Actualiza múltiples documentos en una sola operación atómica (WriteBatch).
    // Si un documento falla, toda la operación se revierte — cero actualizaciones parciales.
    public void batchUpdateFields(List<String> documentIds, Map<String, Object> fields)
            throws ExecutionException, InterruptedException {
        if (documentIds == null || documentIds.isEmpty()) return;
        // Creamos el batch a partir de la instancia de Firestore
        WriteBatch batch = db.batch();
        for (String id : documentIds) {
            DocumentReference docRef = db.collection(collectionName).document(id);
            // Actualizamos solo los campos indicados — no sobreescribimos el documento completo
            batch.update(docRef, fields);
        }
        // Confirmamos el batch y esperamos a que Firestore confirme todos los cambios
        batch.commit().get();
    }

    // Elimina un documento por su ID
    public void delete(String documentId) throws ExecutionException, InterruptedException {
        // Crea una referencia al documento que se quiere borrar
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        // Ejecuta la eliminación en Firestore
        ApiFuture<WriteResult> result = docRef.delete();
        // Espera a que la eliminación termine
        result.get();
    }
}
