package com.itc.healthtrack.models;

/**
 * Representa un medicamento disponible en el sistema
 * Entidad 12 de 15 en el modelo normalizado
 */
public class Medication {
    private String id;              // Identificador único
    private String genericName;     // Nombre genérico del medicamento (Ibuprofen, Paracetamol, etc.)
    private String brandName;       // Marca comercial del medicamento
    private String manufacturer;    // Fabricante del medicamento

    // Constructor vacío requerido por Firestore
    public Medication() {}

    // Constructor con parámetros
    public Medication(String id, String genericName, String brandName, String manufacturer) {
        this.id = id;
        this.genericName = genericName;
        this.brandName = brandName;
        this.manufacturer = manufacturer;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    @Override
    public String toString() {
        return genericName + " (" + brandName + ")";
    }
}

