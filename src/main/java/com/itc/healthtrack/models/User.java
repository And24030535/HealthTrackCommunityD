package com.itc.healthtrack.models;

import java.util.List;

public class User {

    private String uid;

    // correo del usuario tambien usado como id en firebase auth
    private String email;

    private String firstName;

    private String lastName;

    // rol patient doctor o admin
    private String role;

    // campo heredado no se usa en el desktop
    private String password;

    // campo heredado de documentos viejos
    private List<String> patientIds;

    // campos exclusivos para pacientes

    // fecha de nacimiento guardada como texto para simplicidad en fxml
    private String birthDate;

    // genero M F u Otro
    private String gender;

    // estatura del paciente en metros
    private Double height;

    // uid del medico asignado a este paciente
    private String assignedDoctorId;

    // nombre del medico asignado desnormalizado para mostrar sin consulta extra
    private String assignedDoctorName;

    public User() {}

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

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<String> getPatientIds() { return patientIds; }
    public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }

    @Override
    public String toString() {
        return this.firstName + " " + this.lastName;
    }
}
