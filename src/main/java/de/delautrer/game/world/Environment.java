package de.delautrer.game.world;

import org.joml.Vector3f;

public class Environment {
    private float timeOfDay = 0.0f; // 0.0 = Mittag
    private float timeSpeed = 0.25f;

    private final Vector3f colorDay = new Vector3f(0.4f, 0.7f, 1.0f);
    private final Vector3f colorSunrise = new Vector3f(1.0f, 0.4f, 0.1f);
    private final Vector3f colorNight = new Vector3f(0.01f, 0.01f, 0.02f);

    private final Vector3f currentSkyColor = new Vector3f();
    private final Vector3f sunDirection = new Vector3f();
    private float globalLightIntensity = 1.0f;
    private float starAlpha = 0.0f;

    public void update(float deltaTime) {
        timeOfDay = (timeOfDay + deltaTime * timeSpeed) % 24.0f;

        // --- Sterne weich ein- und ausblenden ---
        if (timeOfDay >= 18.5f && timeOfDay <= 19.5f) {
            starAlpha = (timeOfDay - 18.5f); // Abends einblenden
        } else if (timeOfDay > 19.5f || timeOfDay < 4.5f) {
            starAlpha = 1.0f; // Nachts volle Kanne sichtbar
        } else if (timeOfDay >= 4.5f && timeOfDay <= 5.5f) {
            starAlpha = 1.0f - (timeOfDay - 4.5f); // Morgens ausblenden
        } else {
            starAlpha = 0.0f; // Tagsüber weg
        }

        // --- Himmelskörper-Rotation ---
        // 0 = Mitternacht (-90°), 6 = Sonnenaufgang (0°), 12 = Mittag (+90°), 18 = Sonnenuntergang (+180°)
        float celestialAngle = ((timeOfDay - 6.0f) / 24.0f) * (float) (Math.PI * 2.0);

        sunDirection.x = (float) Math.cos(celestialAngle);
        sunDirection.y = (float) Math.sin(celestialAngle);
        sunDirection.z = 0.0f;
        sunDirection.normalize();

        updateSkyAndLight();
    }

    private void updateSkyAndLight() {
        float h = sunDirection.y;

        if (h > 0.1f) {
            // Tag
            currentSkyColor.set(colorDay);
            globalLightIntensity = 1.0f;
        }
        else if (h > 0.0f) {
            // Sonnenauf/untergang (Sonne sichtbar)
            float blend = h / 0.1f;
            // pow(blend, 0.5) lässt das Blau schneller erscheinen
            colorSunrise.lerp(colorDay, (float)Math.pow(blend, 0.5), currentSkyColor);
            globalLightIntensity = 0.01f + (float)Math.pow(blend, 2.0) * 0.99f;
        }
        else if (h > -0.1f) {
            // Dämmerung (Sonne weg, Himmel glüht nach)
            float blend = (h + 0.1f) / 0.1f;
            colorNight.lerp(colorSunrise, blend, currentSkyColor);
            globalLightIntensity = 0.05f; // In der Dämmerung schon fast Nacht-Dunkelheit
        }
        else {
            // Nacht
            currentSkyColor.set(colorNight);
            globalLightIntensity = 0.005f; // Extrem dunkel
        }
    }

    public float getStarAlpha() { return starAlpha; }
    public Vector3f getSunDirection() { return sunDirection; }
    public Vector3f getCurrentSkyColor() { return currentSkyColor; }
    public float getGlobalLightIntensity() { return globalLightIntensity; }
    public float getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float timeOfDay) {
        this.timeOfDay = timeOfDay;
    }
}