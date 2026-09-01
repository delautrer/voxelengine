package de.delautrer.engine.audio;

import com.google.gson.Gson;
import de.delautrer.engine.audio.data.SoundMaterialDefinition;
import de.delautrer.engine.utils.ResourceUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
        loadFromFolder("assets/data/sounds");
    }

    private static void loadFromFolder(String folderPath) {
        List<String> files = ResourceUtils.listResources(folderPath, ".json");
        for (String fileName : files) {
            String fullPath = folderPath + "/" + fileName;
            try {
                Reader reader = ResourceUtils.readResourceToReader(fullPath);
                SoundMaterialDefinition def = GSON.fromJson(reader, SoundMaterialDefinition.class);
                if (def != null && def.materialName != null) {
                    materialDefinitions.put(def.materialName.toLowerCase(), def);
                    if (audioEngine != null && def.actions != null) {
                        for (List<String> paths : def.actions.values()) {
                            for (String path : paths) {
                                audioEngine.loadSound(path);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[SoundManager] Error loading sound definition " + fullPath + ": " + e.getMessage());
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

    private static String resolveMaterialName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "default";
        if (rawName.contains(":")) {
            rawName = rawName.substring(rawName.indexOf(':') + 1);
        }
        String name = rawName.toLowerCase();
        if (materialDefinitions.containsKey(name)) {
            return name;
        }
        return "default";
    }

    private static void playEvent(String materialName, String action, float volume, float minPitch, float maxPitch, float x, float y, float z, boolean relative, String source) {
        if (audioEngine == null) return;

        materialName = resolveMaterialName(materialName);
        action = action.toLowerCase();

        SoundMaterialDefinition def = materialDefinitions.get(materialName);

        // 1. Versuch: Hat das spezifische Material genau diese Aktion definiert?
        if (def != null && def.actions != null) {
            List<String> sounds = def.actions.get(action);
            if (sounds == null || sounds.isEmpty()) {
                if ("place".equals(action) || "step".equals(action)) {
                    sounds = def.actions.get("walk");
                } else if ("break".equals(action)) {
                    sounds = def.actions.get("jump_land");
                }
            }
            if (sounds != null && !sounds.isEmpty()) {
                audioEngine.playRandomFromList(sounds, volume, minPitch, maxPitch, x, y, z, relative);
                return;
            }
        }

        // 2. Versuch (FALLBACK): Material "default"
        SoundMaterialDefinition defaultDef = materialDefinitions.get("default");
        if (defaultDef != null && defaultDef.actions != null) {
            List<String> sounds = defaultDef.actions.get(action);
            if (sounds == null || sounds.isEmpty()) {
                if ("place".equals(action) || "step".equals(action)) {
                    sounds = defaultDef.actions.get("walk");
                } else if ("break".equals(action)) {
                    sounds = defaultDef.actions.get("jump_land");
                }
            }
            if (sounds != null && !sounds.isEmpty()) {
                audioEngine.playRandomFromList(sounds, volume, minPitch, maxPitch, x, y, z, relative);
                return;
            }
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