package de.delautrer.engine.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GamePaths {

    private static Path determineDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Paths.get(appData, "Veinstride");
            }
            return Paths.get(userHome, "Veinstride");
        } else if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "Veinstride");
        } else {
            return Paths.get(userHome, ".local", "share", "Veinstride");
        }
    }

    // Das Hauptverzeichnis für alle generierten Daten
    public static final Path ROOT_DIR = determineDataDir();

    // Unterordner
    public static final Path SAVES_DIR = ROOT_DIR.resolve("saves");
    public static final Path SCREENSHOTS_DIR = ROOT_DIR.resolve("screenshots");
    public static final Path LOGS_DIR = ROOT_DIR.resolve("logs");
    public static final Path STRUCTURES_DIR = ROOT_DIR.resolve("structures");

    // Dateien
    public static final Path SETTINGS_FILE = ROOT_DIR.resolve("settings.json");

    public static void initDirectories() {
        try {
            Files.createDirectories(SAVES_DIR);
            Files.createDirectories(SCREENSHOTS_DIR);
            Files.createDirectories(LOGS_DIR);
            Files.createDirectories(STRUCTURES_DIR);
        } catch (IOException e) {
            System.err.println("[GameFiles] Critical error. Game folders couldnt not be created: " + e.getMessage());
        }
    }
}