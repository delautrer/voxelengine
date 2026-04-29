package de.delautrer.game.world.sky;

import de.delautrer.game.world.StarSystem;
import org.joml.Vector3f;
import java.util.Random;

public class SkyManager {
    // --- Zeit & Licht ---
    private float timeOfDay = 0.0f;
    private float timeSpeed = 0.02f;

    private final Vector3f colorDay = new Vector3f(0.4f, 0.7f, 1.0f);
    private final Vector3f colorSunrise = new Vector3f(1.0f, 0.4f, 0.1f);
    private final Vector3f colorNight = new Vector3f(0.01f, 0.01f, 0.02f);

    private final Vector3f currentSkyColor = new Vector3f();
    private final Vector3f sunDirection = new Vector3f();
    private float globalLightIntensity = 1.0f;
    private float starAlpha = 0.0f;

    // --- Wetter & Systeme ---
    private Weather currentWeather = Weather.PARTLY_CLOUDY;
    private float weatherTimer = 0.0f;
    private final Random weatherRandom = new Random();

    private final CloudSystem cloudSystem;
    private final StarSystem starSystem;
    private final CelestialSystem celestialSystem;

    // Callback, um der Engine zu sagen: "Hey, bau das Wolken-Mesh neu!"
    private Runnable onWeatherChanged;

    public SkyManager() {
        this.cloudSystem = new CloudSystem();
        this.starSystem = new StarSystem();
        this.celestialSystem = new CelestialSystem();
    }

    public void setWeatherCallback(Runnable callback) {
        this.onWeatherChanged = callback;
    }

    public void update(float deltaTime) {
        // --- 1. Zeit & Licht Updates (wie vorher) ---
        timeOfDay = (timeOfDay + deltaTime * timeSpeed) % 24.0f;

        if (timeOfDay >= 18.5f && timeOfDay <= 19.5f) {
            starAlpha = (timeOfDay - 18.5f);
        } else if (timeOfDay > 19.5f || timeOfDay < 4.5f) {
            starAlpha = 1.0f;
        } else if (timeOfDay >= 4.5f && timeOfDay <= 5.5f) {
            starAlpha = 1.0f - (timeOfDay - 4.5f);
        } else {
            starAlpha = 0.0f;
        }

        float celestialAngle = ((timeOfDay - 6.0f) / 24.0f) * (float) (Math.PI * 2.0);
        sunDirection.x = (float) Math.cos(celestialAngle);
        sunDirection.y = (float) Math.sin(celestialAngle);
        sunDirection.z = 0.0f;
        sunDirection.normalize();

        updateSkyAndLight();

        // --- 2. Wolken-Offset updaten ---
        cloudSystem.update(deltaTime);

        // --- 3. Wetter Zyklus (z.B. alle 10 Minuten echtes RL-Wetter ändern) ---
        weatherTimer += deltaTime;
        if (weatherTimer > 600.0f) { // 600 Sekunden = 10 Minuten
            weatherTimer = 0.0f;
            changeWeatherRandomly();
        }
    }

    private void changeWeatherRandomly() {
        Weather[] weathers = Weather.values();
        Weather nextWeather = weathers[weatherRandom.nextInt(weathers.length)];

        if (nextWeather != currentWeather) {
            System.out.println("[SkyManager] Wetter ändert sich zu: " + nextWeather.name());
            this.currentWeather = nextWeather;
            // Mesh neu generieren lassen!
            if (onWeatherChanged != null) {
                onWeatherChanged.run();
            }
        }
    }

    public void forceWeather(Weather weather) {
        if (this.currentWeather != weather) {
            this.currentWeather = weather;
            if (onWeatherChanged != null) onWeatherChanged.run();
        }
    }

    private void updateSkyAndLight() {
        float h = sunDirection.y;
        if (h > 0.2f) {
            currentSkyColor.set(colorDay);
            globalLightIntensity = 1.0f;
        } else if (h > 0.0f) {
            float blend = h / 0.2f;
            colorSunrise.lerp(colorDay, (float)Math.pow(blend, 0.5), currentSkyColor);
            globalLightIntensity = 0.15f + (float)Math.pow(blend, 2.0) * 0.85f;
        } else if (h > -0.2f) {
            float blend = (h + 0.2f) / 0.2f;
            colorNight.lerp(colorSunrise, blend, currentSkyColor);
            globalLightIntensity = 0.05f + blend * 0.1f;
        } else {
            currentSkyColor.set(colorNight);
            globalLightIntensity = 0.05f;
        }

        // Wenn es bewölkt ist, machen wir das globale Licht etwas dunkler!
        if (currentWeather == Weather.OVERCAST) {
            globalLightIntensity *= 0.7f;
        }
    }

    // Getter & Setter
    public Vector3f getSunDirection() { return sunDirection; }
    public Vector3f getCurrentSkyColor() { return currentSkyColor; }
    public float getGlobalLightIntensity() { return globalLightIntensity; }
    public float getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float timeOfDay) { this.timeOfDay = timeOfDay; }
    public float getStarAlpha() { return starAlpha; }
    public Weather getCurrentWeather() { return currentWeather; }
    public void setCurrentWeather(Weather w) { this.currentWeather = w; }

    public CloudSystem getCloudSystem() { return cloudSystem; }
    public StarSystem getStarSystem() { return starSystem; }
    public CelestialSystem getCelestialSystem() { return celestialSystem; }
}