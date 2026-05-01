package de.delautrer.engine.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GamePaths {
    // Das Hauptverzeichnis für alle generierten Daten
    public static final Path ROOT_DIR = Paths.get("game_data");

    // Unterordner
    public static final Path SAVES_DIR = ROOT_DIR.resolve("saves");
    public static final Path SCREENSHOTS_DIR = ROOT_DIR.resolve("screenshots");
    public static final Path LOGS_DIR = ROOT_DIR.resolve("logs");

    // Dateien
    public static final Path SETTINGS_FILE = ROOT_DIR.resolve("settings.json");

    public static void initDirectories() {
        try {
            Files.createDirectories(SAVES_DIR);
            Files.createDirectories(SCREENSHOTS_DIR);
            Files.createDirectories(LOGS_DIR);
        } catch (IOException e) {
            System.err.println("[GameFiles] Critical error. Game folders couldnt not be created: " + e.getMessage());
        }
    }
}