package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class CubeBlock extends Block {
    private final int texTop, texSide, texBottom;

    public CubeBlock(boolean isSolid, boolean isTransparent, int texTop, int texSide, int texBottom) {
        super(isSolid, isTransparent);
        this.texTop = texTop;
        this.texSide = texSide;
        this.texBottom = texBottom;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float lightTop = 1.0f, lightBot = 0.4f, lightFrontBack = 0.8f, lightLeftRight = 0.65f;
        float height = 1.0f;

        // TOP
        Block topNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y + 1, z, cm));
        if (shouldRenderFaceAgainst(topNeighbor, height, 1.0f)) {
            float ao0 = chunk.getAO(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float ao1 = chunk.getAO(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float ao2 = chunk.getAO(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float ao3 = chunk.getAO(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            float sl0 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            chunk.addFace(x,y+1,z,ao0, x,y+1,z+1,ao1, x+1,y+1,z+1,ao2, x+1,y+1,z,ao3, texTop, lightTop, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // BOTTOM
        Block bottomNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y - 1, z, cm));
        if (shouldRenderFaceAgainst(bottomNeighbor, height, 1.0f) && y > 0) {
            float ao0 = chunk.getAO(x, y-1, z, -1, 0, 0, 0, 0, 1, cm);
            float ao1 = chunk.getAO(x, y-1, z, -1, 0, 0, 0, 0, -1, cm);
            float ao2 = chunk.getAO(x, y-1, z, 1, 0, 0, 0, 0, -1, cm);
            float ao3 = chunk.getAO(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            float sl0 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            chunk.addFace(x,y,z+1,ao0, x,y,z,ao1, x+1,y,z,ao2, x+1,y,z+1,ao3, texBottom, lightBot, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z PLUS (Front)
        Block zPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z + 1, cm));
        if (shouldRenderFaceAgainst(zPlusNeighbor, height, 1.0f)) {
            float ao0 = chunk.getAO(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float ao1 = chunk.getAO(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float ao2 = chunk.getAO(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float ao3 = chunk.getAO(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            float sl0 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            chunk.addFace(x,y,z+1,ao0, x+1,y,z+1,ao1, x+1,y+1,z+1,ao2, x,y+1,z+1,ao3, texSide, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z MINUS (Back)
        Block zMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z - 1, cm));
        if (shouldRenderFaceAgainst(zMinusNeighbor, height, 1.0f)) {
            float ao0 = chunk.getAO(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float ao1 = chunk.getAO(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float ao2 = chunk.getAO(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float ao3 = chunk.getAO(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            float sl0 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            float bl0 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            chunk.addFace(x+1,y,z,ao0, x,y,z,ao1, x,y+1,z,ao2, x+1,y+1,z,ao3, texSide, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X MINUS (Left)
        Block xMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x - 1, y, z, cm));
        if (shouldRenderFaceAgainst(xMinusNeighbor, height, 1.0f)) {
            float ao0 = chunk.getAO(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float ao1 = chunk.getAO(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float ao2 = chunk.getAO(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float ao3 = chunk.getAO(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            float sl0 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            float bl0 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            chunk.addFace(x,y,z,ao0, x,y,z+1,ao1, x,y+1,z+1,ao2, x,y+1,z,ao3, texSide, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X PLUS (Right)
        Block xPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x + 1, y, z, cm));
        if (shouldRenderFaceAgainst(xPlusNeighbor, height, 1.0f)) {
            float ao0 = chunk.getAO(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float ao1 = chunk.getAO(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float ao2 = chunk.getAO(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float ao3 = chunk.getAO(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            float sl0 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            float bl0 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            chunk.addFace(x+1,y,z+1,ao0, x+1,y,z,ao1, x+1,y+1,z,ao2, x+1,y+1,z+1,ao3, texSide, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }
    }
}