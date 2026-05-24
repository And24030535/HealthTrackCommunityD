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

// clase generica que centraliza operaciones basicas de firestore para cualquier entidad
public class GenericDAO<T> {

    private final Firestore db;

    private final String collectionName;

    private final Class<T> entityClass;

    public GenericDAO(Class<T> entityClass, String collectionName) {
        this.entityClass = entityClass;
        this.collectionName = collectionName;
        this.db = FirebaseConnection.getInstance().getFirestore();
    }

    // genera un id nuevo para un documento
    public String createDocumentId() {
        DocumentReference docRef = db.collection(collectionName).document();
        return docRef.getId();
    }

    // guarda o actualiza una entidad con el id dado
    public void save(String documentId, T entity) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<WriteResult> result = docRef.set(entity);
        result.get();
    }

    // trae una entidad por su id
    public T getById(String documentId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(entityClass);
        }
        return null;
    }

    // trae todos los documentos de la coleccion
    public List<T> getAll() throws ExecutionException, InterruptedException {
        List<T> results = new ArrayList<>();
        ApiFuture<QuerySnapshot> querySnapshot = db.collection(collectionName).get();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            T entity = document.toObject(entityClass);
            if (entity != null) {
                results.add(entity);
            }
        }
        return results;
    }

    // trae los documentos donde un campo coincide con un valor
    public List<T> getByField(String fieldName, Object value) throws ExecutionException, InterruptedException {
        List<T> results = new ArrayList<>();
        Query query = db.collection(collectionName).whereEqualTo(fieldName, value);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            T entity = document.toObject(entityClass);
            if (entity != null) {
                results.add(entity);
            }
        }
        return results;
    }

    // actualiza varios documentos en una sola operacion atomica
    public void batchUpdateFields(List<String> documentIds, Map<String, Object> fields)
            throws ExecutionException, InterruptedException {
        if (documentIds == null || documentIds.isEmpty()) return;
        WriteBatch batch = db.batch();
        for (String id : documentIds) {
            DocumentReference docRef = db.collection(collectionName).document(id);
            // actualizamos solo los campos indicados sin sobreescribir el documento completo
            batch.update(docRef, fields);
        }
        batch.commit().get();
    }

    // elimina un documento por su id
    public void delete(String documentId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
    }
}
