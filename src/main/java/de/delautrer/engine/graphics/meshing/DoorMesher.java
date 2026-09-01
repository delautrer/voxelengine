package de.delautrer.engine.graphics.meshing;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.DoorBlock;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockProperties.DoorHinge;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class DoorMesher extends CubeMesher {
    private final DoorBlock doorBlock;

    public DoorMesher(DoorBlock block) {
        super(block);
        this.doorBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        AABB b = doorBlock.getBoundingBoxes(state).get(0);
        
        Direction facing = state.getValue(DoorBlock.FACING);
        boolean open = state.getValue(DoorBlock.OPEN);
        boolean hingeLeft = state.getValue(DoorBlock.HINGE) == DoorHinge.LEFT;
        
        boolean mirN = false, mirS = false, mirE = false, mirW = false;

        if (!open) {
            if (facing == Direction.NORTH) {
                mirS = !hingeLeft; mirN = hingeLeft;
            } else if (facing == Direction.SOUTH) {
                mirS = hingeLeft; mirN = !hingeLeft;
            } else if (facing == Direction.WEST) {
                mirE = !hingeLeft; mirW = hingeLeft;
            } else if (facing == Direction.EAST) {
                mirE = hingeLeft; mirW = !hingeLeft;
            }
        } else {
            if (facing == Direction.NORTH) {
                mirE = hingeLeft; mirW = !hingeLeft;
            } else if (facing == Direction.SOUTH) {
                mirE = !hingeLeft; mirW = hingeLeft;
            } else if (facing == Direction.WEST) {
                mirS = !hingeLeft; mirN = hingeLeft;
            } else if (facing == Direction.EAST) {
                mirS = hingeLeft; mirN = !hingeLeft;
            }
        }

        if (!hingeLeft && open) {
            mirN = !mirN;
            mirS = !mirS;
            mirE = !mirE;
            mirW = !mirW;
        }

        block.renderBox(state, x, y, z, b.min.x, b.min.y, b.min.z, b.max.x, b.max.y, b.max.z, 
                true, true, true, true, true, true, false, chunk, cm, 
                0, 0, 0, 0, 0, 0, 
                false, false, mirN, mirS, mirE, mirW);
    }
}
