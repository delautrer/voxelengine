package de.delautrer.game.world.generation.feature;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;
import java.util.Random;

public class StandardVeinFeature extends ConfiguredFeature {
    private final int size;

    public StandardVeinFeature(de.delautrer.game.blocks.Block block, int size) {
        super(block);
        this.size = size;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    public void generate(Chunk chunk, int lx, int y, int lz, int worldX, int worldZ, Random rand, PlacementModifier modifier) {
        // Random Walk Algorithm for Veins
        // We calculate an interpolated ellipsoid / blob
        
        float angle = rand.nextFloat() * (float) Math.PI;
        float sizeFactor = (float) size / 8.0f;
        
        double startX = lx + Math.sin(angle) * sizeFactor;
        double endX = lx - Math.sin(angle) * sizeFactor;
        double startZ = lz + Math.cos(angle) * sizeFactor;
        double endZ = lz - Math.cos(angle) * sizeFactor;
        double startY = y + rand.nextInt(3) - 1;
        double endY = y + rand.nextInt(3) - 1;

        for (int i = 0; i < size; i++) {
            float t = (float) i / (float) size;
            double centerX = startX + (endX - startX) * t;
            double centerY = startY + (endY - startY) * t;
            double centerZ = startZ + (endZ - startZ) * t;

            double radius = rand.nextDouble() * sizeFactor + 0.5;

            int minX = (int) Math.floor(centerX - radius);
            int maxX = (int) Math.ceil(centerX + radius);
            int minY = (int) Math.floor(centerY - radius);
            int maxY = (int) Math.ceil(centerY + radius);
            int minZ = (int) Math.floor(centerZ - radius);
            int maxZ = (int) Math.ceil(centerZ + radius);

            for (int currX = minX; currX <= maxX; currX++) {
                for (int currY = minY; currY <= maxY; currY++) {
                    for (int currZ = minZ; currZ <= maxZ; currZ++) {
                        double dx = (currX + 0.5 - centerX) / radius;
                        double dy = (currY + 0.5 - centerY) / radius;
                        double dz = (currZ + 0.5 - centerZ) / radius;

                        if (dx * dx + dy * dy + dz * dz < 1.0) {
                            if (currX >= 0 && currX < Chunk.SIZE && currZ >= 0 && currZ < Chunk.SIZE) {
                                if (modifier.canReplace(chunk, currX, currY, currZ, rand)) {
                                    de.delautrer.game.blocks.Block replacedBlock = chunk.getBlock(currX, currY, currZ);
                                    chunk.setBlock(currX, currY, currZ, getVariantBlock(replacedBlock), (byte) 0, null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
