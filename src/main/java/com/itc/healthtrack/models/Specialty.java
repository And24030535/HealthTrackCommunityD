package com.itc.healthtrack.models;

/**
 * Representa una especialidad médica
 * Entidad 3 de 15 en el modelo normalizado
 */
public class Specialty {
    private String id;              // Identificador único
    private String name;            // Nombre de la especialidad (Cardiología, Pediatría, etc.)

    // Constructor vacío requerido por Firestore
    public Specialty() {}

    // Constructor con parámetros
    public Specialty(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}

