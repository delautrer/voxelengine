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
            System.err.println("[SoundManager] Hm. Where sounds?... " + folderPath);
            return;
        }

        for (String fileName : files) {
            if (!fileName.endsWith(".json")) continue;

            String fullPath = folderPath + "/" + fileName;

            // Laden als Stream (funktioniert im .jar / .exe)
            try (InputStream is = SoundManager.class.getResourceAsStream(fullPath)) {
                if (is == null) {
                    System.err.println("[SoundManager] Could not read file: " + fullPath);
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
                    //System.out.println("[SoundManager] Geladen: " + def.materialName);
                }
            } catch (Exception e) {
                System.err.println("[SoundManager] Error while parsing: " + fileName + ": " + e.getMessage());
            }
        }
    }

    public static void playEvent(String materialName, String action, float volume) {
        if (audioEngine == null) return;

        materialName = (materialName != null) ? materialName.toLowerCase() : "default";
        action = action.toLowerCase();

        SoundMaterialDefinition def = materialDefinitions.get(materialName);

        // 1. Versuch: Hat das spezifische Material genau diese Aktion definiert?
        if (def != null && def.actions.containsKey(action) && !def.actions.get(action).isEmpty()) {
            audioEngine.playRandomFromList(def.actions.get(action), volume);
            return;
        }

        // 2. Versuch (FALLBACK): Gibt es ein Material namens "default", das diese Aktion hat?
        SoundMaterialDefinition defaultDef = materialDefinitions.get("default");
        if (defaultDef != null && defaultDef.actions.containsKey(action) && !defaultDef.actions.get(action).isEmpty()) {
            audioEngine.playRandomFromList(defaultDef.actions.get(action), volume);
            return;
        }

        // 3. Fallback (Wirklich gar nichts gefunden)
        // Du kannst hier optional einen Print lassen fürs Debugging
        // System.out.println("[SoundManager] Missing Sound: " + materialName + " -> " + action);
    }
}