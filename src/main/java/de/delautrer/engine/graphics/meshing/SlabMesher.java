package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.SlabBlock;
import de.delautrer.game.blocks.state.BlockProperties.SlabType;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class SlabMesher extends CubeMesher {
    private final SlabBlock slabBlock;

    public SlabMesher(SlabBlock block) {
        super(block);
        this.slabBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        SlabType type = state.getValue(SlabBlock.TYPE);

        if (type == SlabType.DOUBLE) block.renderBox(state, x, y, z, 0, 0, 0, 1, 1, 1, true, true, true, true, true, true, false, chunk, cm);
        else if (type == SlabType.TOP) block.renderBox(state, x, y, z, 0, 0.5f, 0, 1, 1, 1, true, true, true, true, true, true, false, chunk, cm);
        else block.renderBox(state, x, y, z, 0, 0, 0, 1, 0.5f, 1, true, true, true, true, true, true, false, chunk, cm);
    }
}
