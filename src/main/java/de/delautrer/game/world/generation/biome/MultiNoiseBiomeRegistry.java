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
    private static volatile List<Biome> BIOMES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;
    private static Biome FALLBACK_BIOME;

    public static final de.delautrer.game.registry.Registry<Biome> BIOME_REGISTRY = new de.delautrer.game.registry.Registry<>();



    public static synchronized void init() {
        if (isInitialized) return;

        if (FALLBACK_BIOME == null) {
            FALLBACK_BIOME = new Biome();
            FALLBACK_BIOME.id = "veinstride:plains";
            FALLBACK_BIOME.temperature = new float[]{-1f, 1f};
            FALLBACK_BIOME.humidity = new float[]{-1f, 1f};
            FALLBACK_BIOME.continentalness = new float[]{-1f, 1f};
            FALLBACK_BIOME.erosion = new float[]{-1f, 1f};
            FALLBACK_BIOME.weirdness = new float[]{-1f, 1f};
            FALLBACK_BIOME.topBlock = "grass_block";
            FALLBACK_BIOME.underBlock = "dirt";
            FALLBACK_BIOME.underwaterBlock = "sand";
        }

        List<Biome> loadedList = new ArrayList<>();
        List<String> files = de.delautrer.engine.utils.ResourceUtils.listResources("assets/data/veinstride/worldgen/biome", ".json");
        if (files == null || files.isEmpty()) {
            throw new IllegalStateException("Critical Error: No biome JSON files found in assets/data/veinstride/worldgen/biome!");
        }

        for (String file : files) {
            if (file == null || file.trim().isEmpty()) continue;
            String path = file.endsWith(".json") ? file.substring(0, file.length() - 5).replace('\\', '/').toLowerCase() : file.replace('\\', '/').toLowerCase();
            if (path.trim().isEmpty()) continue;
            de.delautrer.game.registry.NamespacedKey key = new de.delautrer.game.registry.NamespacedKey(de.delautrer.Constants.NAMESPACE, path);
            try {
                java.io.Reader reader = de.delautrer.engine.utils.ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/biome/" + file);
                Biome biome = GSON.fromJson(reader, Biome.class);
                if (biome != null) {
                    biome.id = key.toString();
                    if ("grass".equals(biome.topBlock)) biome.topBlock = "grass_block";
                    BIOME_REGISTRY.register(key, biome);
                    loadedList.add(biome);
                }
            } catch (Exception e) {
                System.err.println("[MultiNoiseBiomeRegistry] Failed to load biome: " + file);
                throw new IllegalStateException("Failed to load biome file: " + file, e);
            }
        }

        if (!loadedList.isEmpty()) {
            BIOMES = Collections.unmodifiableList(loadedList);
        } else {
            BIOME_REGISTRY.register(de.delautrer.game.registry.NamespacedKey.fromString("veinstride:plains"), FALLBACK_BIOME);
            BIOMES = new ArrayList<>(Collections.singletonList(FALLBACK_BIOME));
        }
        isInitialized = true;
        System.out.println("Loaded " + BIOMES.size() + " biomes.");
    }

    public static Biome getBiomeFor(Climate.TargetPoint point) {
        if (!isInitialized) init();
        List<Biome> currentBiomes = BIOMES;
        if (currentBiomes == null || currentBiomes.isEmpty()) return FALLBACK_BIOME;

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
        if (currentBiomes.isEmpty()) return new float[]{0.0f, 15.0f};

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
        return new float[]{0.0f, 15.0f};
    }
}