package com.itc.healthtrack.models;

/**
 * Representa los detalles de medicamentos en un tratamiento
 * Entidad 13 de 15 en el modelo normalizado
 * Tabla de unión (junction table) entre Treatment y Medication
 */
public class TreatmentDetail {
    private String id;              // Identificador único
    private String treatmentId;     // ID del tratamiento (referencia a "Treatment")
    private String medicationId;    // ID del medicamento (referencia a "Medication")
    private String dosage;          // Dosis (500mg, 10ml, etc.)
    private String frequency;       // Frecuencia de administración (cada 8 horas, 2 veces al día, etc.)

    // Constructor vacío requerido por Firestore
    public TreatmentDetail() {}

    // Constructor con parámetros
    public TreatmentDetail(String id, String treatmentId, String medicationId, String dosage, String frequency) {
        this.id = id;
        this.treatmentId = treatmentId;
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.frequency = frequency;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }

    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    @Override
    public String toString() {
        return "TreatmentDetail{" +
                "id='" + id + '\'' +
                ", dosage='" + dosage + '\'' +
                ", frequency='" + frequency + '\'' +
                '}';
    }
}

