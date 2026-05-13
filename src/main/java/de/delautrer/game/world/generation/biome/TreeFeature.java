package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.Block;
import de.delautrer.Constants;
import java.util.Random;

public class TreeFeature {

    public static final byte LOG_VERTICAL = 1;

    @FunctionalInterface
    private interface BlockSetter {
        void setBlock(int x, int y, int z, byte id, byte state);
    }

    public static void generate(Chunk targetChunk, int worldX, int worldY, int worldZ, long worldSeed, String treeType, byte logId, byte leavesId) {
        BlockSetter setter = (x, y, z, id, state) -> setBlockIfInChunk(targetChunk, x, y, z, id, state);
        generateInternal(setter, worldX, worldY, worldZ, worldSeed, treeType, logId, leavesId);
    }

    public static void generate(World world, int worldX, int worldY, int worldZ, long worldSeed, String treeType, byte logId, byte leavesId) {
        BlockSetter setter = (x, y, z, id, state) -> {
            if (y >= Chunk.MIN_Y && y < Chunk.MAX_Y) {
                byte existing = world.getBlockAt(x, y, z);
                if (canReplace(existing)) world.setBlockWithState(x, y, z, id, state);
            }
        };
        generateInternal(setter, worldX, worldY, worldZ, worldSeed, treeType, logId, leavesId);
    }

    private static void generateInternal(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, String treeType, byte logId, byte leavesId) {
        if (treeType == null) treeType = "alpha_oak";
        
        switch (treeType) {
            case "alpha_pine": generatePine(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId, 8, 4, false); break;
            case "alpha_tall_pine": generatePine(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId, 14, 6, true); break;
            case "alpha_willow": generateWillow(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId); break;
            case "alpha_palm": generatePalm(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId); break;
            case "alpha_baobab": generateBaobab(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId); break;
            case "alpha_mahogany": generateMahogany(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId); break;
            case "alpha_tall_oak": generateStandard(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId, 10, 5); break;
            case "alpha_tall_birch": generateStandard(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId, 10, 5); break;
            case "alpha_oak":
            case "alpha_birch":
            default: generateStandard(setter, worldX, worldY, worldZ, worldSeed, logId, leavesId, 4, 3); break;
        }
    }

    private static void generateStandard(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId, int baseHeight, int varHeight) {
        Random random = new Random(worldSeed ^ ((long) worldX * 31234567L ^ (long) worldZ * 11612345L ^ (long) worldY * 99999L));
        int height = baseHeight + random.nextInt(varHeight);
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, logId, LOG_VERTICAL);
        int crownBottom = worldY + height - 3;
        int crownTop = worldY + height + 1;
        for (int y = crownBottom; y <= crownTop; y++) {
            int radius = (y >= worldY + height) ? 1 : 2;
            for (int x = worldX - radius; x <= worldX + radius; x++) {
                for (int z = worldZ - radius; z <= worldZ + radius; z++) {
                    if (x == worldX && z == worldZ && y < worldY + height) continue;
                    if (Math.abs(x - worldX) == radius && Math.abs(z - worldZ) == radius && (y == crownTop || random.nextBoolean())) continue;
                    setter.setBlock(x, y, z, leavesId, (byte) 0);
                }
            }
        }
    }

    /**
     * Fichte (Pine) - Verbessertes Modell mit "Tall" Logik
     */
    private static void generatePine(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId, int baseHeight, int varHeight, boolean isTall) {
        Random random = new Random(worldSeed ^ ((long) worldX * 7312345L ^ (long) worldZ * 91612345L));
        int totalHeight = baseHeight + random.nextInt(varHeight);
        
        // Tall Pines haben einfach einen längeren nackten Stamm unten
        int leafStartY = isTall ? (4 + random.nextInt(3)) : (1 + random.nextInt(2));
        int crownHeight = totalHeight - leafStartY;
        if (crownHeight < 5) crownHeight = 5;

        // Der Stamm geht bis zum Anfang der obersten Sektion
        int trunkHeight = totalHeight - 3;
        if (trunkHeight < leafStartY) trunkHeight = leafStartY;

        for (int i = 0; i < trunkHeight; i++) {
            setter.setBlock(worldX, worldY + i, worldZ, logId, LOG_VERTICAL);
        }

        // 2-3 Sektionen
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
                        // Nur setzen, wenn wir uns über dem worldY befinden (keine Säulen nach unten!)
                        if (y < worldY) continue; 
                        
                        if (x == worldX && z == worldZ && y < trunkHeight) continue;
                        
                        if (Math.abs(x - worldX) + Math.abs(z - worldZ) <= r + (y == sectionBottom ? 1 : 0)) {
                            setter.setBlock(x, y, z, leavesId, (byte) 0);
                        }
                    }
                }
            }
        }
        
        // Top Auffüllen
        for (int y = Math.max(trunkHeight, worldY); y <= worldY + totalHeight; y++) {
            setter.setBlock(worldX, y, worldZ, leavesId, (byte) 0);
        }
    }

    private static void generateWillow(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId) {
        Random random = new Random(worldSeed ^ ((long) worldX * 12345L ^ (long) worldZ * 67890L));
        int height = 5 + random.nextInt(2);
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, logId, LOG_VERTICAL);
        int crownY = worldY + height - 1;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 1; y++) {
                    double d = x*x + z*z + y*y*2;
                    if (d < 10) {
                        setter.setBlock(worldX + x, crownY + y, worldZ + z, leavesId, (byte) 0);
                        if (d > 6 && random.nextFloat() < 0.7f) {
                            for (int h = 1; h <= 2 + random.nextInt(2); h++) setter.setBlock(worldX + x, crownY + y - h, worldZ + z, leavesId, (byte) 0);
                        }
                    }
                }
            }
        }
    }

    private static void generatePalm(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId) {
        Random random = new Random(worldSeed ^ ((long) worldX * 999L ^ (long) worldZ * 888L));
        int height = 7 + random.nextInt(5);
        double curveX = 0, curveZ = 0;
        double speedX = (random.nextDouble() - 0.5) * 0.5;
        double speedZ = (random.nextDouble() - 0.5) * 0.5;
        for (int i = 0; i < height; i++) {
            setter.setBlock(worldX + (int)Math.round(curveX), worldY + i, worldZ + (int)Math.round(curveZ), logId, LOG_VERTICAL);
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
                setter.setBlock(lx, ly, lz, leavesId, (byte) 0);
            }
        }
        setter.setBlock(topX, worldY + height, topZ, leavesId, (byte) 0);
    }

    private static void generateBaobab(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId) {
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
                        setter.setBlock(worldX + x, worldY + i, worldZ + z, logId, LOG_VERTICAL);
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
                    setter.setBlock(worldX + x, topY, worldZ + z, leavesId, (byte) 0);
                    if (d < threshold * 0.6) setter.setBlock(worldX + x, topY + 1, worldZ + z, leavesId, (byte) 0);
                }
            }
        }
    }

    private static void generateMahogany(BlockSetter setter, int worldX, int worldY, int worldZ, long worldSeed, byte logId, byte leavesId) {
        Random random = new Random(worldSeed ^ ((long) worldX * 111L ^ (long) worldZ * 222L));
        int height = 8 + random.nextInt(4);
        for (int i = 0; i < height; i++) setter.setBlock(worldX, worldY + i, worldZ, logId, LOG_VERTICAL);
        int crownY = worldY + height - 2;
        for (int r = 4; r >= 1; r--) {
            int y = crownY + (4 - r);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x*x + z*z <= r*r) setter.setBlock(worldX + x, y, worldZ + z, leavesId, (byte) 0);
                }
            }
        }
    }

    private static void setBlockIfInChunk(Chunk chunk, int worldX, int worldY, int worldZ, byte blockId, byte state) {
        int lx = worldX - chunk.getWorldX() * Chunk.SIZE;
        int lz = worldZ - chunk.getWorldZ() * Chunk.SIZE;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE && worldY >= Chunk.MIN_Y && worldY < Chunk.MAX_Y) {
            if (canReplace(chunk.getBlock(lx, worldY, lz))) chunk.setBlock(lx, worldY, lz, blockId, state);
        }
    }

    private static boolean canReplace(byte blockId) {
        if (blockId == 0) return true;
        Block block = BlockRegistry.get(blockId);
        return block == null || block.isTransparent || !block.isSolid || block.isPassable;
    }
}
