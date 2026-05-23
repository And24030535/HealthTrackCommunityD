package com.itc.healthtrack.config;

/**
 * Constantes de configuración compartidas en toda la aplicación.
 *
 * Centraliza los tokens de seguridad para evitar duplicación entre
 * LoginController y RegisterController.
 *
 * NOTA: En producción, estos valores deben provenir de un backend
 * seguro o de variables de entorno, nunca de código fuente.
 */
public class AppConfig {

    // Token de acceso requerido para registrar o iniciar sesión como médico
    public static final String TOKEN_DOCTOR = "DOCTOR-2026";

    // Token maestro requerido para registrar una cuenta de administrador
    public static final String TOKEN_ADMIN = "ADMIN-TRACK-2026";

    // Constructor privado: esta clase solo contiene constantes, no se instancia
    private AppConfig() {}
}
