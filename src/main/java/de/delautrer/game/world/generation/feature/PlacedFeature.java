package de.delautrer.game.world.generation.feature;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.feature.placement.DistributionModel;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;

import java.util.Random;

public class PlacedFeature {
    private final String id;
    private final ConfiguredFeature configuredFeature;
    private final int count;
    private final DistributionModel distribution;
    private final PlacementModifier modifier;

    public PlacedFeature(String id, ConfiguredFeature configuredFeature, int count, DistributionModel distribution, PlacementModifier modifier) {
        this.id = id;
        this.configuredFeature = configuredFeature;
        this.count = count;
        this.distribution = distribution;
        this.modifier = modifier;
    }

    public void generate(Chunk chunk, long seed, Climate.TargetPoint[] chunkClimates) {
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();
        
        long featureSeed = seed ^ id.hashCode() ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L);
        Random rand = new Random(featureSeed);

        if (configuredFeature.isGlobal()) {
            generateGlobal(chunk, chunkX, chunkZ, rand);
        } else {
            generateLocal(chunk, chunkX, chunkZ, rand);
        }
    }

    private void generateGlobal(Chunk chunk, int chunkX, int chunkZ, Random rand) {
        // Iterate over the whole chunk volume
        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                // For Biome Filter
                // Note: To simplify, we skip exact biome check per block for global if we don't have it, 
                // but we could just pass a dummy or use a general biome check.
                // Assuming it's fine without Biome check for MegaVeins, or we implement it later.

                for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                    float prob = distribution.getProbabilityAtY(y);
                    if (prob > 0 && (prob >= 1.0f || rand.nextFloat() < prob)) {
                        configuredFeature.generate(chunk, lx, y, lz, worldX, worldZ, rand, modifier);
                    }
                }
            }
        }
    }

    private void generateLocal(Chunk chunk, int chunkX, int chunkZ, Random rand) {
        for (int i = 0; i < count; i++) {
            int lx = rand.nextInt(Chunk.SIZE);
            int lz = rand.nextInt(Chunk.SIZE);
            int y = distribution.getRandomY(rand);

            int worldX = chunkX * Chunk.SIZE + lx;
            int worldZ = chunkZ * Chunk.SIZE + lz;

            // Biome filter is optional, skipped for now to avoid climate sampling cost here
            // If strictly needed, we would sample the biome here
            // String currentBiomeId = MultiNoiseBiomeRegistry.getBiomeFor(climate).id;
            // if (!modifier.isValidBiome(currentBiomeId)) continue;

            configuredFeature.generate(chunk, lx, y, lz, worldX, worldZ, rand, modifier);
        }
    }
}
