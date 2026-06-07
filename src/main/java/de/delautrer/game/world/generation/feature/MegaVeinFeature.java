package de.delautrer.game.world.generation.feature;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;
import java.util.Random;

public class MegaVeinFeature extends ConfiguredFeature {
    private final byte carrierId;
    private final double oreChance;
    
    // Wir initialisieren den NoiseGenerator erst, wenn wir ihn brauchen, oder wir nutzen einen fixen Seed.
    // Da es Data-Driven ist, erstellen wir ihn on-the-fly mit einem Hash des BlockNamens als Seed Offset
    private NoiseGenerator noiseGenerator;
    private boolean noiseInitialized = false;

    public MegaVeinFeature(byte blockId, byte carrierId, double oreChance) {
        super(blockId);
        this.carrierId = carrierId;
        this.oreChance = oreChance;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void generate(Chunk chunk, int lx, int y, int lz, int worldX, int worldZ, Random rand, PlacementModifier modifier) {
        if (!noiseInitialized) {
            // Seed base on block ID to make different mega veins look different
            long noiseSeed = 12345L + blockId * 789L;
            this.noiseGenerator = new NoiseGenerator(noiseSeed);
            this.noiseInitialized = true;
        }

        // Simplex/Perlin 3D Noise Evaluation
        // Scale factors: 0.02 is a good scale for large winding structures
        float scale = 0.02f;
        float noiseVal = noiseGenerator.getFractalNoise3D(worldX * scale, y * scale, worldZ * scale, 2, 0.5f, 2.0f);
        
        // Threshold: very narrow (snake-like)
        if (Math.abs(noiseVal) < 0.035f) {
            if (modifier.canReplace(chunk, lx, y, lz, rand)) {
                if (rand.nextDouble() < oreChance) {
                    chunk.setBlock(lx, y, lz, getVariantBlockId(carrierId));
                } else {
                    chunk.setBlock(lx, y, lz, carrierId);
                }
            }
        }
    }
}
