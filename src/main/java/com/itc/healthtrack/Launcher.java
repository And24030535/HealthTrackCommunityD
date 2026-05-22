package com.itc.healthtrack;

import javafx.application.Application;

// Punto de entrada de la aplicación. Necesario como clase separada para que
// el .jar ejecutable funcione correctamente con JavaFX en módulos.
public class Launcher {
    public static void main(String[] args) {
        Application.launch(HealthTrackApplication.class, args);
    }
}
