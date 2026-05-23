package com.itc.healthtrack;

import javafx.application.Application;

// punto de entrada de la aplicacion
// este truco es necesario para que javafx funcione correctamente como jar ejecutable
public class Launcher {
    public static void main(String[] args) {
        Application.launch(HealthTrackApplication.class, args);
    }
}
