package com.itc.healthtrack.models;

/**
 * Representa un contacto de emergencia para un paciente
 * Entidad 6 de 15 en el modelo normalizado
 */
public class EmergencyContact {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private String fullName;        // Nombre completo del contacto
    private String phoneNumber;     // Número telefónico
    private String relationship;    // Relación con el paciente (padre, hermano, etc.)

    // Constructor vacío requerido por Firestore
    public EmergencyContact() {}

    // Constructor con parámetros
    public EmergencyContact(String id, String patientId, String fullName, String phoneNumber, String relationship) {
        this.id = id;
        this.patientId = patientId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    @Override
    public String toString() {
        return fullName + " (" + relationship + ")";
    }
}

