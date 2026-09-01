package de.delautrer.game.world.generation.feature.placement;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlacementModifier {
    private final Set<de.delautrer.game.blocks.Block> targetBlocks;
    private final double airExposureChance;
    private final Set<String> validBiomes;

    public PlacementModifier(List<String> targetBlockNames, double airExposureChance, List<String> validBiomeNames) {
        this.targetBlocks = new HashSet<>();
        if (targetBlockNames != null) {
            for (String name : targetBlockNames) {
                String key = name.contains(":") ? name : ("veinstride:" + name);
                de.delautrer.game.blocks.Block b = de.delautrer.game.registry.Registries.BLOCKS.get(key);
                if (b != null) {
                    this.targetBlocks.add(b);
                }
            }
        }
        
        this.airExposureChance = airExposureChance;
        
        this.validBiomes = new HashSet<>();
        if (validBiomeNames != null) {
            this.validBiomes.addAll(validBiomeNames);
        }
    }

    public boolean isValidBiome(String biomeId) {
        if (validBiomes.isEmpty()) return true;
        return validBiomes.contains(biomeId) || validBiomes.contains("veinstride:" + biomeId.toLowerCase());
    }

    public boolean canReplace(Chunk chunk, int lx, int y, int lz, Random rand) {
        if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y) return false;
        
        de.delautrer.game.blocks.Block existingBlock = chunk.getBlock(lx, y, lz);
        
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
        if (y < Chunk.MAX_Y - 1 && chunk.getBlock(lx, y + 1, lz).isAir()) return true;
        if (y > Chunk.MIN_Y && chunk.getBlock(lx, y - 1, lz).isAir()) return true;
        
        if (lx > 0 && chunk.getBlock(lx - 1, y, lz).isAir()) return true;
        if (lx < Chunk.SIZE - 1 && chunk.getBlock(lx + 1, y, lz).isAir()) return true;
        if (lz > 0 && chunk.getBlock(lx, y, lz - 1).isAir()) return true;
        if (lz < Chunk.SIZE - 1 && chunk.getBlock(lx, y, lz + 1).isAir()) return true;
        
        return false;
    }
}
