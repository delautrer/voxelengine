package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.persistence.WorldPalette;
import java.util.Random;

public class TreeFeature {

    public enum TreeShape {
        STANDARD, PINE, TALL_PINE, WILLOW, PALM, BAOBAB, MAHOGANY
    }

    public static final byte LOG_VERTICAL = 1;

    @FunctionalInterface
    private interface BlockSetter {
        void setBlock(int x, int y, int z, Block block, byte state);
    }

    public static void generate(Chunk targetChunk, de.delautrer.game.world.WorldGenerator wg, int worldX, int worldY, int worldZ, long worldSeed, TreeShape shape, Block log, Block leaves, int baseHeight, int heightVariation) {
        BlockSetter setter = (x, y, z, b, state) -> setBlockIfInChunk(targetChunk, wg, x, y, z, b, state);
        generateInternal(setter, worldX, worldY, worldZ, worldSeed, shape, log, leaves, baseHeight, heightVariation);
    }

    public static void generate(World world, int worldX, int worldY, int worldZ, long worldSeed, TreeShape shape, Block log, Block leaves, int baseHeight, int heightVariation) {
        BlockSetter setter = (x, y, z, b, state) -> {
            if (y >= Chunk.MIN_Y && y < Chunk.MAX_Y) {
                Block existing = world.getBlock(x, y, z);
                if (canReplace(existing)) world.setBlock(x, y, z, b, state);
            }
        };
        generateInternal(setter, worldX, worldY, worldZ, worldSeed, shape, log, leaves, baseHeight, heightVariation);
    }

    private static void generateInternal(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, TreeShape shape, Block log, Block leaves, int baseHeight, int heightVariation) {
        if (shape == null) shape = TreeShape.STANDARD;
        if (baseHeight <= 0) baseHeight = 4;
        if (heightVariation <= 0) heightVariation = 3;

        switch (shape) {
            case PINE: generatePine(setter, worldX, worldY, worldZ, worldSeed, log, leaves, baseHeight, heightVariation, false); break;
            case TALL_PINE: generatePine(setter, worldX, worldY, worldZ, worldSeed, log, leaves, baseHeight, heightVariation, true); break;
            case WILLOW: generateWillow(setter, worldX, worldY, worldZ, worldSeed, log, leaves); break;
            case PALM: generatePalm(setter, worldX, worldY, worldZ, worldSeed, log, leaves); break;
            case BAOBAB: generateBaobab(setter, worldX, worldY, worldZ, worldSeed, log, leaves); break;
            case MAHOGANY: generateMahogany(setter, worldX, worldY, worldZ, worldSeed, log, leaves); break;
            case STANDARD:
            default: generateStandard(setter, worldX, worldY, worldZ, worldSeed, log, leaves, baseHeight, heightVariation); break;
        }
    }

    private static void generateStandard(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves, int baseHeight, int varHeight) {
        Random random = new Random(worldSeed ^ ((long) worldX * 31234567L ^ (long) worldZ * 11612345L ^ (long) worldY * 99999L));
        int height = baseHeight + random.nextInt(Math.max(1, varHeight));
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, log, LOG_VERTICAL);
        int crownBottom = worldY + height - 3;
        int crownTop = worldY + height + 1;
        for (int y = crownBottom; y <= crownTop; y++) {
            int radius = (y >= worldY + height) ? 1 : 2;
            for (int x = worldX - radius; x <= worldX + radius; x++) {
                for (int z = worldZ - radius; z <= worldZ + radius; z++) {
                    if (x == worldX && z == worldZ && y < worldY + height) continue;
                    if (Math.abs(x - worldX) == radius && Math.abs(z - worldZ) == radius && (y == crownTop || random.nextBoolean())) continue;
                    setter.setBlock(x, y, z, leaves, (byte) 0);
                }
            }
        }
    }

    private static void generatePine(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves, int baseHeight, int varHeight, boolean isTall) {
        Random random = new Random(worldSeed ^ ((long) worldX * 7312345L ^ (long) worldZ * 91612345L));
        int totalHeight = baseHeight + random.nextInt(Math.max(1, varHeight));
        
        int leafStartY = isTall ? (4 + random.nextInt(3)) : (1 + random.nextInt(2));
        int crownHeight = totalHeight - leafStartY;
        if (crownHeight < 5) crownHeight = 5;

        int trunkHeight = totalHeight - 3;
        if (trunkHeight < leafStartY) trunkHeight = leafStartY;

        for (int i = 0; i < trunkHeight; i++) {
            setter.setBlock(worldX, worldY + i, worldZ, log, LOG_VERTICAL);
        }

        int numSections = (crownHeight > 8) ? 3 : 2;
        float sectionH = (float)crownHeight / numSections;
        
        for (int s = 0; s < numSections; s++) {
            int sectionBottom = worldY + leafStartY + (int)(s * sectionH);
            int sectionTop = sectionBottom + (int)sectionH;
            if (s == numSections - 1) sectionTop = worldY + totalHeight;
            
            int maxRadius = numSections - s;
            if (maxRadius < 1) maxRadius = 1;
            
            for (int y = sectionBottom; y <= sectionTop; y++) {
                float t = (float)(y - sectionBottom) / (sectionTop - sectionBottom + 1);
                int r = Math.round(maxRadius * (1.0f - t));
                if (r < 0) r = 0;
                
                for (int x = worldX - r; x <= worldX + r; x++) {
                    for (int z = worldZ - r; z <= worldZ + r; z++) {
                        if (y < worldY) continue; 
                        if (x == worldX && z == worldZ && y < worldY + trunkHeight) continue;
                        
                        if (Math.abs(x - worldX) + Math.abs(z - worldZ) <= r + (y == sectionBottom ? 1 : 0)) {
                            setter.setBlock(x, y, z, leaves, (byte) 0);
                        }
                    }
                }
            }
        }
        
        for (int y = Math.max(trunkHeight, worldY); y <= worldY + totalHeight; y++) {
            setter.setBlock(worldX, y, worldZ, leaves, (byte) 0);
        }
    }

    private static void generateWillow(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves) {
        Random random = new Random(worldSeed ^ ((long) worldX * 12345L ^ (long) worldZ * 67890L));
        int height = 5 + random.nextInt(2);
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, log, LOG_VERTICAL);
        int crownY = worldY + height - 1;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 1; y++) {
                    double d = x*x + z*z + y*y*2;
                    if (d < 10) {
                        setter.setBlock(worldX + x, crownY + y, worldZ + z, leaves, (byte) 0);
                        if (d > 6 && random.nextFloat() < 0.7f) {
                            for (int h = 1; h <= 2 + random.nextInt(2); h++) setter.setBlock(worldX + x, crownY + y - h, worldZ + z, leaves, (byte) 0);
                        }
                    }
                }
            }
        }
    }

    private static void generatePalm(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves) {
        Random random = new Random(worldSeed ^ ((long) worldX * 999L ^ (long) worldZ * 888L));
        int height = 7 + random.nextInt(5);
        double curveX = 0, curveZ = 0;
        double speedX = (random.nextDouble() - 0.5) * 0.5;
        double speedZ = (random.nextDouble() - 0.5) * 0.5;
        for (int i = 0; i < height; i++) {
            setter.setBlock(worldX + (int)Math.round(curveX), worldY + i, worldZ + (int)Math.round(curveZ), log, LOG_VERTICAL);
            curveX += speedX;
            curveZ += speedZ;
            speedX += (random.nextDouble() - 0.5) * 0.15;
            speedZ += (random.nextDouble() - 0.5) * 0.15;
        }
        int topX = worldX + (int)Math.round(curveX);
        int topZ = worldZ + (int)Math.round(curveZ);
        int topY = worldY + height - 1;
        int numLeaves = 12 + random.nextInt(8);
        for (int i = 0; i < numLeaves; i++) {
            double angle = (i * Math.PI * 2) / numLeaves + (random.nextDouble() * 0.2);
            int len = 3 + random.nextInt(3);
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);
            for (int l = 1; l <= len; l++) {
                int lx = topX + (int)Math.round(vx * l);
                int lz = topZ + (int)Math.round(vz * l);
                int ly = topY + 1;
                if (l >= 3) ly -= 1;
                if (l >= 5) ly -= 1;
                setter.setBlock(lx, ly, lz, leaves, (byte) 0);
            }
        }
        setter.setBlock(topX, worldY + height, topZ, leaves, (byte) 0);
    }

    private static void generateBaobab(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves) {
        Random random = new Random(worldSeed ^ ((long) worldX * 555L ^ (long) worldZ * 444L));
        int height = 5 + random.nextInt(8);
        boolean isBig = height > 8;
        for (int i = 0; i < height; i++) {
            double baseRadius = (isBig ? 1.8 : 1.2) - (i * 0.05);
            if (i < 2) baseRadius += 0.4;
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    double dist = x*x + z*z;
                    double noise = new Random(worldSeed + i + (long)x*10 + (long)z*20).nextDouble() * 0.5;
                    if (dist <= baseRadius * baseRadius + noise) {
                        setter.setBlock(worldX + x, worldY + i, worldZ + z, log, LOG_VERTICAL);
                    }
                }
            }
        }
        int topY = worldY + height;
        Random crownRand = new Random(worldSeed ^ 9999L);
        int crownRadius = isBig ? 6 : 4;
        for (int x = -crownRadius; x <= crownRadius; x++) {
            for (int z = -crownRadius; z <= crownRadius; z++) {
                double d = x*x + z*z;
                double threshold = (crownRadius * crownRadius) * 0.8 + crownRand.nextInt(6);
                if (d < threshold) {
                    setter.setBlock(worldX + x, topY, worldZ + z, leaves, (byte) 0);
                    if (d < threshold * 0.6) setter.setBlock(worldX + x, topY + 1, worldZ + z, leaves, (byte) 0);
                }
            }
        }
    }

    private static void generateMahogany(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, Block log, Block leaves) {
        Random random = new Random(worldSeed ^ ((long) worldX * 111L ^ (long) worldZ * 222L));
        int height = 8 + random.nextInt(4);
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, log, LOG_VERTICAL);
        int crownY = worldY + height - 2;
        for (int r = 4; r >= 1; r--) {
            int y = crownY + (4 - r);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x*x + z*z <= r*r) setter.setBlock(worldX + x, y, worldZ + z, leaves, (byte) 0);
                }
            }
        }
    }

    private static void setBlockIfInChunk(Chunk chunk, de.delautrer.game.world.WorldGenerator wg, int worldX, int worldY, int worldZ, Block block, byte state) {
        int lx = worldX - chunk.getWorldX() * Chunk.SIZE;
        int lz = worldZ - chunk.getWorldZ() * Chunk.SIZE;
        WorldPalette palette = null;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE && worldY >= Chunk.MIN_Y && worldY < Chunk.MAX_Y) {
            Block existing = chunk.getBlock(lx, worldY, lz, palette);
            
            if (state == LOG_VERTICAL) {
                Block grassBlock = Registries.BLOCKS.get("veinstride:grass_block");
                Block sGrassBlock = Registries.BLOCKS.get("veinstride:sandy_grass");
                Block dirtBlock = Registries.BLOCKS.get("veinstride:dirt");
                Block sandBlock = Registries.BLOCKS.get("veinstride:sand");
                
                if (existing == grassBlock || existing == sGrassBlock || existing == dirtBlock || existing == sandBlock) {
                    chunk.setBlock(lx, worldY, lz, block, state, palette);
                    if (worldY - 1 >= Chunk.MIN_Y) {
                        Block below = chunk.getBlock(lx, worldY - 1, lz, palette);
                        if (below == grassBlock || below == sGrassBlock) {
                            chunk.setBlock(lx, worldY - 1, lz, dirtBlock, (byte) 0, palette);
                        }
                    }
                } else if (canReplace(existing)) {
                    chunk.setBlock(lx, worldY, lz, block, state, palette);
                    
                    for (int d = 1; d <= 256; d++) {
                        int dy = worldY - d;
                        if (dy < Chunk.MIN_Y) break;
                        Block below = chunk.getBlock(lx, dy, lz, palette);
                        if (below == block) break;
                        
                        if (canReplace(below)) {
                            chunk.setBlock(lx, dy, lz, block, state, palette);
                        } else {
                            if (below == grassBlock || below == sGrassBlock) {
                                chunk.setBlock(lx, dy, lz, dirtBlock, (byte) 0, palette);
                            }
                            break;
                        }
                    }
                }
            } else {
                if (canReplace(existing)) chunk.setBlock(lx, worldY, lz, block, state, palette);
            }
        } else if (wg != null) {
            wg.addPendingBlock(worldX, worldY, worldZ, block, state);
        }
    }

    private static boolean canReplace(Block block) {
        if (block == null || block.isAir()) return true;
        return block instanceof de.delautrer.game.blocks.PlantBlock || block.isPassable;
    }
}
