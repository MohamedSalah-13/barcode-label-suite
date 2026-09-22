package com.mohamed.barcodelabel.demo.v6;

import javafx.application.Application;

/**
 * Classpath-friendly entry point for packaged JavaFX applications.
 *
 * <p>The JDK launcher treats a main class that directly extends {@link Application}
 * as a modular JavaFX launch, which fails when JavaFX is shipped on the classpath.
 * Keeping the entry point separate lets jpackage use the bundled JavaFX jars.</p>
 */
public final class Phase6Launcher {
    private Phase6Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(Phase6DemoApplication.class, args);
    }
}
