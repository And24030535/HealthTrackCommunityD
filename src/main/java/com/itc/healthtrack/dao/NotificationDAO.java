package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Notification
 * Gestiona operaciones CRUD para notificaciones a usuarios
 */
public class NotificationDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Notification";

    public void createNotification(Notification notification) throws Exception {
        if (notification == null || notification.getId() == null) {
            throw new IllegalArgumentException("La notificación y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(notification.getId()).set(notification).get();
    }

    public Notification getNotificationById(String notificationId) throws Exception {
        if (notificationId == null || notificationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(notificationId).get().get().toObject(Notification.class);
    }

    // Obtener notificaciones de un usuario ordenadas por fecha descendente
    public List<Notification> getNotificationsByUser(String userId) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }
        List<Notification> notifications = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Notification notification = querySnapshot.getDocuments().get(i).toObject(Notification.class);
            notifications.add(notification);
        }
        return notifications;
    }

    // Obtener notificaciones sin entregar
    public List<Notification> getUndeliveredNotifications(String userId) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }
        List<Notification> notifications = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDelivered", false)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Notification notification = querySnapshot.getDocuments().get(i).toObject(Notification.class);
            notifications.add(notification);
        }
        return notifications;
    }

    public void updateNotification(Notification notification) throws Exception {
        if (notification == null || notification.getId() == null) {
            throw new IllegalArgumentException("La notificación y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(notification.getId()).set(notification).get();
    }

    public void deleteNotification(String notificationId) throws Exception {
        if (notificationId == null || notificationId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(notificationId).delete().get();
    }
}

