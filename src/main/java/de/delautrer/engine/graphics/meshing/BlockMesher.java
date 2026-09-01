package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public interface BlockMesher {
    void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm);
}
