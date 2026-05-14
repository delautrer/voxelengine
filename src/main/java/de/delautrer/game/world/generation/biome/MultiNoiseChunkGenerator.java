package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;

import java.util.Random;

public class MultiNoiseChunkGenerator {

    private final MultiNoiseSampler sampler;
    private final NoiseGenerator shapeNoise3D;
    private final long seed;

    // NEU: Unsere Noise-Generatoren für Flüsse und Höhlen
    private final NoiseGenerator riverNoise;
//    private final NoiseGenerator caveCheeseNoise;
//    private final NoiseGenerator caveSpaghettiNoise;

    public static final int WATER_LEVEL = 0;

    public MultiNoiseChunkGenerator(long seed) {
        this.seed = seed;
        this.sampler = new MultiNoiseSampler(seed);
        this.shapeNoise3D = new NoiseGenerator(seed * 31);

        // Initialisierung mit Offsets zum Seed, damit sie einzigartig sind
        this.riverNoise = new NoiseGenerator(seed * 234);
//        this.caveCheeseNoise = new NoiseGenerator(seed * 567);
//        this.caveSpaghettiNoise = new NoiseGenerator(seed * 890);
    }

    public void generateBaseTerrain(Chunk chunk, int chunkX, int chunkZ) {
        byte stoneId = BlockRegistry.get(Constants.NAMESPACE + ":stone").getId();
        byte waterId = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        byte bedrockId = BlockRegistry.get(Constants.NAMESPACE + ":bedrock").getId();
        byte waterSourceState = (byte) 8;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                float[] params = MultiNoiseBiomeRegistry.getBlendedTerrainParameters(climate);
                float biomeBaseHeight = params[0];
                float biomeVar = params[1];

                float cont = climate.continentalness;
                float erosion = climate.erosion;

                float baseHeight = biomeBaseHeight;
                if (cont > 0.0f) {
                    // Berge entstehen nur, wenn Continentalness UND Erosion hoch sind
                    float mountainFactor = cont * Math.max(0.0f, erosion);
                    baseHeight += (mountainFactor * mountainFactor * 120.0f) * (biomeVar / 10.0f);
                } else {
                    baseHeight += cont * biomeVar * 1.5f;
                }

                float jaggedness = (biomeVar * 0.15f) + (erosion * biomeVar * 0.8f);
                if (cont > 0.4f && erosion > 0.5f) {
                    jaggedness += (cont - 0.4f) * (erosion - 0.5f) * 150.0f; // Extreme Klippen nur im Gebirge
                }

                // --- MINECRAFT FLUSS LOGIK ---
                // 1. Domain Warping (Verbiegt Grid für natürliche Kurven)
                float warpX = riverNoise.getFractalNoise2D(worldX * 0.02f, worldZ * 0.02f, 2, 0.5f, 2.0f) * 25.0f;
                float warpZ = riverNoise.getFractalNoise2D(worldX * 0.02f + 100, worldZ * 0.02f + 100, 2, 0.5f, 2.0f) * 25.0f;

                // 2. Fluss berechnen (mit verbogenen Koordinaten)
                float rNoise = riverNoise.getFractalNoise2D((worldX + warpX) * 0.003f, (worldZ + warpZ) * 0.003f, 3, 0.5f, 2.0f);
                float riverVal = Math.abs(rNoise);
                float riverThreshold = 0.06f; // Breite des Tals

                if (riverVal < riverThreshold) {
                    float riverBlend = 1.0f - (riverVal / riverThreshold);
                    riverBlend = riverBlend * riverBlend * (3.0f - 2.0f * riverBlend); // Smoothstep für weiches Tal

                    // 3. Lerp: Zieht den Berg sanft ins Tal (statt Rille zu schneiden)
                    baseHeight = (baseHeight * (1.0f - riverBlend)) + ((WATER_LEVEL - 3) * riverBlend);
                    jaggedness *= (1.0f - riverBlend); // Talboden wird flach
                }

                if (climate.continentalness < -0.2f) {
                    float oceanFactor = Math.min(1.0f, (-0.2f - climate.continentalness) * 5.0f);
                    baseHeight = (baseHeight * (1.0f - oceanFactor)) + (-24.0f * oceanFactor);
                    jaggedness *= (1.0f - oceanFactor);
                }

                long brSeed = seed ^ ((long) worldX * 314159L ^ (long) worldZ * 271828L);
                Random brRand = new Random(brSeed);
                int bedrockLimit = Chunk.MIN_Y + brRand.nextInt(4);

                // --- FIX: TOP-DOWN SCHLEIFE & WASSER-SCHUTZ ---
                int waterProtection = 0;

                for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                    float baseDensity = baseHeight - y;
                    float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
                    baseDensity += noise3D * jaggedness;

                    float finalDensity = baseDensity;
//                    boolean isCave = false;

                    // Wasser-Check
                    boolean isWater = (y <= WATER_LEVEL && baseDensity <= 0);
                    if (riverVal < riverThreshold && y <= (MultiNoiseChunkGenerator.WATER_LEVEL - 3)) isWater = true;

                    if (isWater) {
                        waterProtection = 5;
                    } else if (waterProtection > 0) {
                        waterProtection--;
                    }

/*
                    // --- MODERNE HÖHLEN (Cheese + Noodle Netze) ---
                    if (baseDensity > 0 && y > bedrockLimit + 2 && waterProtection == 0) {

                        // 1. CHEESE CAVES (Riesenhallen)
                        // Skalierung geändert (0.012f) für gigantische Hallen
                        float cheese = caveCheeseNoise.getFractalNoise3D(worldX * 0.012f, y * 0.012f, worldZ * 0.012f, 2, 0.5f, 2.0f);

                        // Dynamischer Threshold: Unten leicht (0.35), Oben schwer (0.6) -> Macht sanfte Höhleneingänge
                        float depthFactor = (float) (y - Chunk.MIN_Y) / Chunk.HEIGHT;
                        float currentThreshold = 0.35f + (depthFactor * 0.25f);

                        boolean isCheese = cheese > currentThreshold;

                        // 2. NOODLE CAVES (Verbindungsnetzwerke)
                        float noodle1 = caveSpaghettiNoise.getFractalNoise3D(worldX * 0.02f, y * 0.02f, worldZ * 0.02f, 2, 0.5f, 2.0f);
                        float noodle2 = caveSpaghettiNoise.getFractalNoise3D(worldX * 0.02f + 100, y * 0.02f + 100, worldZ * 0.02f + 100, 2, 0.5f, 2.0f);
                        boolean isNoodle = Math.abs(noodle1) < 0.045f && Math.abs(noodle2) < 0.045f;

                        if (isCheese || isNoodle) {
                            finalDensity = -1.0f;
                            isCave = true;
                        }
                    }
*/

                    // --- SETZEN ---
                    if (y <= bedrockLimit) {
                        chunk.setBlock(lx, y, lz, bedrockId);
                    } else if (finalDensity > 0) {
                        chunk.setBlock(lx, y, lz, stoneId);
                    } else if (isWater && baseDensity <= 0) {
                        chunk.setBlock(lx, y, lz, waterId, waterSourceState);
                    }
                }
            }
        }
    }
    public MultiNoiseSampler getSampler() {
        return sampler;
    }
}