package com.itc.healthtrack.models;

import com.google.cloud.Timestamp;

// representa una nota medica o recomendacion guardada en firestore
// puede ser un analisis automatico del sistema o una nota escrita por el medico
public class Recommendation {
    private String id;
    private String patientId;
    // uid del medico que escribio la nota (null en analisis automaticos)
    private String doctorId;
    private Timestamp generatedAt;
    // tipo de entrada: "suggestion" para analisis automaticos, "note" o "doctor_recommendation" para notas manuales
    private String type;
    private String title;
    private String message;
    private Boolean isRead;

    // constructor vacio requerido por firestore para deserializar documentos
    public Recommendation() {}

    // getters y setters

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
