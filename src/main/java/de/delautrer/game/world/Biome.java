package de.delautrer.game.world;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;

public enum Biome {
    OCEAN("gravel", "gravel", 35.0f, 10.0f),
    PLAINS("grass_block", "dirt", 64.0f, 10.0f),
    FOREST("grass_block", "dirt", 66.0f, 15.0f),
    DESERT("sand", "sand", 65.0f, 12.0f),
    HILLS("grass_block", "dirt", 80.0f, 40.0f),
    MOUNTAINS("stone", "stone", 100.0f, 120.0f);

    private final String surfaceBlockId;
    private final String subSurfaceBlockId;

    public final float baseHeight;
    public final float heightVariation;

    Biome(String surfaceBlockId, String subSurfaceBlockId, float baseHeight, float heightVariation) {
        this.surfaceBlockId = surfaceBlockId;
        this.subSurfaceBlockId = subSurfaceBlockId;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
    }

    public BlockState getSurfaceBlock() {
        return BlockRegistry.get(Constants.NAMESPACE + ":" + surfaceBlockId).getDefaultState();
    }

    public BlockState getSubSurfaceBlock() {
        return BlockRegistry.get(Constants.NAMESPACE + ":" + subSurfaceBlockId).getDefaultState();
    }
}