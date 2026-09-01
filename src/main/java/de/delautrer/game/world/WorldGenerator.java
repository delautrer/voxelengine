package de.delautrer.game.world;

import de.delautrer.game.world.generation.biome.*;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.delautrer.game.world.generation.IChunkGenerator;
import de.delautrer.game.world.generation.DefaultChunkGenerator;
import de.delautrer.game.world.generation.FlatChunkGenerator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Collection;

import de.delautrer.game.blocks.Block;

public class WorldGenerator {

    public static class PendingBlock {
        public final int x, y, z;
        public final Block block;
        public final byte state;
        public PendingBlock(int x, int y, int z, Block block, byte state) {
            this.x = x; this.y = y; this.z = z; this.block = block; this.state = state;
        }
    }

    private final long seed;
    private final MultiNoiseChunkGenerator terrainGenerator;
    private final MultiNoiseSurfaceBuilder surfaceBuilder;
    private final Map<Long, Collection<PendingBlock>> pendingCrossChunkBlocks = new ConcurrentHashMap<>();

    private de.delautrer.game.world.persistence.WorldPalette blockPalette;
    private de.delautrer.game.world.persistence.BiomePalette biomePalette;

    private final IChunkGenerator chunkGenerator;

    public void setPalettes(de.delautrer.game.world.persistence.WorldPalette blockPalette, de.delautrer.game.world.persistence.BiomePalette biomePalette) {
        this.blockPalette = blockPalette;
        this.biomePalette = biomePalette;
    }

    public de.delautrer.game.world.persistence.WorldPalette getBlockPalette() {
        return blockPalette;
    }

    public de.delautrer.game.world.persistence.BiomePalette getBiomePalette() {
        return biomePalette;
    }

    public WorldGenerator(long seed, String generatorType, String generatorOptions) {
        this.seed = seed;
        MultiNoiseBiomeRegistry.init();
        FeatureRegistry.init();

        this.terrainGenerator = new MultiNoiseChunkGenerator(seed);
        this.surfaceBuilder = new MultiNoiseSurfaceBuilder(terrainGenerator.getSampler(), seed);

        if ("FLAT".equalsIgnoreCase(generatorType)) {
            this.chunkGenerator = new FlatChunkGenerator(generatorOptions);
        } else {
            this.chunkGenerator = new DefaultChunkGenerator(seed);
        }
    }

    public WorldGenerator(long seed) {
        this(seed, "DEFAULT", "");
    }

    public void generate(Chunk chunk) {
        chunkGenerator.generate(chunk, this);
    }

    public void addPendingBlock(int worldX, int worldY, int worldZ, Block block, byte state) {
        long pos = ChunkManager.packPos(worldX >> 4, worldZ >> 4);
        pendingCrossChunkBlocks.computeIfAbsent(pos, k -> new ConcurrentLinkedQueue<>())
            .add(new PendingBlock(worldX, worldY, worldZ, block, state));
    }

    public MultiNoiseChunkGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    public MultiNoiseSurfaceBuilder getSurfaceBuilder() {
        return surfaceBuilder;
    }

    public Collection<PendingBlock> removePendingBlocks(int chunkX, int chunkZ) {
        return pendingCrossChunkBlocks.remove(ChunkManager.packPos(chunkX, chunkZ));
    }
}