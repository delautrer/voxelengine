package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;

public enum Biome {
    OCEAN(BlockRegistry.SAND.getDefaultState(), BlockRegistry.SAND.getDefaultState()),
    PLAINS(BlockRegistry.GRASS_BLOCK.getDefaultState(), BlockRegistry.DIRT.getDefaultState()),
    HILLS(BlockRegistry.GRASS_BLOCK.getDefaultState(), BlockRegistry.DIRT.getDefaultState()),
    MOUNTAINS(BlockRegistry.STONE.getDefaultState(), BlockRegistry.STONE.getDefaultState());

    public final BlockState surfaceBlock;
    public final BlockState subSurfaceBlock;

    Biome(BlockState surfaceBlock, BlockState subSurfaceBlock) {
        this.surfaceBlock = surfaceBlock;
        this.subSurfaceBlock = subSurfaceBlock;
    }
}