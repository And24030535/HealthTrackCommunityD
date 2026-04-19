package com.itc.healthtrack.models;

import java.util.List;

/**
 * Representa a un usuario dentro del sistema HealthTrack.
 * Atributos en inglés y comentarios en español como se solicitó.
 */
public class User {
    private String uid;               // Identificador único
    private String email;             // Correo institucional
    private String firstName;         // Nombre
    private String lastName;          // Apellidos
    private String role;              // "patient", "doctor" o "admin"
    private String password;          // Contraseña

    // Campos exclusivos para pacientes
    private String birthDate;         // Fecha de nacimiento (String para simplicidad en FXML)
    private String gender;            // "M" o "F"
    private Double height;            // Estatura en metros
    private String assignedDoctorId;  // UID del médico a cargo

    // Campos exclusivos para médicos
    private String specialty;         // Especialidad médica
    private List<String> patientIds;  // Lista de pacientes a cargo

    // Constructor vacío requerido por Firestore
    public User() {}

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public String getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(String assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<String> getPatientIds() { return patientIds; }
    public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }

    @Override
    public String toString() {
        return this.firstName + " " + this.lastName;
    }
}