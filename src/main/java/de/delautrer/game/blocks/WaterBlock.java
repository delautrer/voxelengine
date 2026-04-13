package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class WaterBlock extends Block {

    public WaterBlock() {
        super(false, true, true, false);
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

        // TOP
        Block topNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y + 1, z, cm));
        boolean drawTop = shouldRenderFaceAgainst(topNeighbor, h, 1.0f);
        if (h < 0.99f) drawTop = true;

        if (drawTop) {
            float sl0 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            chunk.addFace(x,yTop,z,1, x,yTop,z+1,1, x+1,yTop,z+1,1, x+1,yTop,z,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightTop, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // BOTTOM
        Block bottomNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y - 1, z, cm));
        if (shouldRenderFaceAgainst(bottomNeighbor, h, 1.0f) && y > 0) {
            float sl0 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            chunk.addFace(x,y,z+1,1, x,y,z,1, x+1,y,z,1, x+1,y,z+1,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightBot, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z PLUS (Front)
        Block zPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z + 1, cm));
        if (shouldRenderFaceAgainst(zPlusNeighbor, h, getWaterHeight(x, y, z + 1, chunk, cm))) {
            float sl0 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            chunk.addFace(x,y,z+1,1, x+1,y,z+1,1, x+1,yTop,z+1,1, x,yTop,z+1,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z MINUS (Back)
        Block zMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z - 1, cm));
        if (shouldRenderFaceAgainst(zMinusNeighbor, h, getWaterHeight(x, y, z - 1, chunk, cm))) {
            float sl0 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            chunk.addFace(x+1,y,z,1, x,y,z,1, x,yTop,z,1, x+1,yTop,z,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X MINUS (Left)
        Block xMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x - 1, y, z, cm));
        if (shouldRenderFaceAgainst(xMinusNeighbor, h, getWaterHeight(x - 1, y, z, chunk, cm))) {
            float sl0 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            float bl0 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            chunk.addFace(x,y,z,1, x,y,z+1,1, x,yTop,z+1,1, x,yTop,z,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X PLUS (Right)
        Block xPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x + 1, y, z, cm));
        if (shouldRenderFaceAgainst(xPlusNeighbor, h, getWaterHeight(x + 1, y, z, chunk, cm))) {
            float sl0 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            float bl0 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            chunk.addFace(x+1,y,z+1,1, x+1,y,z,1, x+1,yTop,z,1, x+1,yTop,z+1,1,0.0f, 0.0f, 1.0f, 1.0f, texLayer, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }
    }
}