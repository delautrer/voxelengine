package de.delautrer.engine.graphics.meshing;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.TrapdoorBlock;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class TrapdoorMesher extends CubeMesher {
    private final TrapdoorBlock trapdoorBlock;

    public TrapdoorMesher(TrapdoorBlock block) {
        super(block);
        this.trapdoorBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        AABB b = trapdoorBlock.getBoundingBoxes(state).get(0);
        
        boolean open = state.getValue(TrapdoorBlock.OPEN);
        Direction facing = state.getValue(TrapdoorBlock.FACING);
        boolean isTop = state.getValue(TrapdoorBlock.HALF) == Half.TOP;
        float t = 0.1875f;

        float edgeV0 = isTop ? t        : (1.0f - t);
        float edgeV1 = isTop ? 0.0f    : 1.0f;

        int rotN = 0, rotS = 0, rotE = 0, rotW = 0, rotUp = 0, rotDown = 0;
        float tu0=-1, tv0=-1, tu1=-1, tv1=-1;
        float bu0=-1, bv0=-1, bu1=-1, bv1=-1;
        float nu0=-1, nv0=-1, nu1=-1, nv1=-1;
        float su0=-1, sv0=-1, su1=-1, sv1=-1;
        float eu0=-1, ev0=-1, eu1=-1, ev1=-1;
        float wu0=-1, wv0=-1, wu1=-1, wv1=-1;
        boolean mirTop = false, mirBot = false;
        boolean mirN = false, mirS = false, mirE = false, mirW = false;

        if (!open) {
            if (facing == Direction.EAST || facing == Direction.WEST) {
                rotUp = 1; rotDown = 1;
            }
            if (!isTop) {
                if (facing == Direction.NORTH) {
                    mirBot = true; mirE = true;
                } else if (facing == Direction.EAST) {
                    mirTop = true; mirW = true;
                } else if (facing == Direction.SOUTH) {
                    mirTop = true; mirE = true;
                } else if (facing == Direction.WEST) {
                    mirBot = true; mirW = true;
                }
            }
        } else {
            if (facing == Direction.SOUTH) {
                tu0 = 0; tv0 = edgeV0; tu1 = 1; tv1 = edgeV1;
                bu0 = 0; bv0 = edgeV0; bu1 = 1; bv1 = edgeV1;
                eu0 = 0; ev0 = edgeV0; eu1 = 1; ev1 = edgeV1; rotE = 1;
                wu0 = 0; wv0 = edgeV0; wu1 = 1; wv1 = edgeV1; rotW = 1;
                mirBot = true; mirW = true;
            } else if (facing == Direction.EAST) {
                tu0 = 0; tv0 = edgeV0; tu1 = 1; tv1 = edgeV1; rotUp = 1;
                bu0 = 0; bv0 = edgeV0; bu1 = 1; bv1 = edgeV1; rotDown = 1;
                nu0 = 0; nv0 = edgeV0; nu1 = 1; nv1 = edgeV1; rotN = 1;
                su0 = 0; sv0 = edgeV0; su1 = 1; sv1 = edgeV1; rotS = 1;
                mirN = true;
            } else if (facing == Direction.NORTH) {
                tu0 = 0; tv0 = edgeV0; tu1 = 1; tv1 = edgeV1;
                bu0 = 0; bv0 = edgeV0; bu1 = 1; bv1 = edgeV1;
                eu0 = 0; ev0 = edgeV0; eu1 = 1; ev1 = edgeV1; rotE = 1;
                wu0 = 0; wv0 = edgeV0; wu1 = 1; wv1 = edgeV1; rotW = 1;
                mirBot = true; mirE = true;
            } else if (facing == Direction.WEST) {
                tu0 = 0; tv0 = edgeV0; tu1 = 1; tv1 = edgeV1; rotUp = 1;
                bu0 = 0; bv0 = edgeV0; bu1 = 1; bv1 = edgeV1; rotDown = 1;
                nu0 = 0; nv0 = edgeV0; nu1 = 1; nv1 = edgeV1; rotN = 1;
                su0 = 0; sv0 = edgeV0; su1 = 1; sv1 = edgeV1; rotS = 1;
                mirBot = true; mirTop = true; mirS = true;
            }
        }
        
        block.renderBox(state, x, y, z, b.min.x, b.min.y, b.min.z, b.max.x, b.max.y, b.max.z, 
                true, true, true, true, true, true, false, chunk, cm, 
                rotUp, rotDown, rotN, rotS, rotE, rotW, 
                mirTop, mirBot, mirN, mirS, mirE, mirW,
                tu0, tv0, tu1, tv1,
                bu0, bv0, bu1, bv1,
                nu0, nv0, nu1, nv1,
                su0, sv0, su1, sv1,
                eu0, ev0, eu1, ev1,
                wu0, wv0, wu1, wv1);
    }
}
