package de.delautrer.game.world.generation.feature;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;

import java.util.Random;

public abstract class ConfiguredFeature {
    protected final byte blockId;
    private final java.util.Map<Byte, Byte> variantCache = new java.util.HashMap<>();

    public ConfiguredFeature(byte blockId) {
        this.blockId = blockId;
    }

    protected byte getVariantBlockId(byte replacedBlockId) {
        if (variantCache.containsKey(replacedBlockId)) {
            return variantCache.get(replacedBlockId);
        }
        
        de.delautrer.game.blocks.Block replacedBlock = de.delautrer.game.blocks.BlockRegistry.get(replacedBlockId);
        de.delautrer.game.blocks.Block baseOre = de.delautrer.game.blocks.BlockRegistry.get(blockId);
        
        if (replacedBlock == null || baseOre == null) {
            variantCache.put(replacedBlockId, blockId);
            return blockId;
        }
        
        de.delautrer.game.registry.NamespacedKey replacedKey = de.delautrer.game.blocks.BlockRegistry.REGISTRY.getKey(replacedBlock);
        de.delautrer.game.registry.NamespacedKey oreKey = de.delautrer.game.blocks.BlockRegistry.REGISTRY.getKey(baseOre);
        
        if (replacedKey == null || oreKey == null) {
            variantCache.put(replacedBlockId, blockId);
            return blockId;
        }
        
        String carrierName = replacedKey.getKey(); // e.g. "dolomite"
        String oreName = oreKey.getKey(); // e.g. "coal_ore"
        
        if (carrierName.equals("stone")) {
            variantCache.put(replacedBlockId, blockId);
            return blockId;
        }
        
        String variantName = carrierName + "_" + oreName;
        de.delautrer.game.blocks.Block variant = de.delautrer.game.blocks.BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":" + variantName);
        if (variant != null && variant.getId() != 0) {
            variantCache.put(replacedBlockId, variant.getId());
            return variant.getId();
        }
        
        variantCache.put(replacedBlockId, blockId);
        return blockId;
    }

    /**
     * Ob diese Feature pro Block (Global/MegaVein) evaluiert wird, oder punktuell (StandardVein).
     */
    public abstract boolean isGlobal();

    /**
     * Generiert die Struktur im Chunk.
     * @param chunk Der aktuelle Chunk
     * @param lx Start-X im Chunk (0-15)
     * @param y Start-Y
     * @param lz Start-Z im Chunk (0-15)
     * @param worldX Globale X-Koordinate
     * @param worldZ Globale Z-Koordinate
     * @param rand Der Random-Generator für diesen Feature-Spawn
     * @param modifier Der PlacementModifier, um Target-Blocks und Air-Exposure zu prüfen
     */
    public abstract void generate(Chunk chunk, int lx, int y, int lz, int worldX, int worldZ, Random rand, PlacementModifier modifier);
}
