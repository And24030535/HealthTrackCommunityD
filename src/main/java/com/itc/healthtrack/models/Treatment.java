package com.itc.healthtrack.models;

/**
 * Representa un tratamiento médico para un paciente
 * Entidad 11 de 15 en el modelo normalizado
 */
public class Treatment {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private String doctorId;        // ID del doctor que prescribió (referencia a "Doctor")
    private String startDate;       // Fecha de inicio del tratamiento
    private String endDate;         // Fecha de fin del tratamiento (puede ser nulo si está en curso)
    private String diagnosis;       // Diagnóstico de la condición tratada

    // Constructor vacío requerido por Firestore
    public Treatment() {}

    // Constructor con parámetros
    public Treatment(String id, String patientId, String doctorId, String startDate, String endDate, String diagnosis) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.diagnosis = diagnosis;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    @Override
    public String toString() {
        return "Treatment{" +
                "id='" + id + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                '}';
    }
}

