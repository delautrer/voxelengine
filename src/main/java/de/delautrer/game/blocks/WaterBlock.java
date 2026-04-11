package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class WaterBlock extends Block {

    public WaterBlock() {
        super(false, true);
    }

    private float getWaterHeight(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        if (y < 0 || y >= Chunk.HEIGHT) return 1.0f;
        if (chunk.getBlockAt(x, y, z, cm) != this.getId()) return 1.0f;
        if (chunk.getBlockAt(x, y + 1, z, cm) == this.getId()) return 1.0f;

        byte state = chunk.getStateAt(x, y, z, cm);
        return Math.max(0.1f, state / 9.0f);
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock.getId() == this.getId()) {
            return myHeight > neighborHeight + 0.01f;
        }
        return neighborBlock.isTransparent;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float h = getWaterHeight(x, y, z, chunk, cm);
        float yTop = y + h;
        float lightTop = 1.0f, lightBot = 0.4f, lightFrontBack = 0.8f, lightLeftRight = 0.65f;
        int texLayer = 4;

        Block topNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y + 1, z, cm));
        boolean drawTop = shouldRenderFaceAgainst(topNeighbor, h, 1.0f);
        if (h < 0.99f) drawTop = true;

        if (drawTop) {
            chunk.addFace(x,yTop,z,1, x,yTop,z+1,1, x+1,yTop,z+1,1, x+1,yTop,z,1, texLayer, lightTop, this);
        }

        Block bottomNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y - 1, z, cm));
        if (shouldRenderFaceAgainst(bottomNeighbor, h, 1.0f) && y > 0) {
            chunk.addFace(x,y,z+1,1, x,y,z,1, x+1,y,z,1, x+1,y,z+1,1, texLayer, lightBot, this);
        }

        Block zPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z + 1, cm));
        if (shouldRenderFaceAgainst(zPlusNeighbor, h, getWaterHeight(x, y, z + 1, chunk, cm))) {
            chunk.addFace(x,y,z+1,1, x+1,y,z+1,1, x+1,yTop,z+1,1, x,yTop,z+1,1, texLayer, lightFrontBack, this);
        }

        Block zMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z - 1, cm));
        if (shouldRenderFaceAgainst(zMinusNeighbor, h, getWaterHeight(x, y, z - 1, chunk, cm))) {
            chunk.addFace(x+1,y,z,1, x,y,z,1, x,yTop,z,1, x+1,yTop,z,1, texLayer, lightFrontBack, this);
        }

        Block xMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x - 1, y, z, cm));
        if (shouldRenderFaceAgainst(xMinusNeighbor, h, getWaterHeight(x - 1, y, z, chunk, cm))) {
            chunk.addFace(x,y,z,1, x,y,z+1,1, x,yTop,z+1,1, x,yTop,z,1, texLayer, lightLeftRight, this);
        }

        Block xPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x + 1, y, z, cm));
        if (shouldRenderFaceAgainst(xPlusNeighbor, h, getWaterHeight(x + 1, y, z, chunk, cm))) {
            chunk.addFace(x+1,y,z+1,1, x+1,y,z,1, x+1,yTop,z,1, x+1,yTop,z+1,1, texLayer, lightLeftRight, this);
        }
    }
}