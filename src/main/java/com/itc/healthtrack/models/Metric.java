package com.itc.healthtrack.models;

import com.google.cloud.Timestamp;

// Representa una medicion individual en el sistema (presion, glucosa, peso)
public class Metric {
    private String id;
    private String patientId;
    private Timestamp timestamp;
    private Integer systolic;
    private Integer diastolic;
    private Integer heartRate;
    private Double weight;
    private Double bmi;
    private Double glucoseLevel;

    // Constructor requerido por Firebase
    public Metric() {}

    // Getters y Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

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

    // Igual que en User — BeanMapper necesita getter+setter para registrar la propiedad.
    public String getMetricType() { return null; }
    public void   setMetricType(String metricType) {} // no-op: campo eliminado del modelo

    public String getNotes() { return null; }
    public void   setNotes(String notes) {} // no-op: campo eliminado del modelo
}