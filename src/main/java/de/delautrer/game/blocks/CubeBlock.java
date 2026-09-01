package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.game.blocks.models.BlockModelData;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockProperties.SlabType;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

public class CubeBlock extends Block {

    public CubeBlock(boolean isSolid, boolean isTransparent, boolean isPassable, boolean isRaycastable) {
        super(isSolid, isTransparent, isPassable, isRaycastable);
        this.mesher = new de.delautrer.engine.graphics.meshing.CubeMesher(this);
    }

    public CubeBlock(boolean isSolid, boolean isTransparent, boolean isPassable) {
        super(isSolid, isTransparent, isPassable, true);
        this.mesher = new de.delautrer.engine.graphics.meshing.CubeMesher(this);
    }

    public CubeBlock(boolean isSolid, boolean isTransparent) {
        super(isSolid, isTransparent, false, true);
        this.mesher = new de.delautrer.engine.graphics.meshing.CubeMesher(this);
    }

    protected float getColorTint() {
        return 1.0f;
    }

    public AtlasRegion getTextureForFace(BlockState state, BlockFace face) {
        BlockModelData model = getModel();
        if (model == null)
            return null;
        if (face == BlockFace.UP)
            return model.top;
        if (face == BlockFace.DOWN)
            return model.bottom;
        if (face == BlockFace.NORTH)
            return model.north;
        if (face == BlockFace.SOUTH)
            return model.south;
        if (face == BlockFace.EAST)
            return model.east;
        return model.west;
    }

    protected BlockState getNeighborState(Chunk chunk, ChunkManager cm, int nx, int ny, int nz) {
        Block nBlock = chunk.getBlock(nx, ny, nz, cm);
        byte sId = chunk.getStateAt(nx, ny, nz, cm);
        return nBlock.getStateForId(sId);
    }

    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        Block myBlock = myState.getBlock();
        Block nBlock = neighborState.getBlock();
        if (nBlock == null || nBlock.isAir())
            return true;

        if (myBlock.isTransparent && myBlock == nBlock) {
            return false;
        }

        // Chests are smaller than full blocks (14x14x14) and do not cover full faces
        if (nBlock instanceof ChestBlock) {
            return true;
        }

        // Slabs only cover certain faces fully
        if (nBlock instanceof SlabBlock) {
            SlabType type = neighborState.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM) {
                if (face == BlockFace.UP) return false;
            } else if (type == SlabType.TOP) {
                if (face == BlockFace.DOWN) return false;
            } else if (type == SlabType.DOUBLE) {
                return nBlock.isTransparent;
            }
            return true;
        }

        // Stairs only cover certain faces fully
        if (nBlock instanceof StairBlock) {
            Half half = neighborState.getValue(StairBlock.HALF);
            if (half == Half.BOTTOM) {
                if (face == BlockFace.UP) return false;
            } else if (half == Half.TOP) {
                if (face == BlockFace.DOWN) return false;
            }
            return true;
        }

        return nBlock.isTransparent;
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock == null || neighborBlock.isAir())
            return true;
        if (neighborBlock instanceof SlabBlock)
            return true;
        if (neighborBlock instanceof StairBlock)
            return true;
        if (neighborBlock instanceof ChestBlock)
            return true;

        if (this.isTransparent && this == neighborBlock)
            return false;
        return neighborBlock.isTransparent;
    }

    private float bilerp(float c00, float c10, float c11, float c01, float u, float v) {
        return c00 * (1 - u) * (1 - v) + c10 * u * (1 - v) + c01 * (1 - u) * v + c11 * u * v;
    }

    private float getCrease(float vx, float vy, float vz) {
        boolean inX = vx > 0.01f && vx < 0.99f;
        boolean inY = vy > 0.01f && vy < 0.99f;
        boolean inZ = vz > 0.01f && vz < 0.99f;
        if ((inX && inY) || (inX && inZ) || (inY && inZ))
            return 0.85f;
        return 1.0f;
    }

    protected void addMappedFace(Chunk chunk, float x0, float y0, float z0, float ao0, float x1, float y1, float z1,
            float ao1, float x2, float y2, float z2, float ao2, float x3, float y3, float z3, float ao3, float lu0,
            float lv0, float lu1, float lv1, AtlasRegion reg, float light, float sl0, float sl1, float sl2, float sl3,
            float bl0, float bl1, float bl2, float bl3, int rotation, boolean mirrorHorizontal) {
        if (reg == null)
            return;

        float u0 = reg.u0 + (reg.u1 - reg.u0) * (mirrorHorizontal ? lu1 : lu0);
        float v0 = reg.v0 + (reg.v1 - reg.v0) * lv0;
        float u1 = reg.u0 + (reg.u1 - reg.u0) * (mirrorHorizontal ? lu0 : lu1);
        float v1 = reg.v0 + (reg.v1 - reg.v0) * lv1;

        // Base mapping (rotation 0):
        // Vertex 0: u0, v1
        // Vertex 1: u1, v1
        // Vertex 2: u1, v0
        // Vertex 3: u0, v0
        float[][] uvs = {
                { u0, v1 }, { u1, v1 }, { u1, v0 }, { u0, v0 }
        };

        int r = rotation % 4;
        float uv0_u = uvs[r][0], uv0_v = uvs[r][1];
        float uv1_u = uvs[(r + 1) % 4][0], uv1_v = uvs[(r + 1) % 4][1];
        float uv2_u = uvs[(r + 2) % 4][0], uv2_v = uvs[(r + 2) % 4][1];
        float uv3_u = uvs[(r + 3) % 4][0], uv3_v = uvs[(r + 3) % 4][1];

        chunk.addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3,
                uv0_u, uv0_v, uv1_u, uv1_v, uv2_u, uv2_v, uv3_u, uv3_v,
                reg.layer, light, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
    }

    public void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW,
            boolean fullUVs, Chunk chunk, ChunkManager cm) {
        renderBox(state, x, y, z, minX, minY, minZ, maxX, maxY, maxZ, rTop, rBot, rN, rS, rE, rW, fullUVs, chunk, cm, 0,
                0, 0, 0, 0, 0, false, false, false, false, false, false);
    }

    public void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW,
            boolean fullUVs, Chunk chunk, ChunkManager cm, boolean mirrorHorizontal) {
        renderBox(state, x, y, z, minX, minY, minZ, maxX, maxY, maxZ, rTop, rBot, rN, rS, rE, rW, fullUVs, chunk, cm, 0,
                0, 0, 0, 0, 0, mirrorHorizontal, mirrorHorizontal, mirrorHorizontal, mirrorHorizontal, mirrorHorizontal, mirrorHorizontal);
    }

    public void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW,
            boolean fullUVs, Chunk chunk, ChunkManager cm, int rotTop, int rotBot, int rotN, int rotS, int rotE,
            int rotW) {
        renderBox(state, x, y, z, minX, minY, minZ, maxX, maxY, maxZ, rTop, rBot, rN, rS, rE, rW, fullUVs, chunk, cm, rotTop,
                rotBot, rotN, rotS, rotE, rotW, false, false, false, false, false, false);
    }

    public void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW,
            boolean fullUVs, Chunk chunk, ChunkManager cm, int rotTop, int rotBot, int rotN, int rotS, int rotE,
            int rotW, boolean mirTop, boolean mirBot, boolean mirN, boolean mirS, boolean mirE, boolean mirW) {
        renderBox(state, x, y, z, minX, minY, minZ, maxX, maxY, maxZ, rTop, rBot, rN, rS, rE, rW, fullUVs, chunk, cm,
                rotTop, rotBot, rotN, rotS, rotE, rotW,
                mirTop, mirBot, mirN, mirS, mirE, mirW,
                -1, -1, -1, -1, // Top UVs
                -1, -1, -1, -1, // Bot UVs
                -1, -1, -1, -1, // North UVs
                -1, -1, -1, -1, // South UVs
                -1, -1, -1, -1, // East UVs
                -1, -1, -1, -1  // West UVs
        );
    }

    public void renderBox(BlockState state, int x, int y, int z, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, boolean rTop, boolean rBot, boolean rN, boolean rS, boolean rE, boolean rW,
            boolean fullUVs, Chunk chunk, ChunkManager cm, int rotTop, int rotBot, int rotN, int rotS, int rotE,
            int rotW, boolean mirTop, boolean mirBot, boolean mirN, boolean mirS, boolean mirE, boolean mirW,
            float tu0, float tv0, float tu1, float tv1,
            float bu0, float bv0, float bu1, float bv1,
            float nu0, float nv0, float nu1, float nv1,
            float su0, float sv0, float su1, float sv1,
            float eu0, float ev0, float eu1, float ev1,
            float wu0, float wv0, float wu1, float wv1) {
        float tint = getColorTint();
        float lightTop = 1.0f * tint, lightBot = 0.50f * tint, lightFrontBack = 0.82f * tint,
                lightLeftRight = 0.70f * tint;

        AtlasRegion tTop = getTextureForFace(state, BlockFace.UP);
        AtlasRegion tBot = getTextureForFace(state, BlockFace.DOWN);
        AtlasRegion tNorth = getTextureForFace(state, BlockFace.NORTH);
        AtlasRegion tSouth = getTextureForFace(state, BlockFace.SOUTH);
        AtlasRegion tEast = getTextureForFace(state, BlockFace.EAST);
        AtlasRegion tWest = getTextureForFace(state, BlockFace.WEST);

        // ==========================================
        // DYNAMISCHE TEXTUR-WEICHE
        // ==========================================
        BlockModelData model = getModel();
        boolean isDirectional = (model != null && model.directional_textures);

        float sideV0;
        float sideV1;

        if (isDirectional) {
            // Directional = TRUE (z.B. Holztreppen, Steintreppen)
            // Textur wird stufenlos an die Y-Koordinaten der Box (Bit) zugeschnitten.
            sideV0 = 1.0f - maxY;
            sideV1 = 1.0f - minY;
        } else {
            // Directional = FALSE (z.B. Gras, Dirt)
            // Nutzt die klassische "Obere oder Untere Hälfte"-Bit-Logik
            if (maxY - minY > 0.99f) {
                // Voller Block
                sideV0 = 0.0f;
                sideV1 = 1.0f;
            } else {
                // Halber Block / Treppen-Bit
                if (rTop) {
                    // Kein Bit drüber -> Obere Hälfte der Textur (Gras)
                    sideV0 = 0.0f;
                    sideV1 = 0.5f;
                } else {
                    // Bit drüber -> Untere Hälfte der Textur (Dreck)
                    sideV0 = 0.5f;
                    sideV1 = 1.0f;
                }
            }
        }

        BlockState sTop = getNeighborState(chunk, cm, x, y + 1, z);
        if (rTop && (maxY < 1.0f || shouldRenderFaceAgainstState(state, sTop, BlockFace.UP))) {
            float ao0 = chunk.getAO(x, y + 1, z, -1, 0, 0, 0, 0, -1, cm);
            float ao1 = chunk.getAO(x, y + 1, z, -1, 0, 0, 0, 0, 1, cm);
            float ao2 = chunk.getAO(x, y + 1, z, 1, 0, 0, 0, 0, 1, cm);
            float ao3 = chunk.getAO(x, y + 1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y + 1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y + 1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y + 1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y + 1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y + 1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y + 1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y + 1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y + 1, z, 1, 0, 0, 0, 0, -1, cm);
            addMappedFace(chunk, x + minX, y + maxY, z + maxZ,
                    bilerp(ao0, ao3, ao2, ao1, minX, maxZ) * getCrease(minX, maxY, maxZ), x + maxX, y + maxY, z + maxZ,
                    bilerp(ao0, ao3, ao2, ao1, maxX, maxZ) * getCrease(maxX, maxY, maxZ), x + maxX, y + maxY, z + minZ,
                    bilerp(ao0, ao3, ao2, ao1, maxX, minZ) * getCrease(maxX, maxY, minZ), x + minX, y + maxY, z + minZ,
                    bilerp(ao0, ao3, ao2, ao1, minX, minZ) * getCrease(minX, maxY, minZ), 
                    (tu0 != -1) ? tu0 : (fullUVs ? 0 : minX), (tv0 != -1) ? tv0 : (fullUVs ? 0 : minZ), 
                    (tu1 != -1) ? tu1 : (fullUVs ? 1 : maxX), (tv1 != -1) ? tv1 : (fullUVs ? 1 : maxZ), tTop,
                    lightTop, bilerp(sl0, sl3, sl2, sl1, minX, maxZ), bilerp(sl0, sl3, sl2, sl1, maxX, maxZ),
                    bilerp(sl0, sl3, sl2, sl1, maxX, minZ), bilerp(sl0, sl3, sl2, sl1, minX, minZ),
                    bilerp(bl0, bl3, bl2, bl1, minX, maxZ), bilerp(bl0, bl3, bl2, bl1, maxX, maxZ),
                    bilerp(bl0, bl3, bl2, bl1, maxX, minZ), bilerp(bl0, bl3, bl2, bl1, minX, minZ), rotTop, mirTop);
        }
        BlockState sBot = getNeighborState(chunk, cm, x, y - 1, z);
        if (rBot && (minY > 0.0f || (y > Chunk.MIN_Y && shouldRenderFaceAgainstState(state, sBot, BlockFace.DOWN)))) {
            float ao0 = chunk.getAO(x, y - 1, z, -1, 0, 0, 0, 0, 1, cm);
            float ao1 = chunk.getAO(x, y - 1, z, -1, 0, 0, 0, 0, -1, cm);
            float ao2 = chunk.getAO(x, y - 1, z, 1, 0, 0, 0, 0, -1, cm);
            float ao3 = chunk.getAO(x, y - 1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y - 1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y - 1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y - 1, z, 1, 0, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y - 1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y - 1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y - 1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y - 1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y - 1, z, 1, 0, 0, 0, 0, 1, cm);
            addMappedFace(chunk, x + minX, y + minY, z + minZ,
                    bilerp(ao1, ao2, ao3, ao0, minX, minZ) * getCrease(minX, minY, minZ), x + maxX, y + minY, z + minZ,
                    bilerp(ao1, ao2, ao3, ao0, maxX, minZ) * getCrease(maxX, minY, minZ), x + maxX, y + minY, z + maxZ,
                    bilerp(ao1, ao2, ao3, ao0, maxX, maxZ) * getCrease(maxX, minY, maxZ), x + minX, y + minY, z + maxZ,
                    bilerp(ao1, ao2, ao3, ao0, minX, maxZ) * getCrease(minX, minY, maxZ), 
                    (bu0 != -1) ? bu0 : (fullUVs ? 0 : minX), (bv0 != -1) ? bv0 : (fullUVs ? 0 : minZ), 
                    (bu1 != -1) ? bu1 : (fullUVs ? 1 : maxX), (bv1 != -1) ? bv1 : (fullUVs ? 1 : maxZ), tBot,
                    lightBot, bilerp(sl1, sl2, sl3, sl0, minX, minZ), bilerp(sl1, sl2, sl3, sl0, maxX, minZ),
                    bilerp(sl1, sl2, sl3, sl0, maxX, maxZ), bilerp(sl1, sl2, sl3, sl0, minX, maxZ),
                    bilerp(bl1, bl2, bl3, bl0, minX, minZ), bilerp(bl1, bl2, bl3, bl0, maxX, minZ),
                    bilerp(bl1, bl2, bl3, bl0, maxX, maxZ), bilerp(bl1, bl2, bl3, bl0, minX, maxZ), rotBot, mirBot);
        }
        BlockState sSouth = getNeighborState(chunk, cm, x, y, z + 1);
        if (rS && (maxZ < 1.0f || shouldRenderFaceAgainstState(state, sSouth, BlockFace.SOUTH))) {
            float ao0 = chunk.getAO(x, y, z + 1, -1, 0, 0, 0, -1, 0, cm);
            float ao1 = chunk.getAO(x, y, z + 1, 1, 0, 0, 0, -1, 0, cm);
            float ao2 = chunk.getAO(x, y, z + 1, 1, 0, 0, 0, 1, 0, cm);
            float ao3 = chunk.getAO(x, y, z + 1, -1, 0, 0, 0, 1, 0, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y, z + 1, -1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z + 1, 1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z + 1, 1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z + 1, -1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z + 1, -1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z + 1, 1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z + 1, 1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z + 1, -1, 0, 0, 0, 1, 0, cm);
            addMappedFace(chunk, x + minX, y + minY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, minX, minY) * getCrease(minX, minY, maxZ), x + maxX, y + minY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, maxX, minY) * getCrease(maxX, minY, maxZ), x + maxX, y + maxY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, maxX, maxY) * getCrease(maxX, maxY, maxZ), x + minX, y + maxY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, minX, maxY) * getCrease(minX, maxY, maxZ), 
                    (su0 != -1) ? su0 : (fullUVs ? 0 : minX), (sv0 != -1) ? sv0 : (fullUVs ? 0 : sideV0), 
                    (su1 != -1) ? su1 : (fullUVs ? 1 : maxX), (sv1 != -1) ? sv1 : (fullUVs ? 1 : sideV1),
                    tSouth, lightFrontBack, bilerp(sl0, sl1, sl2, sl3, minX, minY),
                    bilerp(sl0, sl1, sl2, sl3, maxX, minY), bilerp(sl0, sl1, sl2, sl3, maxX, maxY),
                    bilerp(sl0, sl1, sl2, sl3, minX, maxY), bilerp(bl0, bl1, bl2, bl3, minX, minY),
                    bilerp(bl0, bl1, bl2, bl3, maxX, minY), bilerp(bl0, bl1, bl2, bl3, maxX, maxY),
                    bilerp(bl0, bl1, bl2, bl3, minX, maxY), rotS, mirS);
        }
        BlockState sNorth = getNeighborState(chunk, cm, x, y, z - 1);
        if (rN && (minZ > 0.0f || shouldRenderFaceAgainstState(state, sNorth, BlockFace.NORTH))) {
            float ao0 = chunk.getAO(x, y, z - 1, 1, 0, 0, 0, -1, 0, cm);
            float ao1 = chunk.getAO(x, y, z - 1, -1, 0, 0, 0, -1, 0, cm);
            float ao2 = chunk.getAO(x, y, z - 1, -1, 0, 0, 0, 1, 0, cm);
            float ao3 = chunk.getAO(x, y, z - 1, 1, 0, 0, 0, 1, 0, cm);
            float sl0 = chunk.getSmoothSkyLight(x, y, z - 1, 1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z - 1, -1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z - 1, -1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z - 1, 1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z - 1, 1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z - 1, -1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z - 1, -1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z - 1, 1, 0, 0, 0, 1, 0, cm);
            addMappedFace(chunk, x + maxX, y + minY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, maxX, minY) * getCrease(maxX, minY, minZ), x + minX, y + minY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, minX, minY) * getCrease(minX, minY, minZ), x + minX, y + maxY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, minX, maxY) * getCrease(minX, maxY, minZ), x + maxX, y + maxY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, maxX, maxY) * getCrease(maxX, maxY, minZ), 
                    (nu0 != -1) ? nu0 : (fullUVs ? 0 : 1.0f - maxX), (nv0 != -1) ? nv0 : (fullUVs ? 0 : sideV0),
                    (nu1 != -1) ? nu1 : (fullUVs ? 1 : 1.0f - minX), (nv1 != -1) ? nv1 : (fullUVs ? 1 : sideV1), tNorth, lightFrontBack, bilerp(sl1, sl0, sl3, sl2, maxX, minY),
                    bilerp(sl1, sl0, sl3, sl2, minX, minY), bilerp(sl1, sl0, sl3, sl2, minX, maxY),
                    bilerp(sl1, sl0, sl3, sl2, maxX, maxY), bilerp(bl1, bl0, bl3, bl2, maxX, minY),
                    bilerp(bl1, bl0, bl3, bl2, minX, minY), bilerp(bl1, bl0, bl3, bl2, minX, maxY),
                    bilerp(bl1, bl0, bl3, bl2, maxX, maxY), rotN, mirN);
        }
        BlockState sWest = getNeighborState(chunk, cm, x - 1, y, z);
        if (rW && (minX > 0.0f || shouldRenderFaceAgainstState(state, sWest, BlockFace.WEST))) {
            float ao0 = chunk.getAO(x - 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float ao1 = chunk.getAO(x - 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float ao2 = chunk.getAO(x - 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float ao3 = chunk.getAO(x - 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl0 = chunk.getSmoothSkyLight(x - 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x - 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x - 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x - 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x - 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x - 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x - 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x - 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            addMappedFace(chunk, x + minX, y + minY, z + minZ,
                    bilerp(ao0, ao1, ao2, ao3, minZ, minY) * getCrease(minX, minY, minZ), x + minX, y + minY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, maxZ, minY) * getCrease(minX, minY, maxZ), x + minX, y + maxY, z + maxZ,
                    bilerp(ao0, ao1, ao2, ao3, maxZ, maxY) * getCrease(minX, maxY, maxZ), x + minX, y + maxY, z + minZ,
                    bilerp(ao0, ao1, ao2, ao3, minZ, maxY) * getCrease(minX, maxY, minZ), 
                    (wu0 != -1) ? wu0 : (fullUVs ? 0 : minZ), (wv0 != -1) ? wv0 : (fullUVs ? 0 : sideV0), 
                    (wu1 != -1) ? wu1 : (fullUVs ? 1 : maxZ), (wv1 != -1) ? wv1 : (fullUVs ? 1 : sideV1),
                    tWest, lightLeftRight, bilerp(sl0, sl1, sl2, sl3, minZ, minY),
                    bilerp(sl0, sl1, sl2, sl3, maxZ, minY), bilerp(sl0, sl1, sl2, sl3, maxZ, maxY),
                    bilerp(sl0, sl1, sl2, sl3, minZ, maxY), bilerp(bl0, bl1, bl2, bl3, minZ, minY),
                    bilerp(bl0, bl1, bl2, bl3, maxZ, minY), bilerp(bl0, bl1, bl2, bl3, maxZ, maxY),
                    bilerp(bl0, bl1, bl2, bl3, minZ, maxY), rotW, mirW);
        }
        BlockState sEast = getNeighborState(chunk, cm, x + 1, y, z);
        if (rE && (maxX < 1.0f || shouldRenderFaceAgainstState(state, sEast, BlockFace.EAST))) {
            float ao0 = chunk.getAO(x + 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float ao1 = chunk.getAO(x + 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float ao2 = chunk.getAO(x + 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float ao3 = chunk.getAO(x + 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl0 = chunk.getSmoothSkyLight(x + 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x + 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x + 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x + 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x + 1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x + 1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x + 1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x + 1, y, z, 0, 1, 0, 0, 0, 1, cm);
            addMappedFace(chunk, x + maxX, y + minY, z + maxZ,
                    bilerp(ao1, ao0, ao3, ao2, maxZ, minY) * getCrease(maxX, minY, maxZ), x + maxX, y + minY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, minZ, minY) * getCrease(maxX, minY, minZ), x + maxX, y + maxY, z + minZ,
                    bilerp(ao1, ao0, ao3, ao2, minZ, maxY) * getCrease(maxX, maxY, minZ), x + maxX, y + maxY, z + maxZ,
                    bilerp(ao1, ao0, ao3, ao2, maxZ, maxY) * getCrease(maxX, maxY, maxZ), 
                    (eu0 != -1) ? eu0 : (fullUVs ? 0 : 1.0f - maxZ), (ev0 != -1) ? ev0 : (fullUVs ? 0 : sideV0),
                    (eu1 != -1) ? eu1 : (fullUVs ? 1 : 1.0f - minZ), (ev1 != -1) ? ev1 : (fullUVs ? 1 : sideV1), tEast, lightLeftRight, bilerp(sl1, sl0, sl3, sl2, maxZ, minY),
                    bilerp(sl1, sl0, sl3, sl2, minZ, minY), bilerp(sl1, sl0, sl3, sl2, minZ, maxY),
                    bilerp(sl1, sl0, sl3, sl2, maxZ, maxY), bilerp(bl1, bl0, bl3, bl2, maxZ, minY),
                    bilerp(bl1, bl0, bl3, bl2, minZ, minY), bilerp(bl1, bl0, bl3, bl2, minZ, maxY),
                    bilerp(bl1, bl0, bl3, bl2, maxZ, maxY), rotE, mirE);
        }
    }
}
