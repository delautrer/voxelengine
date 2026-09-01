package de.delautrer.engine.graphics.meshing;

import de.delautrer.game.blocks.StairBlock;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockProperties.StairShape;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class StairMesher extends CubeMesher {
    private final StairBlock stairBlock;

    public StairMesher(StairBlock block) {
        super(block);
        this.stairBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        Direction facing = state.getValue(StairBlock.FACING);
        Half half = state.getValue(StairBlock.HALF);
        StairShape shape = state.getValue(StairBlock.SHAPE);

        boolean[] q = new boolean[8];

        if (half == Half.BOTTOM) { q[0]=true; q[1]=true; q[2]=true; q[3]=true; }
        else { q[4]=true; q[5]=true; q[6]=true; q[7]=true; }

        int offset = (half == Half.BOTTOM) ? 4 : 0;
        boolean nw=false, ne=false, sw=false, se=false;

        if (facing == Direction.NORTH) { nw=true; ne=true; }
        else if (facing == Direction.SOUTH) { sw=true; se=true; }
        else if (facing == Direction.WEST) { nw=true; sw=true; }
        else if (facing == Direction.EAST) { ne=true; se=true; }

        if (shape == StairShape.INNER_LEFT) {
            if (facing == Direction.NORTH) se=true; if (facing == Direction.SOUTH) nw=true;
            if (facing == Direction.WEST) ne=true; if (facing == Direction.EAST) sw=true;
        } else if (shape == StairShape.INNER_RIGHT) {
            if (facing == Direction.NORTH) sw=true; if (facing == Direction.SOUTH) ne=true;
            if (facing == Direction.WEST) se=true; if (facing == Direction.EAST) nw=true;
        } else if (shape == StairShape.OUTER_LEFT) {
            if (facing == Direction.NORTH) nw=false; if (facing == Direction.SOUTH) se=false;
            if (facing == Direction.WEST) sw=false; if (facing == Direction.EAST) ne=false;
        } else if (shape == StairShape.OUTER_RIGHT) {
            if (facing == Direction.NORTH) ne=false; if (facing == Direction.SOUTH) sw=false;
            if (facing == Direction.WEST) nw=false; if (facing == Direction.EAST) se=false;
        }

        q[offset] = nw; q[offset+1] = ne; q[offset+2] = sw; q[offset+3] = se;

        float[][] bounds = {
                {0, 0, 0, 0.5f, 0.5f, 0.5f},      // 0: B-NW
                {0.5f, 0, 0, 1.0f, 0.5f, 0.5f},   // 1: B-NE
                {0, 0, 0.5f, 0.5f, 0.5f, 1.0f},   // 2: B-SW
                {0.5f, 0, 0.5f, 1.0f, 0.5f, 1.0f},// 3: B-SE
                {0, 0.5f, 0, 0.5f, 1.0f, 0.5f},   // 4: T-NW
                {0.5f, 0.5f, 0, 1.0f, 1.0f, 0.5f},// 5: T-NE
                {0, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f},// 6: T-SW
                {0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f} // 7: T-SE
        };

        for (int i = 0; i < 8; i++) {
            if (!q[i]) continue;
            boolean rTop = (i >= 4) ? true : !q[i + 4];
            boolean rBot = (i < 4) ? true : !q[i - 4];
            boolean rN = true; if (i==2||i==3||i==6||i==7) rN = !q[i-2];
            boolean rS = true; if (i==0||i==1||i==4||i==5) rS = !q[i+2];
            boolean rW = true; if (i==1||i==3||i==5||i==7) rW = !q[i-1];
            boolean rE = true; if (i==0||i==2||i==4||i==6) rE = !q[i+1];

            block.renderBox(state, x, y, z, bounds[i][0], bounds[i][1], bounds[i][2], bounds[i][3], bounds[i][4], bounds[i][5], rTop, rBot, rN, rS, rE, rW, false, chunk, cm);
        }
    }
}
