package com.itc.healthtrack.models;

/**
 * Representa una recomendación médica enviada a un paciente
 * Entidad 14 de 15 en el modelo normalizado
 */
public class Recommendation {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private Long generatedAt;       // Fecha y hora de generación (milisegundos)
    private String type;            // Tipo de recomendación (suggestion, alert, info)
    private String title;           // Título de la recomendación
    private String message;         // Contenido detallado de la recomendación
    private Boolean isRead;         // Indica si ha sido leída por el usuario

    // Constructor vacío requerido por Firestore
    public Recommendation() {}

    // Constructor con parámetros
    public Recommendation(String id, String patientId, Long generatedAt, String type, String title, String message, Boolean isRead) {
        this.id = id;
        this.patientId = patientId;
        this.generatedAt = generatedAt;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Long generatedAt) { this.generatedAt = generatedAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    @Override
    public String toString() {
        return "Recommendation{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}