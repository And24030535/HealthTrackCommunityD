package com.itc.healthtrack.config;

// clase que guarda las constantes globales del proyecto
public class AppConfig {

    // token requerido para registrarse o iniciar sesion como medico
    public static final String TOKEN_DOCTOR = "DOCTOR-2026";

    // token requerido para registrar una cuenta de administrador
    public static final String TOKEN_ADMIN = "ADMIN-TRACK-2026";

    // constructor privado para que nadie pueda crear una instancia de esta clase
    private AppConfig() {}
}
