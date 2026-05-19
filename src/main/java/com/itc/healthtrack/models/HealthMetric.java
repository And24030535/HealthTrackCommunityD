package com.itc.healthtrack.models;

/**
 * Representa una métrica de salud registrada de un paciente
 * Entidad 9 de 15 en el modelo normalizado
 */
public class HealthMetric {
    private String id;              // Identificador único
    private String patientId;       // ID del paciente (referencia a "Patient")
    private Long timestamp;         // Fecha y hora del registro (milisegundos)
    private Integer systolic;       // Presión arterial sistólica
    private Integer diastolic;      // Presión arterial diastólica
    private Integer heartRate;      // Frecuencia cardíaca en latidos por minuto
    private Double weight;          // Peso en kilogramos
    private Double bmi;             // Índice de masa corporal
    private Double glucoseLevel;    // Nivel de glucosa en mg/dL

    // Constructor vacío requerido por Firestore
    public HealthMetric() {}

    // Constructor con parámetros
    public HealthMetric(String id, String patientId, Long timestamp, Integer systolic, Integer diastolic,
                       Integer heartRate, Double weight, Double bmi, Double glucoseLevel) {
        this.id = id;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.heartRate = heartRate;
        this.weight = weight;
        this.bmi = bmi;
        this.glucoseLevel = glucoseLevel;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Integer getSystolic() { return systolic; }
    public void setSystolic(Integer systolic) { this.systolic = systolic; }

    public Integer getDiastolic() { return diastolic; }
    public void setDiastolic(Integer diastolic) { this.diastolic = diastolic; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public Double getGlucoseLevel() { return glucoseLevel; }
    public void setGlucoseLevel(Double glucoseLevel) { this.glucoseLevel = glucoseLevel; }

    @Override
    public String toString() {
        return "HealthMetric{" +
                "id='" + id + '\'' +
                ", systolic=" + systolic +
                ", diastolic=" + diastolic +
                '}';
    }
}

