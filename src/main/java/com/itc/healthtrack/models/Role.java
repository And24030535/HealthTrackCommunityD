package com.itc.healthtrack.models;

/**
 * Representa un rol en el sistema (patient, doctor, admin)
 * Entidad 1 de 15 en el modelo normalizado
 */
public class Role {
    private String id;              // Identificador único
    private String name;            // Nombre del rol (patient, doctor, admin)
    private String description;     // Descripción del rol

    // Constructor vacío requerido por Firestore
    public Role() {}

    // Constructor con parámetros
    public Role(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name;
    }
}

