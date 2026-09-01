package de.delautrer.engine.graphics.meshing;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class WaterMesher implements BlockMesher {
    private final WaterBlock block;

    public WaterMesher(WaterBlock block) {
        this.block = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float h = block.getWaterHeight(x, y, z, chunk, cm);
        float yTop = y + h;
        float lightTop = 1.0f, lightBot = 0.4f, lightFrontBack = 0.8f, lightLeftRight = 0.65f;
        TextureStitcher.AtlasRegion reg = block.getModel().top;

        Block topNeighbor = chunk.getBlock(x, y + 1, z, cm);
        if (block.shouldRenderFaceAgainst(topNeighbor, h, 1.0f) || h < 0.99f) {
            float sl0 = chunk.getSmoothSkyLight(x, y + 1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y + 1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y + 1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y + 1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y + 1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y + 1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y + 1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y + 1, z, 1, 0, 0, 0, 0, -1, cm);

            chunk.addFace(x, yTop, z, 1, x, yTop, z + 1, 1, x + 1, yTop, z + 1, 1, x + 1, yTop, z, 1, reg.u0, reg.v0,
                    reg.u1, reg.v1, reg.layer, lightTop, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);

            if (topNeighbor != block) {
                float surfaceY = yTop - 0.001f;
                chunk.addFace(x, surfaceY, z + 1, 1, x, surfaceY, z, 1, x + 1, surfaceY, z, 1, x + 1, surfaceY, z + 1,
                        1, reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, 0.5f, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2,
                        bl3);
            }
        }

        Block bottomNeighbor = chunk.getBlock(x, y - 1, z, cm);
        if (block.shouldRenderFaceAgainst(bottomNeighbor, h, 1.0f) && y > Chunk.MIN_Y) {
            float sl0 = chunk.getSmoothSkyLight(x, y - 1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y - 1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y - 1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y - 1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y - 1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y - 1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y - 1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y - 1, z, 1, 0, 0, 0, 0, 1, cm);
            chunk.addFace(x, y, z + 1, 1, x, y, z, 1, x + 1, y, z, 1, x + 1, y, z + 1, 1, reg.u0, reg.v0, reg.u1,
                    reg.v1, reg.layer, lightBot, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z+
        float nHeightZPlus = block.getWaterHeight(x, y, z + 1, chunk, cm);
        Block zPlusNeighbor = chunk.getBlock(x, y, z + 1, cm);
        if (block.shouldRenderFaceAgainst(zPlusNeighbor, h, nHeightZPlus)) {
            float yBot = (zPlusNeighbor == block) ? y + nHeightZPlus : y;
            float vBot = reg.v1;
            if (zPlusNeighbor == block && h > 0)
                vBot = reg.v1 - (nHeightZPlus / h) * (reg.v1 - reg.v0);
            float sl0 = chunk.getSmoothSkyLight(x, y, z + 1, -1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z + 1, 1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z + 1, 1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z + 1, -1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z + 1, -1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z + 1, 1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z + 1, 1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z + 1, -1, 0, 0, 0, 1, 0, cm);
            chunk.addFace(x, yBot, z + 1, 1, x + 1, yBot, z + 1, 1, x + 1, yTop, z + 1, 1, x, yTop, z + 1, 1, reg.u0,
                    reg.v0, reg.u1, vBot, reg.layer, lightFrontBack, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z-
        float nHeightZMinus = block.getWaterHeight(x, y, z - 1, chunk, cm);
        Block zMinusNeighbor = chunk.getBlock(x, y, z - 1, cm);
        if (block.shouldRenderFaceAgainst(zMinusNeighbor, h, nHeightZMinus)) {
            float yBot = (zMinusNeighbor == block) ? y + nHeightZMinus : y;
            float vBot = reg.v1;
            if (zMinusNeighbor == block && h > 0)
                vBot = reg.v1 - (nHeightZMinus / h) * (reg.v1 - reg.v0);
            float sl0 = chunk.getSmoothSkyLight(x, y, z - 1, 1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z - 1, -1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z - 1, -1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z - 1, 1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z - 1, 1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z - 1, -1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z - 1, -1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z - 1, 1, 0, 0, 0, 1, 0, cm);
            chunk.addFace(x + 1, yBot, z, 1, x, yBot, z, 1, x, yTop, z, 1, x + 1, yTop, z, 1, reg.u0, reg.v0, reg.u1,
                    vBot, reg.layer, lightFrontBack, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X-
        float nHeightXMinus = block.getWaterHeight(x - 1, y, z, chunk, cm);
        Block xMinusNeighbor = chunk.getBlock(x - 1, y, z, cm);
        if (block.shouldRenderFaceAgainst(xMinusNeighbor, h, nHeightXMinus)) {
            float yBot = (xMinusNeighbor == block) ? y + nHeightXMinus : y;
            float vBot = reg.v1;
            if (xMinusNeighbor == block && h > 0)
                vBot = reg.v1 - (nHeightXMinus / h) * (reg.v1 - reg.v0);
            float sl0 = chunk.getSmoothSkyLight(x - 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x - 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x - 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x - 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x - 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x - 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x - 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x - 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            chunk.addFace(x, yBot, z, 1, x, yBot, z + 1, 1, x, yTop, z + 1, 1, x, yTop, z, 1, reg.u0, reg.v0, reg.u1,
                    vBot, reg.layer, lightLeftRight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X+
        float nHeightXPlus = block.getWaterHeight(x + 1, y, z, chunk, cm);
        Block xPlusNeighbor = chunk.getBlock(x + 1, y, z, cm);
        if (block.shouldRenderFaceAgainst(xPlusNeighbor, h, nHeightXPlus)) {
            float yBot = (xPlusNeighbor == block) ? y + nHeightXPlus : y;
            float vBot = reg.v1;
            if (xPlusNeighbor == block && h > 0)
                vBot = reg.v1 - (nHeightXPlus / h) * (reg.v1 - reg.v0);
            float sl0 = chunk.getSmoothSkyLight(x + 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x + 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x + 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x + 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x + 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x + 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x + 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x + 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            chunk.addFace(x + 1, yBot, z + 1, 1, x + 1, yBot, z, 1, x + 1, yTop, z, 1, x + 1, yTop, z + 1, 1, reg.u0,
                    reg.v0, reg.u1, vBot, reg.layer, lightLeftRight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }
    }
}
