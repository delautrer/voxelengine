package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class CubeBlock extends Block {
    protected final int texTop, texSide, texBottom;

    public CubeBlock(boolean isSolid, boolean isTransparent, int texTop, int texSide, int texBottom) {
        super(isSolid, isTransparent);
        this.texTop = texTop;
        this.texSide = texSide;
        this.texBottom = texBottom;
    }

    protected float getColorTint() { return 1.0f; }

    public int getTextureForFace(BlockState state, BlockFace face) {
        if (face == BlockFace.UP) return texTop;
        if (face == BlockFace.DOWN) return texBottom;
        return texSide;
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock.getId() == 0) return true;
        if (neighborBlock.isTransparent) return true;
        return false;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        BlockState state = chunk.getBlockState(x, y, z);
        renderBox(state, x, y, z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, true, true, true, true, true, true, chunk, cm);
    }

    // --- MAGIE 1: Bilineare Interpolation ---
    // Berechnet fließende Übergänge für Licht und AO auf Teil-Blöcken (z.B. Slabs)
    private float bilerp(float c00, float c10, float c11, float c01, float u, float v) {
        return c00 * (1 - u) * (1 - v) + c10 * u * (1 - v) + c01 * (1 - u) * v + c11 * u * v;
    }

    // --- MAGIE 2: Der Treppen-Knick ---
    // Fügt exakt an inneren Kanten (wie der Mitte einer Treppe) einen leichten Schatten hinzu
    private float getCrease(float vx, float vy, float vz) {
        boolean inX = vx > 0.01f && vx < 0.99f;
        boolean inY = vy > 0.01f && vy < 0.99f;
        boolean inZ = vz > 0.01f && vz < 0.99f;
        if ((inX && inY) || (inX && inZ) || (inY && inZ)) return 0.85f; // Leichter Schatten!
        return 1.0f;
    }

    protected void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW, Chunk chunk, ChunkManager cm) {
        float tint = getColorTint();
        float lightTop = 1.0f * tint, lightBot = 0.4f * tint, lightFrontBack = 0.8f * tint, lightLeftRight = 0.65f * tint;

        int tTop = getTextureForFace(state, BlockFace.UP); int tBot = getTextureForFace(state, BlockFace.DOWN);
        int tNorth = getTextureForFace(state, BlockFace.NORTH); int tSouth = getTextureForFace(state, BlockFace.SOUTH);
        int tEast = getTextureForFace(state, BlockFace.EAST); int tWest = getTextureForFace(state, BlockFace.WEST);

        // TOP (UP)
        if (rTop && (maxY < 1.0f || shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x, y + 1, z, cm)), 1.0f, 1.0f))) {
            float ao0 = chunk.getAO(x, y+1, z, -1, 0, 0, 0, 0, -1, cm); float ao1 = chunk.getAO(x, y+1, z, -1, 0, 0, 0, 0, 1, cm); float ao2 = chunk.getAO(x, y+1, z, 1, 0, 0, 0, 0, 1, cm); float ao3 = chunk.getAO(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm); float sl1 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm); float sl2 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm); float sl3 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm); float bl1 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm); float bl2 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm); float bl3 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);

            float n_ao0 = bilerp(ao0, ao3, ao2, ao1, minX, minZ) * getCrease(minX, maxY, minZ);
            float n_ao1 = bilerp(ao0, ao3, ao2, ao1, minX, maxZ) * getCrease(minX, maxY, maxZ);
            float n_ao2 = bilerp(ao0, ao3, ao2, ao1, maxX, maxZ) * getCrease(maxX, maxY, maxZ);
            float n_ao3 = bilerp(ao0, ao3, ao2, ao1, maxX, minZ) * getCrease(maxX, maxY, minZ);

            float n_sl0 = bilerp(sl0, sl3, sl2, sl1, minX, minZ); float n_sl1 = bilerp(sl0, sl3, sl2, sl1, minX, maxZ); float n_sl2 = bilerp(sl0, sl3, sl2, sl1, maxX, maxZ); float n_sl3 = bilerp(sl0, sl3, sl2, sl1, maxX, minZ);
            float n_bl0 = bilerp(bl0, bl3, bl2, bl1, minX, minZ); float n_bl1 = bilerp(bl0, bl3, bl2, bl1, minX, maxZ); float n_bl2 = bilerp(bl0, bl3, bl2, bl1, maxX, maxZ); float n_bl3 = bilerp(bl0, bl3, bl2, bl1, maxX, minZ);

            chunk.addFace(x+minX, y+maxY, z+minZ, n_ao0, x+minX, y+maxY, z+maxZ, n_ao1, x+maxX, y+maxY, z+maxZ, n_ao2, x+maxX, y+maxY, z+minZ, n_ao3, minX, minZ, maxX, maxZ, tTop, lightTop, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
        // BOTTOM (DOWN)
        if (rBot && (minY > 0.0f || (y > 0 && shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x, y - 1, z, cm)), 1.0f, 1.0f)))) {
            float ao0 = chunk.getAO(x, y-1, z, -1, 0, 0, 0, 0, 1, cm); float ao1 = chunk.getAO(x, y-1, z, -1, 0, 0, 0, 0, -1, cm); float ao2 = chunk.getAO(x, y-1, z, 1, 0, 0, 0, 0, -1, cm); float ao3 = chunk.getAO(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm); float sl1 = chunk.getSmoothSkyLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm); float sl2 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm); float sl3 = chunk.getSmoothSkyLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, 1, cm); float bl1 = chunk.getSmoothBlockLight(x, y-1, z, -1, 0, 0, 0, 0, -1, cm); float bl2 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, -1, cm); float bl3 = chunk.getSmoothBlockLight(x, y-1, z, 1, 0, 0, 0, 0, 1, cm);

            float n_ao0 = bilerp(ao1, ao2, ao3, ao0, minX, maxZ) * getCrease(minX, minY, maxZ);
            float n_ao1 = bilerp(ao1, ao2, ao3, ao0, minX, minZ) * getCrease(minX, minY, minZ);
            float n_ao2 = bilerp(ao1, ao2, ao3, ao0, maxX, minZ) * getCrease(maxX, minY, minZ);
            float n_ao3 = bilerp(ao1, ao2, ao3, ao0, maxX, maxZ) * getCrease(maxX, minY, maxZ);

            float n_sl0 = bilerp(sl1, sl2, sl3, sl0, minX, maxZ); float n_sl1 = bilerp(sl1, sl2, sl3, sl0, minX, minZ); float n_sl2 = bilerp(sl1, sl2, sl3, sl0, maxX, minZ); float n_sl3 = bilerp(sl1, sl2, sl3, sl0, maxX, maxZ);
            float n_bl0 = bilerp(bl1, bl2, bl3, bl0, minX, maxZ); float n_bl1 = bilerp(bl1, bl2, bl3, bl0, minX, minZ); float n_bl2 = bilerp(bl1, bl2, bl3, bl0, maxX, minZ); float n_bl3 = bilerp(bl1, bl2, bl3, bl0, maxX, maxZ);

            chunk.addFace(x+minX, y+minY, z+maxZ, n_ao0, x+minX, y+minY, z+minZ, n_ao1, x+maxX, y+minY, z+minZ, n_ao2, x+maxX, y+minY, z+maxZ, n_ao3, minX, minZ, maxX, maxZ, tBot, lightBot, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
        // SOUTH (Z+)
        if (rS && (maxZ < 1.0f || shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x, y, z + 1, cm)), 1.0f, 1.0f))) {
            float ao0 = chunk.getAO(x, y, z+1, -1, 0, 0, 0, -1, 0, cm); float ao1 = chunk.getAO(x, y, z+1, 1, 0, 0, 0, -1, 0, cm); float ao2 = chunk.getAO(x, y, z+1, 1, 0, 0, 0, 1, 0, cm); float ao3 = chunk.getAO(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm); float sl1 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm); float sl2 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm); float sl3 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm); float bl1 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm); float bl2 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm); float bl3 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);

            float n_ao0 = bilerp(ao0, ao1, ao2, ao3, minX, minY) * getCrease(minX, minY, maxZ);
            float n_ao1 = bilerp(ao0, ao1, ao2, ao3, maxX, minY) * getCrease(maxX, minY, maxZ);
            float n_ao2 = bilerp(ao0, ao1, ao2, ao3, maxX, maxY) * getCrease(maxX, maxY, maxZ);
            float n_ao3 = bilerp(ao0, ao1, ao2, ao3, minX, maxY) * getCrease(minX, maxY, maxZ);

            float n_sl0 = bilerp(sl0, sl1, sl2, sl3, minX, minY); float n_sl1 = bilerp(sl0, sl1, sl2, sl3, maxX, minY); float n_sl2 = bilerp(sl0, sl1, sl2, sl3, maxX, maxY); float n_sl3 = bilerp(sl0, sl1, sl2, sl3, minX, maxY);
            float n_bl0 = bilerp(bl0, bl1, bl2, bl3, minX, minY); float n_bl1 = bilerp(bl0, bl1, bl2, bl3, maxX, minY); float n_bl2 = bilerp(bl0, bl1, bl2, bl3, maxX, maxY); float n_bl3 = bilerp(bl0, bl1, bl2, bl3, minX, maxY);

            chunk.addFace(x+minX, y+minY, z+maxZ, n_ao0, x+maxX, y+minY, z+maxZ, n_ao1, x+maxX, y+maxY, z+maxZ, n_ao2, x+minX, y+maxY, z+maxZ, n_ao3, minX, 1.0f-maxY, maxX, 1.0f-minY, tSouth, lightFrontBack, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
        // NORTH (Z-)
        if (rN && (minZ > 0.0f || shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x, y, z - 1, cm)), 1.0f, 1.0f))) {
            float ao0 = chunk.getAO(x, y, z-1, 1, 0, 0, 0, -1, 0, cm); float ao1 = chunk.getAO(x, y, z-1, -1, 0, 0, 0, -1, 0, cm); float ao2 = chunk.getAO(x, y, z-1, -1, 0, 0, 0, 1, 0, cm); float ao3 = chunk.getAO(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm); float sl1 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm); float sl2 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm); float sl3 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm); float bl1 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm); float bl2 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm); float bl3 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);

            float n_ao0 = bilerp(ao1, ao0, ao3, ao2, maxX, minY) * getCrease(maxX, minY, minZ);
            float n_ao1 = bilerp(ao1, ao0, ao3, ao2, minX, minY) * getCrease(minX, minY, minZ);
            float n_ao2 = bilerp(ao1, ao0, ao3, ao2, minX, maxY) * getCrease(minX, maxY, minZ);
            float n_ao3 = bilerp(ao1, ao0, ao3, ao2, maxX, maxY) * getCrease(maxX, maxY, minZ);

            float n_sl0 = bilerp(sl1, sl0, sl3, sl2, maxX, minY); float n_sl1 = bilerp(sl1, sl0, sl3, sl2, minX, minY); float n_sl2 = bilerp(sl1, sl0, sl3, sl2, minX, maxY); float n_sl3 = bilerp(sl1, sl0, sl3, sl2, maxX, maxY);
            float n_bl0 = bilerp(bl1, bl0, bl3, bl2, maxX, minY); float n_bl1 = bilerp(bl1, bl0, bl3, bl2, minX, minY); float n_bl2 = bilerp(bl1, bl0, bl3, bl2, minX, maxY); float n_bl3 = bilerp(bl1, bl0, bl3, bl2, maxX, maxY);

            chunk.addFace(x+maxX, y+minY, z+minZ, n_ao0, x+minX, y+minY, z+minZ, n_ao1, x+minX, y+maxY, z+minZ, n_ao2, x+maxX, y+maxY, z+minZ, n_ao3, 1.0f-maxX, 1.0f-maxY, 1.0f-minX, 1.0f-minY, tNorth, lightFrontBack, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
        // WEST (X-)
        if (rW && (minX > 0.0f || shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x - 1, y, z, cm)), 1.0f, 1.0f))) {
            float ao0 = chunk.getAO(x-1, y, z, 0, -1, 0, 0, 0, -1, cm); float ao1 = chunk.getAO(x-1, y, z, 0, -1, 0, 0, 0, 1, cm); float ao2 = chunk.getAO(x-1, y, z, 0, 1, 0, 0, 0, 1, cm); float ao3 = chunk.getAO(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl0 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm); float sl1 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm); float sl2 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm); float sl3 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm); float bl1 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm); float bl2 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm); float bl3 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);

            float n_ao0 = bilerp(ao0, ao1, ao2, ao3, minZ, minY) * getCrease(minX, minY, minZ);
            float n_ao1 = bilerp(ao0, ao1, ao2, ao3, maxZ, minY) * getCrease(minX, minY, maxZ);
            float n_ao2 = bilerp(ao0, ao1, ao2, ao3, maxZ, maxY) * getCrease(minX, maxY, maxZ);
            float n_ao3 = bilerp(ao0, ao1, ao2, ao3, minZ, maxY) * getCrease(minX, maxY, minZ);

            float n_sl0 = bilerp(sl0, sl1, sl2, sl3, minZ, minY); float n_sl1 = bilerp(sl0, sl1, sl2, sl3, maxZ, minY); float n_sl2 = bilerp(sl0, sl1, sl2, sl3, maxZ, maxY); float n_sl3 = bilerp(sl0, sl1, sl2, sl3, minZ, maxY);
            float n_bl0 = bilerp(bl0, bl1, bl2, bl3, minZ, minY); float n_bl1 = bilerp(bl0, bl1, bl2, bl3, maxZ, minY); float n_bl2 = bilerp(bl0, bl1, bl2, bl3, maxZ, maxY); float n_bl3 = bilerp(bl0, bl1, bl2, bl3, minZ, maxY);

            chunk.addFace(x+minX, y+minY, z+minZ, n_ao0, x+minX, y+minY, z+maxZ, n_ao1, x+minX, y+maxY, z+maxZ, n_ao2, x+minX, y+maxY, z+minZ, n_ao3, minZ, 1.0f-maxY, maxZ, 1.0f-minY, tWest, lightLeftRight, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
        // EAST (X+)
        if (rE && (maxX < 1.0f || shouldRenderFaceAgainst(BlockRegistry.get(chunk.getBlockAt(x + 1, y, z, cm)), 1.0f, 1.0f))) {
            float ao0 = chunk.getAO(x+1, y, z, 0, -1, 0, 0, 0, 1, cm); float ao1 = chunk.getAO(x+1, y, z, 0, -1, 0, 0, 0, -1, cm); float ao2 = chunk.getAO(x+1, y, z, 0, 1, 0, 0, 0, -1, cm); float ao3 = chunk.getAO(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl0 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm); float sl1 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm); float sl2 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm); float sl3 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm); float bl1 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm); float bl2 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm); float bl3 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);

            float n_ao0 = bilerp(ao1, ao0, ao3, ao2, maxZ, minY) * getCrease(maxX, minY, maxZ);
            float n_ao1 = bilerp(ao1, ao0, ao3, ao2, minZ, minY) * getCrease(maxX, minY, minZ);
            float n_ao2 = bilerp(ao1, ao0, ao3, ao2, minZ, maxY) * getCrease(maxX, maxY, minZ);
            float n_ao3 = bilerp(ao1, ao0, ao3, ao2, maxZ, maxY) * getCrease(maxX, maxY, maxZ);

            float n_sl0 = bilerp(sl1, sl0, sl3, sl2, maxZ, minY); float n_sl1 = bilerp(sl1, sl0, sl3, sl2, minZ, minY); float n_sl2 = bilerp(sl1, sl0, sl3, sl2, minZ, maxY); float n_sl3 = bilerp(sl1, sl0, sl3, sl2, maxZ, maxY);
            float n_bl0 = bilerp(bl1, bl0, bl3, bl2, maxZ, minY); float n_bl1 = bilerp(bl1, bl0, bl3, bl2, minZ, minY); float n_bl2 = bilerp(bl1, bl0, bl3, bl2, minZ, maxY); float n_bl3 = bilerp(bl1, bl0, bl3, bl2, maxZ, maxY);

            chunk.addFace(x+maxX, y+minY, z+maxZ, n_ao0, x+maxX, y+minY, z+minZ, n_ao1, x+maxX, y+maxY, z+minZ, n_ao2, x+maxX, y+maxY, z+maxZ, n_ao3, 1.0f-maxZ, 1.0f-maxY, 1.0f-minZ, 1.0f-minY, tEast, lightLeftRight, this, n_sl0, n_sl1, n_sl2, n_sl3, n_bl0, n_bl1, n_bl2, n_bl3);
        }
    }
}