package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.CubeBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class CubeMesher implements BlockMesher {
    protected final CubeBlock block;

    public CubeMesher(CubeBlock block) {
        this.block = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        block.renderBox(state, x, y, z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, true, true, true, true, true, true, false, chunk, cm);
    }
}
