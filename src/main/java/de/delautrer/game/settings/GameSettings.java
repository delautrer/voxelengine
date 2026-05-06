package de.delautrer.game.settings;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.SerializedName;
import org.lwjgl.glfw.GLFW;


public class GameSettings {

    // --- NEU: Konstante Limits (Gson ignoriert statische Felder) ---
    public static final float MIN_FOV = 30.0f;
    public static final float MAX_FOV = 120.0f;

    public static final int MIN_RENDER_DISTANCE = 4;
    public static final int MAX_RENDER_DISTANCE = 12;

    public static final float MIN_SENSITIVITY = 0.1f;
    public static final float MAX_SENSITIVITY = 2.0f;

    public static final float MIN_VOLUME = 0.0f;
    public static final float MAX_VOLUME = 1.0f;

    // Grafik / Kamera
    @SerializedName("fov")
    public float fov = 70.0f;

    @SerializedName("render_distance")
    public int renderDistance = 8;

    // Steuerung
    @SerializedName("mouse_sensitivity")
    public float mouseSensitivity = 0.5f;

    @SerializedName("mouse_invert_y")
    public boolean invertY = false;

    // Audio (Schon mal vorbereitet für das Soundsystem!)
    @SerializedName("sound_master_volume")
    public float masterVolume = 1.0f;

    @SerializedName("sound_sfx_volume")
    public float sfxVolume = 1.0f;

    @SerializedName("sound_ambient_volume")
    public float ambientVolume = 1.0f;

    // Tastenbelegungen

    @SerializedName("key_binds")
    public Map<String, Integer> keyBinds = new HashMap<>();

    public GameSettings() {
        keyBinds.put("MOVE_FORWARD", GLFW.GLFW_KEY_W);
        keyBinds.put("MOVE_BACKWARD", GLFW.GLFW_KEY_S);
        keyBinds.put("MOVE_LEFT", GLFW.GLFW_KEY_A);
        keyBinds.put("MOVE_RIGHT", GLFW.GLFW_KEY_D);
        keyBinds.put("JUMP", GLFW.GLFW_KEY_SPACE);
        keyBinds.put("SNEAK", GLFW.GLFW_KEY_LEFT_SHIFT);
        keyBinds.put("SPRINT", GLFW.GLFW_KEY_LEFT_CONTROL);
        keyBinds.put("INVENTORY", GLFW.GLFW_KEY_E);
        keyBinds.put("DROP_ITEM", GLFW.GLFW_KEY_Q);
        keyBinds.put("TOGGLE_UI", GLFW.GLFW_KEY_F1);
        keyBinds.put("SCREENSHOT", GLFW.GLFW_KEY_F2);
        keyBinds.put("DEBUG_MENU", GLFW.GLFW_KEY_F3);
        keyBinds.put("CHAT_OPEN_T", GLFW.GLFW_KEY_T);
    }

    public void validate() {
        fov = Math.max(MIN_FOV, Math.min(MAX_FOV, fov));
        renderDistance = Math.max(MIN_RENDER_DISTANCE, Math.min(MAX_RENDER_DISTANCE, renderDistance));
        mouseSensitivity = Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, mouseSensitivity));

        masterVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, masterVolume));
        sfxVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, sfxVolume));
        ambientVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, ambientVolume));
    }
}