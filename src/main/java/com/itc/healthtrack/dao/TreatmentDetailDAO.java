package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.TreatmentDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad TreatmentDetail
 * Gestiona operaciones CRUD para detalles de medicamentos en tratamientos
 */
public class TreatmentDetailDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "TreatmentDetail";

    public void createTreatmentDetail(TreatmentDetail detail) throws Exception {
        if (detail == null || detail.getId() == null) {
            throw new IllegalArgumentException("El detalle y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(detail.getId()).set(detail).get();
    }

    public TreatmentDetail getTreatmentDetailById(String detailId) throws Exception {
        if (detailId == null || detailId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(detailId).get().get().toObject(TreatmentDetail.class);
    }

    public List<TreatmentDetail> getDetailsByTreatment(String treatmentId) throws Exception {
        if (treatmentId == null || treatmentId.isEmpty()) {
            throw new IllegalArgumentException("El ID del tratamiento no puede ser nulo");
        }
        List<TreatmentDetail> details = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("treatmentId", treatmentId)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            TreatmentDetail detail = querySnapshot.getDocuments().get(i).toObject(TreatmentDetail.class);
            details.add(detail);
        }
        return details;
    }

    public void updateTreatmentDetail(TreatmentDetail detail) throws Exception {
        if (detail == null || detail.getId() == null) {
            throw new IllegalArgumentException("El detalle y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(detail.getId()).set(detail).get();
    }

    public void deleteTreatmentDetail(String detailId) throws Exception {
        if (detailId == null || detailId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(detailId).delete().get();
    }
}

