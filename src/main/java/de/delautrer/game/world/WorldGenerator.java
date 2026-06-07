package de.delautrer.game.world;

import de.delautrer.game.world.generation.biome.*;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldGenerator {

    public static class PendingBlock {
        public final int x, y, z;
        public final byte blockId, state;
        public PendingBlock(int x, int y, int z, byte blockId, byte state) {
            this.x = x; this.y = y; this.z = z; this.blockId = blockId; this.state = state;
        }
    }

    private final long seed;
    private final MultiNoiseChunkGenerator terrainGenerator;
    private final MultiNoiseSurfaceBuilder surfaceBuilder;
    private final Map<Long, List<PendingBlock>> pendingCrossChunkBlocks = new ConcurrentHashMap<>();

    public WorldGenerator(long seed) {
        this.seed = seed;
        MultiNoiseBiomeRegistry.init();
        FeatureRegistry.init();

        // Neues Multi-Noise System initialisieren
        this.terrainGenerator = new MultiNoiseChunkGenerator(seed);
        this.surfaceBuilder = new MultiNoiseSurfaceBuilder(terrainGenerator.getSampler(), seed);
    }

    public void generate(Chunk chunk) {
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        terrainGenerator.generateBaseTerrain(chunk, chunkX, chunkZ);
        CaveCarver.carve(chunk, seed, terrainGenerator.getSampler());
        surfaceBuilder.buildSurface(chunk, chunkX, chunkZ, this);

        // Apply pending blocks from neighbors (e.g. tree crowns that spilled over)
        List<PendingBlock> pending = pendingCrossChunkBlocks.remove(ChunkManager.packPos(chunkX, chunkZ));
        if (pending != null) {
            for (PendingBlock pb : pending) {
                if (pb.y >= Chunk.MIN_Y && pb.y < Chunk.MAX_Y) {
                    chunk.setBlock(pb.x & 15, pb.y, pb.z & 15, pb.blockId, pb.state);
                }
            }
        }
        
        // Generiere Erze (Features) nach der Oberfläche
        FeatureRegistry.generateOres(chunk, seed);
    }

    public void addPendingBlock(int worldX, int worldY, int worldZ, byte blockId, byte state) {
        long pos = ChunkManager.packPos(worldX >> 4, worldZ >> 4);
        pendingCrossChunkBlocks.computeIfAbsent(pos, k -> new CopyOnWriteArrayList<>())
            .add(new PendingBlock(worldX, worldY, worldZ, blockId, state));
    }

    public MultiNoiseChunkGenerator getTerrainGenerator() {
        return terrainGenerator;
    }
}