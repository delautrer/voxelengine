package de.delautrer.game.world;

public class BiomeProvider {

    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final NoiseGenerator floraNoise;

    // Wenn du ALLE Biome noch größer haben willst, mach diese Zahl kleiner (z.B. 0.0015f)
    private static final float BIOME_SCALE = 0.002f;
    private static final float FLOWER_PATCH_SCALE = 0.005f;

    public BiomeProvider(long seed) {
        this.temperatureNoise = new NoiseGenerator(seed * 1234L);
        this.humidityNoise = new NoiseGenerator(seed * 5678L);
        this.floraNoise = new NoiseGenerator(seed * 9999L);
    }

    public Biome getBiome(int globalX, int globalZ, float elevation, float roughness) {
        float temp = temperatureNoise.getFractalNoise2D(globalX * BIOME_SCALE, globalZ * BIOME_SCALE, 3, 0.5f, 2.0f);
        float hum = humidityNoise.getFractalNoise2D(globalX * BIOME_SCALE, globalZ * BIOME_SCALE, 3, 0.5f, 2.0f);

        boolean isDesert = temp > 0.2f && hum < -0.1f;

        if (roughness > 0.1f) {
            return isDesert ? Biome.DESERT_HILLS : Biome.MOUNTAINS;
        }
        if (elevation < -0.15f) {
            return Biome.OCEAN;
        }
        if (elevation > 0.05f) {
            return isDesert ? Biome.DESERT_HILLS : Biome.HILLS;
        }
        if (isDesert) {
            return Biome.DESERT;
        }
        // HIER IST DER FIX: Die Schwellenwerte sind deutlich niedriger.
        // Dadurch deckt der Wald jetzt eine viel größere Fläche auf der "Klima-Karte" ab.
        else if (temp > -0.05f && hum > 0.0f) {
            return Biome.FOREST;
        }

        float flower = floraNoise.getFractalNoise2D(globalX * FLOWER_PATCH_SCALE, globalZ * FLOWER_PATCH_SCALE, 2, 0.5f, 2.0f);
        if (flower > 0.3f) {
            return Biome.FLOWER_PLAINS;
        }

        return Biome.PLAINS;
    }
}