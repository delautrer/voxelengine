package de.delautrer;

import de.delautrer.engine.Engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) {

        /*
        try {
            File logFile = new File("crash.log");
            PrintStream logStream = new PrintStream(new FileOutputStream(logFile, true));
            System.setOut(logStream);
            System.setErr(logStream);

            System.out.println("=== SPIEL GESTARTET ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        Engine engine = new Engine();
        engine.run();
    }
}