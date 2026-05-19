package com.itc.healthtrack.models;

/**
 * Representa un tipo de alergia o sustancia alérgena
 * Entidad 7 de 15 en el modelo normalizado
 */
public class Allergy {
    private String id;              // Identificador único
    private String name;            // Nombre de la alergia (Penicilina, Cacahuete, etc.)
    private String severity;        // Severidad (leve, moderada, severa)

    // Constructor vacío requerido por Firestore
    public Allergy() {}

    // Constructor con parámetros
    public Allergy(String id, String name, String severity) {
        this.id = id;
        this.name = name;
        this.severity = severity;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    @Override
    public String toString() {
        return name + " (" + severity + ")";
    }
}

