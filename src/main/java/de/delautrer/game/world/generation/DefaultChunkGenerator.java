package de.delautrer.game.world.generation;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.WorldGenerator;
import de.delautrer.game.world.generation.biome.CaveCarver;
import de.delautrer.game.world.generation.feature.FeatureRegistry;

import java.util.Collection;
public class DefaultChunkGenerator implements IChunkGenerator {
    private final long seed;

    public DefaultChunkGenerator(long seed) {
        this.seed = seed;
    }

    @Override
    public void generate(Chunk chunk, WorldGenerator worldGenerator) {
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        worldGenerator.getTerrainGenerator().generateBaseTerrain(chunk, chunkX, chunkZ);
        CaveCarver.carve(chunk, seed, worldGenerator.getTerrainGenerator().getSampler());
        worldGenerator.getSurfaceBuilder().buildSurface(chunk, chunkX, chunkZ, worldGenerator);

        // Apply pending blocks from neighbors
        Collection<WorldGenerator.PendingBlock> pending = worldGenerator.removePendingBlocks(chunkX, chunkZ);
        if (pending != null) {
            for (WorldGenerator.PendingBlock pb : pending) {
                if (pb.y >= Chunk.MIN_Y && pb.y < Chunk.MAX_Y) {
                    int lx = pb.x & 15;
                    int lz = pb.z & 15;
                    byte existing = chunk.getBlock(lx, pb.y, lz);
                    if (existing == 0) {
                        chunk.setBlock(lx, pb.y, lz, pb.blockId, pb.state);
                    } else {
                        de.delautrer.game.blocks.Block b = de.delautrer.game.blocks.BlockRegistry.get(existing);
                        if (b instanceof de.delautrer.game.blocks.PlantBlock || b instanceof de.delautrer.game.blocks.LeavesBlock) {
                            chunk.setBlock(lx, pb.y, lz, pb.blockId, pb.state);
                        }
                    }
                }
            }
        }
        
        FeatureRegistry.generateOres(chunk, seed);
    }
}
