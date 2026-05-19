package com.itc.healthtrack.models;

/**
 * Representa una cita médica entre un paciente y un doctor
 * Entidad 10 de 15 en el modelo normalizado
 */
public class Appointment {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private String doctorId;        // ID del doctor (referencia a "Doctor")
    private Long scheduledDatetime; // Fecha y hora programada (milisegundos)
    private String status;          // Estado de la cita (programada, completada, cancelada)
    private String reason;          // Motivo de la cita

    // Constructor vacío requerido por Firestore
    public Appointment() {}

    // Constructor con parámetros
    public Appointment(String id, String patientId, String doctorId, Long scheduledDatetime, String status, String reason) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.scheduledDatetime = scheduledDatetime;
        this.status = status;
        this.reason = reason;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public Long getScheduledDatetime() { return scheduledDatetime; }
    public void setScheduledDatetime(Long scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "Appointment{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

