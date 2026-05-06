package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;
public class LeavesBlock extends CubeBlock {

    public LeavesBlock() {
        super(true, true);
    }

    @Override
    protected float getColorTint() {
        return 0.65f;
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (this.getId() == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "leaves").getId()) return true;
        return super.shouldRenderFaceAgainst(neighborBlock, myHeight, neighborHeight);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        super.generateMesh(x, y, z, chunk, cm);
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockProperties.BlockFace face) {
        if (this.getId() == neighborState.getBlock().getId()) return true;
        return super.shouldRenderFaceAgainstState(myState, neighborState, face);
    }
}
