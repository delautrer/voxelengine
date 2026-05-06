package de.delautrer.game.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.engine.utils.GamePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public class SettingsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GameSettings currentSettings;

    public static void load() {
        if (!Files.exists(GamePaths.SETTINGS_FILE)) {
            // Datei existiert noch nicht -> Standardwerte nehmen und speichern
            currentSettings = new GameSettings();
            save();
            System.out.println("[SettingsManager] Created default settings.");
            return;
        }

        try {
            String json = Files.readString(GamePaths.SETTINGS_FILE);
            currentSettings = GSON.fromJson(json, GameSettings.class);
            currentSettings.validate();
            save();

            System.out.println("[SettingsManager] Settings loaded successfully.");
        } catch (IOException e) {
            System.err.println("[SettingsManager] Failed to load settings. Using defaults.");
            e.printStackTrace();
            currentSettings = new GameSettings();
        }
    }

    public static void save() {
        if (currentSettings == null) return;
        try {
            String json = GSON.toJson(currentSettings);
            Files.writeString(GamePaths.SETTINGS_FILE, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[SettingsManager] Failed to save settings.");
            e.printStackTrace();
        }
    }

    public static GameSettings get() {
        if (currentSettings == null) {
            load();
        }
        return currentSettings;
    }
}