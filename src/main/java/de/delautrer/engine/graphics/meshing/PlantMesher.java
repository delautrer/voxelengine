package de.delautrer.engine.graphics.meshing;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.PlantBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class PlantMesher implements BlockMesher {
    private final PlantBlock plantBlock;

    public PlantMesher(PlantBlock block) {
        this.plantBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        TextureStitcher.AtlasRegion reg = plantBlock.getModel().top;
        if (reg == null)
            return;

        long seed = ((long) x * 3129871) ^ ((long) z * 116129781L) ^ ((long) y);
        seed = seed * seed * 42317861L + seed * 11L;
        float offX = (((float) (seed >> 16 & 15L) / 15.0f) - 0.5f) * 0.5f;
        float offZ = (((float) (seed >> 24 & 15L) / 15.0f) - 0.5f) * 0.5f;

        float x0 = x + offX;
        float x1 = x + 1 + offX;
        float z0 = z + offZ;
        float z1 = z + 1 + offZ;

        float light = 1.0f;
        float sl = chunk.getSmoothSkyLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);
        float bl = chunk.getSmoothBlockLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);
        float slBottom = sl * 0.80f;
        float blBottom = bl * 0.80f;

        chunk.addFace(x0, y, z0, 1.0f, x1, y, z1, 1.0f, x1, y + 1, z1, 1.0f, x0, y + 1, z0, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, plantBlock, slBottom, slBottom, sl, sl, blBottom, blBottom,
                bl, bl);
        chunk.addFace(x1, y, z1, 1.0f, x0, y, z0, 1.0f, x0, y + 1, z0, 1.0f, x1, y + 1, z1, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, plantBlock, slBottom, slBottom, sl, sl, blBottom, blBottom,
                bl, bl);

        chunk.addFace(x1, y, z0, 1.0f, x0, y, z1, 1.0f, x0, y + 1, z1, 1.0f, x1, y + 1, z0, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, plantBlock, slBottom, slBottom, sl, sl, blBottom, blBottom,
                bl, bl);
        chunk.addFace(x0, y, z1, 1.0f, x1, y, z0, 1.0f, x1, y + 1, z0, 1.0f, x0, y + 1, z1, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, plantBlock, slBottom, slBottom, sl, sl, blBottom, blBottom,
                bl, bl);
    }
}
