package com.itc.healthtrack.models;

/**
 * Representa la relación entre un paciente y sus alergias
 * Entidad 8 de 15 en el modelo normalizado
 * Tabla de unión (junction table) entre Patient y Allergy
 */
public class PatientAllergy {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private String allergyId;       // ID de la alergia (referencia a "Allergy")
    private String detectionDate;   // Fecha en que se detectó la alergia
    private String notes;           // Notas adicionales sobre la alergia

    // Constructor vacío requerido por Firestore
    public PatientAllergy() {}

    // Constructor con parámetros
    public PatientAllergy(String id, String patientId, String allergyId, String detectionDate, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.allergyId = allergyId;
        this.detectionDate = detectionDate;
        this.notes = notes;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAllergyId() { return allergyId; }
    public void setAllergyId(String allergyId) { this.allergyId = allergyId; }

    public String getDetectionDate() { return detectionDate; }
    public void setDetectionDate(String detectionDate) { this.detectionDate = detectionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "PatientAllergy{id='" + id + "'}";
    }
}

