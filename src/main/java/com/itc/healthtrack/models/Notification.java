package com.itc.healthtrack.models;

/**
 * Representa una notificación enviada a un usuario
 * Entidad 15 de 15 en el modelo normalizado
 */
public class Notification {
    private String id;              // Identificador único
    private String userId;          // ID del usuario que recibe la notificación (referencia a "UserProfile")
    private Long sentAt;            // Fecha y hora de envío (milisegundos)
    private String message;         // Contenido de la notificación
    private Boolean isDelivered;    // Indica si la notificación fue entregada

    // Constructor vacío requerido por Firestore
    public Notification() {}

    // Constructor con parámetros
    public Notification(String id, String userId, Long sentAt, String message, Boolean isDelivered) {
        this.id = id;
        this.userId = userId;
        this.sentAt = sentAt;
        this.message = message;
        this.isDelivered = isDelivered;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getSentAt() { return sentAt; }
    public void setSentAt(Long sentAt) { this.sentAt = sentAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsDelivered() { return isDelivered; }
    public void setIsDelivered(Boolean isDelivered) { this.isDelivered = isDelivered; }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", isDelivered=" + isDelivered +
                '}';
    }
}

