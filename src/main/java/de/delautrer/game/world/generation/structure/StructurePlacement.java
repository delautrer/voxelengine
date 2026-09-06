package de.delautrer.game.world.generation.structure;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.LeavesBlock;
import de.delautrer.game.blocks.LogBlock;
import de.delautrer.game.blocks.PlantBlock;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.WorldGenerator;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseChunkGenerator;

import java.util.Random;

public class StructurePlacement {

    public static class OriginResult {
        public final int originX;
        public final int originY;
        public final int originZ;
        public final Biome biome;

        public OriginResult(int originX, int originY, int originZ, Biome biome) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.biome = biome;
        }
    }

    public static OriginResult computeOrigin(WorldGenerator wg, Chunk currentChunk, Structure structure, StructureTemplate template, int ownerCX, int ownerCZ, long seed, long salt) {
        if (structure == null || template == null || wg == null) return null;

        Random rand = new Random(seed ^ ((long) ownerCX * 1234567L ^ (long) ownerCZ * 7654321L ^ salt));
        int originX = ownerCX * 16 + (rand.nextInt(12) + 2);
        int originZ = ownerCZ * 16 + (rand.nextInt(12) + 2);

        int sizeX = template.getSizeX();
        int sizeZ = template.getSizeZ();

        // Measure solidY at the 4 footprint corners
        int h00 = getSolidTopY(wg, currentChunk, originX, originZ);
        int h10 = getSolidTopY(wg, currentChunk, originX + sizeX - 1, originZ);
        int h01 = getSolidTopY(wg, currentChunk, originX, originZ + sizeZ - 1);
        int h11 = getSolidTopY(wg, currentChunk, originX + sizeX - 1, originZ + sizeZ - 1);

        if (h00 == Chunk.MIN_Y || h10 == Chunk.MIN_Y || h01 == Chunk.MIN_Y || h11 == Chunk.MIN_Y) {
            return null;
        }

        // If any corner solidY <= WATER_LEVEL: skip
        if (h00 <= MultiNoiseChunkGenerator.WATER_LEVEL || h10 <= MultiNoiseChunkGenerator.WATER_LEVEL ||
            h01 <= MultiNoiseChunkGenerator.WATER_LEVEL || h11 <= MultiNoiseChunkGenerator.WATER_LEVEL) {
            return null;
        }

        // If maxCorner - minCorner > maxTilt: skip
        int minCorner = Math.min(Math.min(h00, h10), Math.min(h01, h11));
        int maxCorner = Math.max(Math.max(h00, h10), Math.max(h01, h11));
        int maxTilt = 2;
        if (maxCorner - minCorner > maxTilt) {
            return null;
        }

        // originY = minCorner (floor replaces surface block, no air gap)
        int originY = minCorner;

        // Sample biome at (originX + sizeX/2, originZ + sizeZ/2), not chunk (8,8)
        int centerX = originX + sizeX / 2;
        int centerZ = originZ + sizeZ / 2;
        Climate.TargetPoint climate = wg.getTerrainGenerator().getSampler().sample(centerX, centerZ);
        Biome centerBiome = MultiNoiseBiomeRegistry.getBiomeFor(climate);

        if (centerBiome == null || centerBiome.id == null) {
            return null;
        }

        NamespacedKey bKey = centerBiome.id.contains(":") ? NamespacedKey.fromString(centerBiome.id) : NamespacedKey.fromString("veinstride:" + centerBiome.id);
        if (!structure.isBiomeAllowed(bKey)) {
            return null;
        }

        return new OriginResult(originX, originY, originZ, centerBiome);
    }

    public static int getSolidTopY(WorldGenerator wg, Chunk currentChunk, int worldX, int worldZ) {
        if (currentChunk != null && currentChunk.getWorldX() == (worldX >> 4) && currentChunk.getWorldZ() == (worldZ >> 4)) {
            int lx = worldX & 15;
            int lz = worldZ & 15;
            for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                Block b = currentChunk.getBlock(lx, y, lz);
                if (b != null && b.isSolid && !(b instanceof WaterBlock) && !(b instanceof PlantBlock) && !(b instanceof LeavesBlock) && !(b instanceof LogBlock)) {
                    return y;
                }
            }
            return Chunk.MIN_Y;
        }

        if (wg != null && wg.getTerrainGenerator() != null) {
            return wg.getTerrainGenerator().getSolidTopY(worldX, worldZ);
        }

        return Chunk.MIN_Y;
    }
}
