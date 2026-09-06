package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.JigsawBlock;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class JigsawMesher extends CubeMesher {

    public JigsawMesher(JigsawBlock block) {
        super(block);
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        Direction facing = state.contains(JigsawBlock.FACING) ? state.getValue(JigsawBlock.FACING) : Direction.NORTH;
        int rotTop = 0, rotBot = 0, rotN = 0, rotS = 0, rotE = 0, rotW = 0;

        switch (facing) {
            case NORTH -> { rotTop = 0; rotBot = 0; rotS = 2; }
            case EAST  -> { rotTop = 1; rotBot = 1; rotW = 2; }
            case SOUTH -> { rotTop = 2; rotBot = 2; rotN = 2; }
            case WEST  -> { rotTop = 3; rotBot = 3; rotE = 2; }
            case UP    -> { rotTop = 0; rotBot = 2; }
            case DOWN  -> { rotBot = 0; rotTop = 2; }
        }

        block.renderBox(state, x, y, z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
                true, true, true, true, true, true, false, chunk, cm,
                rotTop, rotBot, rotN, rotS, rotE, rotW);
    }
}
