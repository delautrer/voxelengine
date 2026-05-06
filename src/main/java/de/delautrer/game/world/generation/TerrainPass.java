package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.BiomeProvider;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;

public class TerrainPass implements IGenerationPass {

    private final BiomeProvider biomeProvider;
    private final NoiseGenerator elevationNoise;
    private final NoiseGenerator roughnessNoise;
    private final NoiseGenerator detailNoise;
    private final NoiseGenerator riverNoise;
    private final NoiseGenerator lakeNoise;

    private final BlockState air;
    private final BlockState bedrock;
    private final BlockState water;

    private static final int WATER_LEVEL = 60;

    public TerrainPass(long seed) {
        this.biomeProvider = new BiomeProvider(seed);
        this.elevationNoise = new NoiseGenerator(seed);
        this.roughnessNoise = new NoiseGenerator(seed * 2);
        this.detailNoise = new NoiseGenerator(seed * 3);
        this.riverNoise = new NoiseGenerator(seed * 4444L);
        this.lakeNoise = new NoiseGenerator(seed * 5555L);

        // FALLBACK: Falls die BlockRegistry aus irgendeinem Grund spinnt, fangen wir das ab
        BlockState tmpAir = null, tmpBedrock = null, tmpWater = null;
        try {
            tmpAir = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
            tmpBedrock = BlockRegistry.get(Constants.NAMESPACE + ":bedrock").getDefaultState();
            tmpWater = BlockRegistry.get(Constants.NAMESPACE + ":water").getDefaultState().with(WaterBlock.LEVEL, 8);
        } catch (Exception e) {
            System.err.println("[TerrainPass] Warnung: Konnte Basisblöcke nicht laden!");
            // In diesem fatalen Fall ist das Spiel ohnehin hinüber, aber wir werfen keine stumme NPE
        }
        this.air = tmpAir;
        this.bedrock = tmpBedrock;
        this.water = tmpWater;
    }

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (chunk == null || heightMap == null || air == null) return; // Sicher ist sicher

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int realX = worldX * Chunk.SIZE + x;
                int realZ = worldZ * Chunk.SIZE + z;

                float elevation = elevationNoise.getFractalNoise2D(realX * 0.0015f, realZ * 0.0015f, 4, 0.5f, 2.0f);
                float roughness = roughnessNoise.getFractalNoise2D(realX * 0.002f, realZ * 0.002f, 4, 0.5f, 2.0f);
                float detail = detailNoise.getFractalNoise2D(realX * 0.01f, realZ * 0.01f, 4, 0.5f, 2.0f);

                float baseHeight = 64.0f + (elevation * 40.0f);
                float localRoughness = 10.0f;

                float mountainFactor = Math.max(0.0f, roughness * 2.7f);
                baseHeight += (float)Math.pow(mountainFactor, 3.0) * 160.0f;
                localRoughness += mountainFactor * 60.0f;

                float riverVal = Math.abs(riverNoise.getFractalNoise2D(realX * 0.001f, realZ * 0.001f, 3, 0.5f, 2.0f));
                float valleyWidth = 0.04f + (Math.max(0.0f, baseHeight - WATER_LEVEL) * 0.0025f);

                if (riverVal < valleyWidth) {
                    float t = 1.0f - (riverVal / valleyWidth);
                    float carveForce = (float) Math.pow(t, 2.0);

                    float riverBottom = WATER_LEVEL - 3.0f;
                    baseHeight = baseHeight * (1.0f - carveForce) + riverBottom * carveForce;
                    localRoughness *= (1.0f - carveForce);
                }

                float lakeVal = lakeNoise.getFractalNoise2D(realX * 0.003f, realZ * 0.003f, 3, 0.5f, 2.0f);
                if (lakeVal > 0.3f && riverVal >= valleyWidth) {
                    float t = Math.min(1.0f, (lakeVal - 0.3f) * 4.0f);
                    float carveForce = t * t * (3.0f - 2.0f * t);

                    float lakeBottom = WATER_LEVEL - 5.0f;
                    baseHeight = baseHeight * (1.0f - carveForce) + lakeBottom * carveForce;
                    localRoughness *= (1.0f - carveForce);
                }

                int terrainHeight = (int) (baseHeight + (detail * localRoughness));
                terrainHeight = Math.min(Chunk.HEIGHT - 2, Math.max(1, terrainHeight));
                heightMap[x][z] = terrainHeight;

                Biome biome = biomeProvider.getBiome(realX, realZ, elevation, roughness);
                if (biome == null) biome = Biome.PLAINS; // Fallback
                chunk.setBiome(x, z, biome);

                BlockState deepMaterial = biome.getDeepMaterial();
                if (deepMaterial == null) deepMaterial = BlockRegistry.get(Constants.NAMESPACE + ":stone").getDefaultState(); // Fallback

                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, bedrock.getBlock().getId(), bedrock.getStateId());
                    } else if (y <= terrainHeight) {
                        chunk.setBlock(x, y, z, deepMaterial.getBlock().getId(), deepMaterial.getStateId());
                    } else if (y <= WATER_LEVEL) {
                        chunk.setBlock(x, y, z, water.getBlock().getId(), water.getStateId());
                    } else {
                        chunk.setBlock(x, y, z, air.getBlock().getId(), air.getStateId());
                    }
                }
            }
        }
    }
}