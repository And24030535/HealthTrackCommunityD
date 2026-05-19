package com.itc.healthtrack.models;

/**
 * Representa a un médico en el sistema
 * Entidad 4 de 15 en el modelo normalizado
 */
public class Doctor {
    private String id;              // Identificador único
    private String userProfileId;   // ID del perfil de usuario (referencia a "UserProfile")
    private String specialtyId;     // ID de la especialidad (referencia a "Specialty")
    private String licenseNumber;   // Número de licencia médica
    private String firstName;       // Nombre
    private String lastName;        // Apellido

    // Constructor vacío requerido por Firestore
    public Doctor() {}

    // Constructor con parámetros
    public Doctor(String id, String userProfileId, String specialtyId, String licenseNumber, String firstName, String lastName) {
        this.id = id;
        this.userProfileId = userProfileId;
        this.specialtyId = specialtyId;
        this.licenseNumber = licenseNumber;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserProfileId() { return userProfileId; }
    public void setUserProfileId(String userProfileId) { this.userProfileId = userProfileId; }

    public String getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(String specialtyId) { this.specialtyId = specialtyId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}

