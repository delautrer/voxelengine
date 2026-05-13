package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.NoiseGenerator;

public class MultiNoiseSampler {

    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final NoiseGenerator continentalnessNoise;
    private final NoiseGenerator erosionNoise;
    private final NoiseGenerator weirdnessNoise;

    public MultiNoiseSampler(long seed) {
        this.temperatureNoise = new NoiseGenerator(seed * 11);
        this.humidityNoise = new NoiseGenerator(seed * 37);
        this.continentalnessNoise = new NoiseGenerator(seed * 97);
        this.erosionNoise = new NoiseGenerator(seed * 151);
        this.weirdnessNoise = new NoiseGenerator(seed * 211);
    }

    public Climate.TargetPoint sample(int x, int z) {
        // --- Skalierungen verringert für deutlich GRÖSSERE Biome ---
        float scaleTempHum = 0.0025f;   
        float scaleCont = 0.0015f;      
        float scaleErosion = 0.0025f;   
        float scaleWeirdness = 0.003f; 

        float temp = temperatureNoise.getFractalNoise2D(x * scaleTempHum, z * scaleTempHum, 4, 0.5f, 2.0f);
        float hum = humidityNoise.getFractalNoise2D(x * scaleTempHum, z * scaleTempHum, 4, 0.5f, 2.0f);
        float cont = continentalnessNoise.getFractalNoise2D(x * scaleCont, z * scaleCont, 5, 0.5f, 2.0f);
        float erosion = erosionNoise.getFractalNoise2D(x * scaleErosion, z * scaleErosion, 4, 0.5f, 2.0f);
        float weirdness = weirdnessNoise.getFractalNoise2D(x * scaleWeirdness, z * scaleWeirdness, 3, 0.5f, 2.0f);

        return new Climate.TargetPoint(temp, hum, cont, erosion, weirdness);
    }
}