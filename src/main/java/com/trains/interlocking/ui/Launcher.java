/*
 * Launcher.java
 * A plain class with main() that starts the JavaFX application. Running MainApp
 * directly from an IDE can fail with "JavaFX runtime components are missing";
 * starting through a class that does NOT extend Application avoids that.
 * Lecture deck: JavaFX (Application lifecycle).
 * Owner: Aryan
 */
package com.trains.interlocking.ui;

import javafx.application.Application;

public class Launcher {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
