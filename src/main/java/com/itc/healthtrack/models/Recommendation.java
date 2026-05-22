package com.itc.healthtrack.models;

import com.google.cloud.Timestamp;

//Representa una recomendación o alerta médica enviada a un paciente,
// o una nota clínica escrita manualmente por un médico (type = "note").
public class Recommendation {
    private String id;
    private String patientId;
    // UID del médico autor — obligatorio cuando type = "note", null en análisis automáticos
    private String doctorId;
    private Timestamp generatedAt;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;

    // Constructor vacío requerido por Firestore
    public Recommendation() {}

    // Getters y Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public Timestamp getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Timestamp generatedAt) { this.generatedAt = generatedAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}