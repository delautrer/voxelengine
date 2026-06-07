package de.delautrer.game.world.generation.feature.placement;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlacementModifier {
    private final Set<Byte> targetBlocks;
    private final double airExposureChance;
    private final Set<String> validBiomes;

    public PlacementModifier(List<String> targetBlockNames, double airExposureChance, List<String> validBiomeNames) {
        this.targetBlocks = new HashSet<>();
        if (targetBlockNames != null) {
            for (String name : targetBlockNames) {
                Byte id = getBlockId(name);
                if (id != null) {
                    this.targetBlocks.add(id);
                }
            }
        }
        
        this.airExposureChance = airExposureChance;
        
        this.validBiomes = new HashSet<>();
        if (validBiomeNames != null) {
            this.validBiomes.addAll(validBiomeNames);
        }
    }

    private Byte getBlockId(String name) {
        var block = BlockRegistry.get(name);
        if (block != null) {
            return block.getId();
        }
        // Fallback for names without namespace
        block = BlockRegistry.get("voxelengine:" + name);
        if (block != null) {
            return block.getId();
        }
        return null;
    }

    public boolean isValidBiome(String biomeId) {
        if (validBiomes.isEmpty()) return true;
        return validBiomes.contains(biomeId) || validBiomes.contains("minecraft:" + biomeId.toLowerCase()) || validBiomes.contains("voxelengine:" + biomeId.toLowerCase());
    }

    public boolean canReplace(Chunk chunk, int lx, int y, int lz, Random rand) {
        if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y) return false;
        
        byte existingBlock = chunk.getBlock(lx, y, lz);
        
        // Target Block Check
        if (!targetBlocks.isEmpty() && !targetBlocks.contains(existingBlock)) {
            return false;
        }

        // Air Exposure Check
        if (airExposureChance > 0) {
            if (isExposedToAir(chunk, lx, y, lz)) {
                if (rand.nextDouble() < airExposureChance) {
                    return false; // Discard because it is exposed to air and chance failed
                }
            }
        }

        return true;
    }

    private boolean isExposedToAir(Chunk chunk, int lx, int y, int lz) {
        byte airId = 0; // Assuming 0 is air
        
        if (y < Chunk.MAX_Y - 1 && chunk.getBlock(lx, y + 1, lz) == airId) return true;
        if (y > Chunk.MIN_Y && chunk.getBlock(lx, y - 1, lz) == airId) return true;
        
        if (lx > 0 && chunk.getBlock(lx - 1, y, lz) == airId) return true;
        if (lx < Chunk.SIZE - 1 && chunk.getBlock(lx + 1, y, lz) == airId) return true;
        if (lz > 0 && chunk.getBlock(lx, y, lz - 1) == airId) return true;
        if (lz < Chunk.SIZE - 1 && chunk.getBlock(lx, y, lz + 1) == airId) return true;
        
        return false;
    }
}
