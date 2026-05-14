package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class GravityBlock extends CubeBlock {

    public GravityBlock() {
        super(true, false);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        world.getTickScheduler().scheduleTick(new Vector3i(x, y, z), this, 2);
    }

    @Override
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, de.delautrer.game.entity.player.Player player) {
        world.getTickScheduler().scheduleTick(pos, this, 2);
    }

    @Override
    public void scheduledTick(World world, int x, int y, int z) {
        checkFalling(world, x, y, z);
    }

    private void checkFalling(World world, int x, int y, int z) {
        if (y > Chunk.MIN_Y && world.getBlockAt(x, y - 1, z) == 0) {
            byte blockId = world.getBlockAt(x, y, z);
            if (blockId != getId()) return;

            BlockState stateObj = world.getBlockState(x, y, z);
            byte state = stateObj.getStateId();
            
            world.setBlock(x, y, z, (byte) 0);
            
            de.delautrer.game.entity.FallingBlockEntity falling = new de.delautrer.game.entity.FallingBlockEntity(
                blockId, state, new org.joml.Vector3d(x + 0.5, y, z + 0.5)
            );
            world.spawnEntity(falling);
        }
    }
}
