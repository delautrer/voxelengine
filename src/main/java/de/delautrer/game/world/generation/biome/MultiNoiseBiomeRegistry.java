package de.delautrer.game.world.generation.biome;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiNoiseBiomeRegistry {
    private static volatile List<Biome> BIOMES = Collections.emptyList();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static synchronized void init() {
        if (isInitialized) return;
        
        try (InputStream is = MultiNoiseBiomeRegistry.class.getResourceAsStream("/assets/world/biomes.json")) {
            if (is == null) {
                System.err.println("[BiomeRegistry] ERROR: /assets/world/biomes.json not found!");
                return;
            }
            List<Biome> loaded = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), new TypeToken<List<Biome>>(){}.getType());
            if (loaded != null) {
                BIOMES = Collections.unmodifiableList(loaded);
                isInitialized = true;
                System.out.println("[BiomeRegistry] Loaded " + BIOMES.size() + " biomes from JSON.");
            }
        } catch (Exception e) {
            System.err.println("[BiomeRegistry] Failed to load biomes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Biome getBiomeFor(Climate.TargetPoint point) {
        List<Biome> currentBiomes = BIOMES;
        if (currentBiomes.isEmpty()) return null;

        Biome bestBiome = currentBiomes.get(0);
        float bestFitness = bestBiome.calculateFitness(point);

        for (int i = 1; i < currentBiomes.size(); i++) {
            Biome candidate = currentBiomes.get(i);
            float fitness = candidate.calculateFitness(point);

            if (fitness < bestFitness) {
                bestFitness = fitness;
                bestBiome = candidate;
            }
        }
        return bestBiome;
    }

    public static List<Biome> getBiomes() {
        return BIOMES;
    }

    /**
     * Berechnet geblendete Terrain-Parameter basierend auf dem Klima.
     * Sorgt für weiche Übergänge zwischen Biomen.
     */
    public static float[] getBlendedTerrainParameters(Climate.TargetPoint point) {
        List<Biome> currentBiomes = BIOMES;
        if (currentBiomes.isEmpty()) return new float[]{64.0f, 15.0f};

        float totalWeight = 0.0f;
        float blendedBase = 0.0f;
        float blendedVar = 0.0f;

        for (Biome b : currentBiomes) {
            float fitness = b.calculateFitness(point);
            // Je kleiner die Fitness, desto höher das Gewicht.
            float weight = (float) Math.pow(1.0f / (fitness + 0.05f), 3); 
            
            blendedBase += b.baseHeight * weight;
            blendedVar += b.heightVariation * weight;
            totalWeight += weight;
        }

        if (totalWeight > 0) {
            return new float[]{blendedBase / totalWeight, blendedVar / totalWeight};
        }
        return new float[]{64.0f, 15.0f};
    }
}