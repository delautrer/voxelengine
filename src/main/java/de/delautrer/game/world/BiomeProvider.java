package de.delautrer.game.world;

public class BiomeProvider {

    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;

    public BiomeProvider(long seed) {
        // Unterschiedliche Offsets, damit Temperatur und Feuchtigkeit nicht identisch sind
        this.temperatureNoise = new NoiseGenerator(seed * 1234L);
        this.humidityNoise = new NoiseGenerator(seed * 5678L);
    }

    public Biome getBiome(int globalX, int globalZ, float elevation, float roughness) {
        // Temperatur und Feuchtigkeit berechnen (Wertebereich ca. -1.0 bis 1.0)
        float temp = temperatureNoise.getFractalNoise2D(globalX * 0.002f, globalZ * 0.002f, 3, 0.5f, 2.0f);
        float hum = humidityNoise.getFractalNoise2D(globalX * 0.002f, globalZ * 0.002f, 3, 0.5f, 2.0f);

        // 1. GEBIRGE (Haben oberste Priorität, da der TerrainPass sie massiv nach oben zieht)
        if (roughness > 0.1f) {
            return Biome.MOUNTAINS;
        }

        // 2. OZEAN (Alles, was tief liegt und kein Berg ist)
        if (elevation < -0.15f) {
            return Biome.OCEAN;
        }

        // 3. HÜGEL (Leicht erhöhte Zonen, die keine extremen Berge sind)
        if (elevation > 0.05f) {
            return Biome.HILLS;
        }

        // 4. FLACHLÄNDER (aufgeteilt durch Temperatur & Feuchtigkeit)
        if (temp > 0.2f && hum < -0.1f) {
            return Biome.DESERT;
        } else if (temp > 0.1f && hum > 0.1f) {
            return Biome.FOREST;
        }

        // Standard-Fallback
        return Biome.PLAINS;
    }
}