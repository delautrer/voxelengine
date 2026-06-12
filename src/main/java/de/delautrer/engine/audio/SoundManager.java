package de.delautrer.engine.audio;

import com.google.gson.Gson;
import de.delautrer.engine.audio.data.SoundMaterialDefinition;
import de.delautrer.engine.utils.ResourceUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundManager {

    private static AudioEngine audioEngine;
    private static final Gson GSON = new Gson();

    private static final Map<String, SoundMaterialDefinition> materialDefinitions = new HashMap<>();

    public static void init(AudioEngine engine) {
        audioEngine = engine;
        loadAllDefinitions();
    }

    private static void loadAllDefinitions() {
        String folderPath = "/assets/data/sounds";

        List<String> files = ResourceUtils.listResourceFolder(folderPath);

        if (files == null || files.isEmpty()) {
            System.err.println("Hm. Where sounds?... " + folderPath);
            return;
        }

        for (String fileName : files) {
            if (!fileName.endsWith(".json")) continue;

            String fullPath = folderPath + "/" + fileName;

            // Laden als Stream (funktioniert im .jar / .exe)
            try (InputStream is = SoundManager.class.getResourceAsStream(fullPath)) {
                if (is == null) {
                    System.err.println("Could not read file: " + fullPath);
                    continue;
                }

                try (InputStreamReader reader = new InputStreamReader(is)) {
                    SoundMaterialDefinition def = GSON.fromJson(reader, SoundMaterialDefinition.class);
                    materialDefinitions.put(def.materialName.toLowerCase(), def);

                    // Alle in der JSON gefundenen OGGs direkt laden
                    for (List<String> paths : def.actions.values()) {
                        for (String path : paths) {
                            audioEngine.loadSound(path);
                        }
                    }
                    //System.out.println("Geladen: " + def.materialName);
                }
            } catch (Exception e) {
                System.err.println("Error while parsing: " + fileName + ": " + e.getMessage());
            }
        }
    }

    public static void playEvent(String materialName, String action, float volume) {
        playEvent(materialName, action, volume, 0.9f, 1.1f, "Environment");
    }

    public static void playEvent(String materialName, String action, float volume, String source) {
        playEvent(materialName, action, volume, 0.9f, 1.1f, source);
    }

    public static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch) {
        playEvent(materialName, action, volume, minPitch, maxPitch, 0, 0, 0, true, "Environment");
    }

    public static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch, String source) {
        playEvent(materialName, action, volume, minPitch, maxPitch, 0, 0, 0, true, source);
    }

    public static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch, float x, float y, float z) {
        playEvent(materialName, action, volume, minPitch, maxPitch, x, y, z, false, "Environment");
    }

    public static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch, float x, float y, float z, String source) {
        playEvent(materialName, action, volume, minPitch, maxPitch, x, y, z, false, source);
    }

    private static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch, float x, float y, float z, boolean relative, String source) {
        if (audioEngine == null) return;

        materialName = (materialName != null) ? materialName.toLowerCase() : "default";
        action = action.toLowerCase();

        SoundMaterialDefinition def = materialDefinitions.get(materialName);

        // Debug Logging
        if (de.delautrer.game.settings.SettingsManager.get().soundDebug) {
            float dist = (float) Math.sqrt((x-lastListenerPos.x)*(x-lastListenerPos.x) + (y-lastListenerPos.y)*(y-lastListenerPos.y) + (z-lastListenerPos.z)*(z-lastListenerPos.z));
            System.out.printf("[SoundDebug] [%-11s] Action: %-12s | Mat: %-8s | Vol: %.2f | Dist: %4.1f\n",
                source, action, materialName, volume, dist);
        }

        // 1. Versuch: Hat das spezifische Material genau diese Aktion definiert?
        if (def != null && def.actions.containsKey(action) && !def.actions.get(action).isEmpty()) {
            audioEngine.playRandomFromList(def.actions.get(action), volume, minPitch, maxPitch, x, y, z, relative);
            return;
        }

        // 2. Versuch (FALLBACK): Gibt es ein Material namens "default", das diese Aktion hat?
        SoundMaterialDefinition defaultDef = materialDefinitions.get("default");
        if (defaultDef != null && defaultDef.actions.containsKey(action) && !defaultDef.actions.get(action).isEmpty()) {
            audioEngine.playRandomFromList(defaultDef.actions.get(action), volume, minPitch, maxPitch, x, y, z, relative);
            return;
        }
    }

    public static void updateVolume() {
        if (audioEngine != null) {
            audioEngine.updateListener();
        }
    }

    public static void updateListener(org.joml.Vector3d pos, org.joml.Vector3f forward, org.joml.Vector3f up) {
        if (audioEngine != null) {
            audioEngine.updateListener((float) pos.x, (float) pos.y, (float) pos.z, forward.x, forward.y, forward.z, up.x, up.y, up.z);
            lastListenerPos.set(pos);
        }
    }

    private static final org.joml.Vector3d lastListenerPos = new org.joml.Vector3d();
}