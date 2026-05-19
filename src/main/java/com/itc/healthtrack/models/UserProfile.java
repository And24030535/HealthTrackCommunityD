package com.itc.healthtrack.models;

/**
 * Representa el perfil de usuario vinculado a Firebase Authentication
 * Entidad 2 de 15 en el modelo normalizado
 */
public class UserProfile {
    private String id;              // Identificador único (documento en Firestore)
    private String authUid;         // UID del usuario en Firebase Authentication
    private String roleId;          // ID del rol (referencia a colección "Role")
    private String email;           // Correo del usuario
    private Long registeredAt;      // Timestamp de registro (milisegundos)

    // Constructor vacío requerido por Firestore
    public UserProfile() {}

    // Constructor con parámetros
    public UserProfile(String id, String authUid, String roleId, String email, Long registeredAt) {
        this.id = id;
        this.authUid = authUid;
        this.roleId = roleId;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthUid() { return authUid; }
    public void setAuthUid(String authUid) { this.authUid = authUid; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Long registeredAt) { this.registeredAt = registeredAt; }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

