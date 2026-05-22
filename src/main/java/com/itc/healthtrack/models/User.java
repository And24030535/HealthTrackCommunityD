package com.itc.healthtrack.models;

/**
 * Representa a un usuario dentro del sistema HealthTrack.
 *
 * NOTA DE SEGURIDAD: El campo "password" fue eliminado intencionalmente.
 * Las contraseñas son gestionadas de forma segura por Firebase Authentication
 * y NUNCA deben almacenarse en Firestore. El identificador único (uid) que
 * vincula este perfil con Firebase Auth se guarda en el campo "uid".
 *
 * Atributos en inglés y comentarios en español como se solicitó.
 */
public class User {

    // Identificador único del usuario — corresponde al UID generado por Firebase Auth
    private String uid;

    // Correo electrónico del usuario, también usado como identificador en Firebase Auth
    private String email;

    // Nombre del usuario
    private String firstName;

    // Apellido(s) del usuario
    private String lastName;

    // Rol dentro del sistema: "patient", "doctor" o "admin"
    private String role;

    // -------------------------------------------------------------------
    // Campos exclusivos para pacientes
    // -------------------------------------------------------------------

    // Fecha de nacimiento almacenada como texto para simplicidad en FXML (ej: "2000-01-15")
    private String birthDate;

    // Género del paciente: "M", "F" u "Otro"
    private String gender;

    // Estatura del paciente en metros (ej: 1.75)
    private Double height;

    // UID del médico asignado a este paciente
    private String assignedDoctorId;

    // Nombre completo del médico asignado (desnormalizado para mostrar en UI sin consulta extra)
    private String assignedDoctorName;

    // Constructor vacío requerido por Firestore para deserializar documentos automáticamente
    public User() {}

    // -------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------

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

    // BeanMapper construye su mapa de propiedades a partir de los GETTERS.
    // Sin getter, el setter correspondiente es ignorado y el warning persiste.
    // Solución: agregar ambos. El getter devuelve null para no escribir datos obsoletos en Firestore.
    public String getPassword() { return null; }
    public void   setPassword(String password) {} // no-op: contraseña vive en Firebase Auth, no en Firestore

    public java.util.List<String> getPatientIds() { return null; }
    public void setPatientIds(java.util.List<String> patientIds) {} // no-op: campo obsoleto

    @Override
    public String toString() {
        return this.firstName + " " + this.lastName;
    }
}