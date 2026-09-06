package de.delautrer.game.world.sky;

import de.delautrer.game.world.StarSystem;
import org.joml.Vector3f;
import java.util.Random;

public class SkyManager {
    // --- Zeit & Licht ---
    private float timeOfDay = 9f;
    private float timeSpeed = 0.0189f;
    private long dayIndex = 0;

    private final Vector3f colorDay = new Vector3f(0.37f, 0.62f, 0.95f);
    private final Vector3f colorSunrise = new Vector3f(1.0f, 0.38f, 0.12f);
    private final Vector3f colorSunset = new Vector3f(1.0f, 0.28f, 0.08f);
    private final Vector3f colorNight = new Vector3f(0.01f, 0.01f, 0.02f);

    private final Vector3f currentSkyColor = new Vector3f();
    private final Vector3f currentHorizonColor = new Vector3f();
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
        // --- 1. Zeit & Licht Updates ---
        float nextTime = timeOfDay + deltaTime * timeSpeed;
        if (nextTime >= 24.0f) {
            dayIndex += (long)(nextTime / 24.0f);
        }
        timeOfDay = nextTime % 24.0f;

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
            System.out.println("Weather has changed to " + nextWeather.name());
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
        float DAY_H = 0.12f;
        Vector3f duskOrDawn = (timeOfDay > 12.0f) ? colorSunset : colorSunrise;

        if (h > DAY_H) {
            currentSkyColor.set(colorDay);
            globalLightIntensity = 1.0f;
        } else if (h > 0.0f) {
            float t = h / DAY_H;
            float tSky = t * t;
            float tLight = t * t;
            duskOrDawn.lerp(colorDay, tSky, currentSkyColor);
            globalLightIntensity = 0.18f + tLight * 0.82f;
        } else if (h > -DAY_H) {
            float t = (h + DAY_H) / DAY_H;
            colorNight.lerp(duskOrDawn, t, currentSkyColor);
            globalLightIntensity = 0.05f + t * 0.13f;
        } else {
            currentSkyColor.set(colorNight);
            globalLightIntensity = 0.05f;
        }

        // Wenn es bewölkt ist, machen wir das globale Licht etwas dunkler!
        if (currentWeather == Weather.OVERCAST) {
            globalLightIntensity *= 0.7f;
        }

        // --- Horizont-Farbe zentral berechnen ---
        float absH = Math.abs(h);
        if (absH < DAY_H) {
            currentHorizonColor.set(
                currentSkyColor.x * 1.15f,
                currentSkyColor.y * 0.85f,
                currentSkyColor.z * 0.55f
            );
        } else {
            currentHorizonColor.set(
                currentSkyColor.x * 1.08f,
                currentSkyColor.y * 1.08f,
                currentSkyColor.z * 1.08f
            );
        }

        // Rot-Mix NUR in der Abend-/Morgendämmerung
        if ((timeOfDay >= 18.13f && timeOfDay < 18.40f) || (timeOfDay >= 5.60f && timeOfDay < 5.87f)) {
            currentHorizonColor.lerp(duskOrDawn, 0.55f);
        }

        // Uhr-basierte Rampe (nightAmt)
        float NIGHT_START = 18.13f;
        float NIGHT_FULL  = 18.40f;
        float DAWN_START  = 5.60f;
        float DAWN_END    = 5.87f;

        float nightAmt;
        if (timeOfDay >= NIGHT_START && timeOfDay <= NIGHT_FULL) {
            nightAmt = (timeOfDay - NIGHT_START) / (NIGHT_FULL - NIGHT_START);
        } else if (timeOfDay > NIGHT_FULL || timeOfDay < DAWN_START) {
            nightAmt = 1.0f;
        } else if (timeOfDay >= DAWN_START && timeOfDay <= DAWN_END) {
            nightAmt = 1.0f - (timeOfDay - DAWN_START) / (DAWN_END - DAWN_START);
        } else {
            nightAmt = 0.0f;
        }

        currentSkyColor.lerp(colorNight, nightAmt);
        currentHorizonColor.lerp(colorNight, nightAmt);
        globalLightIntensity = globalLightIntensity + (0.05f - globalLightIntensity) * nightAmt;

        starAlpha = nightAmt;

        currentHorizonColor.x = Math.min(Math.max(currentHorizonColor.x, 0.0f), 1.0f);
        currentHorizonColor.y = Math.min(Math.max(currentHorizonColor.y, 0.0f), 1.0f);
        currentHorizonColor.z = Math.min(Math.max(currentHorizonColor.z, 0.0f), 1.0f);
    }

    // Getter & Setter
    public Vector3f getSunDirection() { return sunDirection; }
    public Vector3f getCurrentSkyColor() { return currentSkyColor; }
    public Vector3f getCurrentHorizonColor() { return currentHorizonColor; }
    public float getGlobalLightIntensity() { return globalLightIntensity; }
    public float getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float time) {
        float oldTime = this.timeOfDay;
        float normalized = (time % 24.0f + 24.0f) % 24.0f;
        if (normalized < oldTime || time >= 24.0f) {
            long daysPassed = (long) Math.max(1, Math.floor(time / 24.0f));
            if (normalized >= oldTime && time < 24.0f) daysPassed = 0;
            dayIndex += daysPassed;
        }
        this.timeOfDay = normalized;
    }
    public long getDayIndex() { return dayIndex; }
    public void setDayIndex(long dayIndex) { this.dayIndex = dayIndex; }
    public int getMoonPhaseIndex() { return (int) Math.floorMod(dayIndex, 8L); }
    public float getStarAlpha() { return starAlpha; }
    public Weather getCurrentWeather() { return currentWeather; }
    public void setCurrentWeather(Weather w) { this.currentWeather = w; }

    public CloudSystem getCloudSystem() { return cloudSystem; }
    public StarSystem getStarSystem() { return starSystem; }
    public CelestialSystem getCelestialSystem() { return celestialSystem; }
}