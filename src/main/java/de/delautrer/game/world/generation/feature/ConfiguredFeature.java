package de.delautrer.game.world.generation.feature;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class ConfiguredFeature {
    protected final Block block;
    private final Map<Byte, Byte> variantCache = new HashMap<>();

    public ConfiguredFeature(Block block) {
        this.block = block;
    }

    protected Block getVariantBlock(Block replacedBlock) {
        if (block == null) return null;
        if (replacedBlock == null) return block;
        
        NamespacedKey replacedKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(replacedBlock);
        NamespacedKey oreKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(block);
        
        if (replacedKey == null || oreKey == null) {
            return block;
        }
        
        String carrierName = replacedKey.getKey();
        String oreName = oreKey.getKey();
        
        if (carrierName.equals("stone")) {
            return block;
        }
        
        String variantName = carrierName + "_" + oreName;
        Block variant = de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:" + variantName);
        if (variant != null) {
            return variant;
        }
        
        return block;
    }

    public abstract boolean isGlobal();
    public abstract void generate(Chunk chunk, int lx, int y, int lz, int worldX, int worldZ, Random rand, PlacementModifier modifier);

    public void generate(Chunk chunk, de.delautrer.game.world.WorldGenerator wg, int worldX, int worldY, int worldZ, long seed) {}
    public void generate(de.delautrer.game.world.World world, int worldX, int worldY, int worldZ, long seed) {}
}
