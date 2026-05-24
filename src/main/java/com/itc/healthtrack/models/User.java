package com.itc.healthtrack.models;

// usuario del sistema puede ser paciente medico o admin
public class User {

    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    // rol patient doctor o admin
    private String role;

    // campos exclusivos de pacientes
    // fecha de nacimiento como texto para simplicidad en FXML ej 2000-01-15
    private String birthDate;
    // M F u Otro
    private String gender;
    // estatura en metros
    private Double height;
    private String assignedDoctorId;
    // nombre del medico desnormalizado para mostrar en la ui sin consulta extra
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

    @Override
    public String toString() {
        return this.firstName + " " + this.lastName;
    }
}
