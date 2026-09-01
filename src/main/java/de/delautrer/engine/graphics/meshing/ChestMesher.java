package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.ChestBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class ChestMesher extends CubeMesher {
    public ChestMesher(ChestBlock block) {
        super(block);
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float offset = 1.0f / 16.0f;
        block.renderBox(state, x, y, z, offset, 0.0f, offset, 1.0f - offset, 14.0f / 16.0f, 1.0f - offset, true, true, true, true, true, true, true, chunk, cm);
    }
}
