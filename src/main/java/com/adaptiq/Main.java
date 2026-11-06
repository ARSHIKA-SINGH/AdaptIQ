package com.adaptiq;

/** JVM entry point kept separate from the JavaFX Application subclass. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        App.launchApp(args);
    }
}
