package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class LeavesBlock extends CubeBlock {

    public LeavesBlock(int topTex, int bottomTex, int sideTex) {
        super(true, true, topTex, bottomTex, sideTex);
    }

    @Override
    protected float getColorTint() {
        return 0.65f;
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (this.getId() == BlockRegistry.LEAVES.getId()) return true;
        return super.shouldRenderFaceAgainst(neighborBlock, myHeight, neighborHeight);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        super.generateMesh(x, y, z, chunk, cm);
    }
}