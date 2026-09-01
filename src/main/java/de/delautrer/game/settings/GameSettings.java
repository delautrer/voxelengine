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

    public static final int MIN_FPS = 30;
    public static final int MAX_FPS = 240;
    public static final int UNLIMITED_FPS = 1000;

    public static class SkinToneKeyframe {
        public final float pos;
        public final float r, g, b;

        public SkinToneKeyframe(float pos, float r, float g, float b) {
            this.pos = pos;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static final SkinToneKeyframe[] SKIN_KEYFRAMES = new SkinToneKeyframe[] {
        new SkinToneKeyframe(0.00f, 1.00f, 0.90f, 0.82f), // Alabaster / Lightest
        new SkinToneKeyframe(0.05f, 0.95f, 0.80f, 0.70f), // Light Fair
        new SkinToneKeyframe(0.10f, 0.78f, 0.60f, 0.44f), // Mocha / Medium Tan (formerly at 0.5)
        new SkinToneKeyframe(0.25f, 0.60f, 0.42f, 0.28f), // Warm Bronze
        new SkinToneKeyframe(0.45f, 0.44f, 0.28f, 0.17f), // Rich Chestnut
        new SkinToneKeyframe(0.65f, 0.28f, 0.16f, 0.09f), // Deep Espresso
        new SkinToneKeyframe(0.82f, 0.16f, 0.09f, 0.05f), // Dark Cocoa
        new SkinToneKeyframe(1.00f, 0.07f, 0.04f, 0.02f)  // Ebony / Deepest Dark
    };

    // Backwards compatibility SkinTone class
    public static class SkinTone {
        public final String name;
        public final float r, g, b;
        public SkinTone(String name, float r, float g, float b) {
            this.name = name; this.r = r; this.g = g; this.b = b;
        }
    }

    // Player Customization
    @SerializedName("player_name")
    public String playerName = "Player";

    @SerializedName("skin_tone_factor")
    public float skinToneFactor = 0.05f;

    @SerializedName("skin_tone_index")
    public int skinToneIndex = 1;

    @SerializedName("first_launch")
    public boolean firstLaunch = true;

    public float[] getSkinToneColorRGB() {
        float f = Math.max(0.0f, Math.min(1.0f, skinToneFactor));
        SkinToneKeyframe[] kf = SKIN_KEYFRAMES;

        if (f <= kf[0].pos) return new float[] { kf[0].r, kf[0].g, kf[0].b };
        if (f >= kf[kf.length - 1].pos) {
            SkinToneKeyframe last = kf[kf.length - 1];
            return new float[] { last.r, last.g, last.b };
        }

        for (int i = 0; i < kf.length - 1; i++) {
            if (f >= kf[i].pos && f <= kf[i + 1].pos) {
                float t = (f - kf[i].pos) / (kf[i + 1].pos - kf[i].pos);
                float r = kf[i].r + (kf[i + 1].r - kf[i].r) * t;
                float g = kf[i].g + (kf[i + 1].g - kf[i].g) * t;
                float b = kf[i].b + (kf[i + 1].b - kf[i].b) * t;
                return new float[] { r, g, b };
            }
        }

        return new float[] { kf[0].r, kf[0].g, kf[0].b };
    }

    public SkinTone getSkinTone() {
        float[] rgb = getSkinToneColorRGB();
        return new SkinTone("Custom", rgb[0], rgb[1], rgb[2]);
    }

    // Grafik / Kamera
    @SerializedName("fov")
    public float fov = 70.0f;

    @SerializedName("render_distance")
    public int renderDistance = 8;

    // Steuerung
    @SerializedName("mouse_sensitivity")
    public float mouseSensitivity = 0.5f;

    @SerializedName("max_fps")
    public int maxFps = 120;

    @SerializedName("mouse_invert_y")
    public boolean invertY = false;

    @SerializedName("view_bobbing")
    public boolean viewBobbing = true;

    @SerializedName("item_breathing")
    public boolean itemBreathing = true;

    // Audio (Schon mal vorbereitet für das Soundsystem!)
    @SerializedName("sound_master_volume")
    public float masterVolume = 1.0f;

    @SerializedName("sound_sfx_volume")
    public float sfxVolume = 1.0f;

    @SerializedName("sound_ambient_volume")
    public float ambientVolume = 1.0f;

    @SerializedName("sound_debug")
    public boolean soundDebug = false;

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
        
        if (maxFps != UNLIMITED_FPS) {
            maxFps = Math.max(MIN_FPS, Math.min(MAX_FPS, maxFps));
        }

        masterVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, masterVolume));
        sfxVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, sfxVolume));
        ambientVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, ambientVolume));
    }
}