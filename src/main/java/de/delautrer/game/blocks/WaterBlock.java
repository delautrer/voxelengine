package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.game.blocks.state.IntProperty;
import de.delautrer.game.blocks.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class WaterBlock extends Block {

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static final IntProperty LEVEL = IntProperty.create("level", 0, 8);

    public WaterBlock() {
        super(false, true, true, false);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(LEVEL);
    }

    private boolean isWaterReplaceable(Block b) {
        return b == BlockRegistry.AIR || b.canWaterFlowInto();
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        world.getTickScheduler().scheduleTick(new Vector3i(x, y, z), this, 5);
    }

    @Override
    public void scheduledTick(World world, int x, int y, int z) {
        BlockState currentStateObj = world.getBlockState(x, y, z);
        if (currentStateObj.getBlock() != this) return;

        int currentLevel = currentStateObj.getValue(LEVEL);
        int expectedLevel = calculateExpectedState(world, x, y, z, currentLevel);

        if (expectedLevel == 0) {
            world.setBlockState(x, y, z, BlockRegistry.AIR.getDefaultState());
            return;
        } else if (expectedLevel != currentLevel) {
            BlockState newState = getDefaultState().with(LEVEL, expectedLevel);
            world.setBlockState(x, y, z, newState);
            currentLevel = expectedLevel;
        }

        if (currentLevel > 0) {
            tryFlow(world, x, y, z, currentLevel);
        }
    }

    private void tryFlow(World world, int x, int y, int z, int currentLevel) {
        BlockState blockBelow = world.getBlockState(x, y - 1, z);

        if (isWaterReplaceable(blockBelow.getBlock()) || (blockBelow.getBlock() == this && blockBelow.getValue(LEVEL) < 8)) {
            world.setBlockState(x, y - 1, z, getDefaultState().with(LEVEL, 7));
            return;
        }

        if (currentLevel > 1) {
            for (int i = 0; i < 4; i++) {
                int nx = x + DIRS[i][0];
                int nz = z + DIRS[i][1];
                Block nBlock = world.getBlockState(nx, y, nz).getBlock();
                if (isWaterReplaceable(nBlock)) {
                    boolean canFlow = canFlowInto(world, x, y, z, i);
                    if (canFlow) {
                        world.setBlockState(nx, y, nz, getDefaultState().with(LEVEL, currentLevel - 1));
                    }
                }
            }
        }
    }

    private int calculateExpectedState(World world, int x, int y, int z, int currentLevel) {
        if (currentLevel == 8) return 8;
        if (world.getBlockState(x, y + 1, z).getBlock() == this) return 7;

        int sources = 0;
        for (int[] dir : DIRS) {
            BlockState neighbor = world.getBlockState(x + dir[0], y, z + dir[1]);
            if (neighbor.getBlock() == this && neighbor.getValue(LEVEL) == 8) sources++;
        }

        if (sources >= 2) {
            BlockState blockBelow = world.getBlockState(x, y - 1, z);
            if (!isWaterReplaceable(blockBelow.getBlock()) && blockBelow.getBlock() != this) {
                return 8;
            }
        }

        int maxFlowLevel = 0;
        for (int i = 0; i < 4; i++) {
            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];
            BlockState neighbor = world.getBlockState(nx, y, nz);
            if (neighbor.getBlock() == this) {
                int ns = neighbor.getValue(LEVEL);
                BlockState neighborBelow = world.getBlockState(nx, y - 1, nz);
                boolean neighborIsFalling = isWaterReplaceable(neighborBelow.getBlock()) ||
                        (neighborBelow.getBlock() == this && neighborBelow.getValue(LEVEL) < 8);
                if (!neighborIsFalling) {
                    if (ns > 1 && canFlowInto(world, nx, y, nz, opposite(i))) {
                        if (ns > maxFlowLevel) maxFlowLevel = ns;
                    }
                }
            }
        }
        return maxFlowLevel > 1 ? (maxFlowLevel - 1) : 0;
    }

    private boolean canFlowInto(World world, int wx, int wy, int wz, int dirToUs) {
        int[] costs = new int[4];
        int minCost = 999;
        for (int i = 0; i < 4; i++) {
            int nx = wx + DIRS[i][0];
            int nz = wz + DIRS[i][1];
            if (isSolid(world, nx, wy, nz)) costs[i] = 999;
            else if (canFallInto(world, nx, wy - 1, nz)) costs[i] = 0;
            else costs[i] = calculateDropCost(world, nx, wy, nz, 1, opposite(i));
            if (costs[i] < minCost) minCost = costs[i];
        }
        if (minCost > 4) return !isSolid(world, wx + DIRS[dirToUs][0], wy, wz + DIRS[dirToUs][1]);
        return costs[dirToUs] == minCost;
    }

    private int calculateDropCost(World world, int x, int y, int z, int distance, int incomingDir) {
        if (distance > 4 || isSolid(world, x, y, z)) return 999;
        if (canFallInto(world, x, y - 1, z)) return distance;
        int minCost = 999;
        for (int i = 0; i < 4; i++) {
            if (i == incomingDir) continue;
            int nx = x + DIRS[i][0], nz = z + DIRS[i][1];
            int cost = calculateDropCost(world, nx, y, nz, distance + 1, opposite(i));
            if (cost < minCost) minCost = cost;
        }
        return minCost;
    }

    private boolean isSolid(World world, int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return true;
        Block b = world.getBlockState(x, y, z).getBlock();
        return !isWaterReplaceable(b) && b != this;
    }

    private boolean canFallInto(World world, int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return false;
        BlockState state = world.getBlockState(x, y, z);
        Block b = state.getBlock();
        return isWaterReplaceable(b) || (b == this && state.getValue(LEVEL) < 8);
    }

    private int opposite(int dir) {
        return switch (dir) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            default -> 2;
        };
    }

    // --- RENDER LOGIK ---
    private float getWaterHeight(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        if (y < 0 || y >= Chunk.HEIGHT) return 1.0f;
        int globalX = chunk.getWorldX() * Chunk.SIZE + x;
        int globalZ = chunk.getWorldZ() * Chunk.SIZE + z;
        Chunk targetChunk = cm.getChunkAtBlock(globalX, y, globalZ);
        if (targetChunk == null) return 1.0f;
        int localX = Math.floorMod(globalX, Chunk.SIZE);
        int localZ = Math.floorMod(globalZ, Chunk.SIZE);
        BlockState state = targetChunk.getBlockState(localX, y, localZ);
        if (state.getBlock() != this) return 1.0f;
        if (y + 1 < Chunk.HEIGHT) {
            BlockState topState = targetChunk.getBlockState(localX, y + 1, localZ);
            if (topState.getBlock() == this) return 1.0f;
        }
        int level = state.getValue(LEVEL);
        return Math.max(0.1f, level / 9.0f);
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock == this) {
            return myHeight > neighborHeight + 0.01f;
        }
        return neighborBlock.isTransparent;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float h = getWaterHeight(x, y, z, chunk, cm);
        float yTop = y + h;
        float lightTop = 1.0f, lightBot = 0.4f, lightFrontBack = 0.8f, lightLeftRight = 0.65f;
        TextureStitcher.AtlasRegion reg = getModel().top;

        // TOP FACE
        Block topNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y + 1, z, cm));
        if (shouldRenderFaceAgainst(topNeighbor, h, 1.0f) || h < 0.99f) {
            float sl0 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y+1, z, -1, 0, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y+1, z, 1, 0, 0, 0, 0, -1, cm);
            chunk.addFace(x,yTop,z,1, x,yTop,z+1,1, x+1,yTop,z+1,1, x+1,yTop,z,1,reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, lightTop, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // BOTTOM FACE
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
            chunk.addFace(x,y,z+1,1, x,y,z,1, x+1,y,z,1, x+1,y,z+1,1,reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, lightBot, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // --- SEITEN-LOGIK MIT PARTIELLEN FACES ---

        // Z+ (Vorne)
        float nHeightZPlus = getWaterHeight(x, y, z + 1, chunk, cm);
        Block zPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z + 1, cm));
        if (shouldRenderFaceAgainst(zPlusNeighbor, h, nHeightZPlus)) {
            float yBot = (zPlusNeighbor == this) ? y + nHeightZPlus : y;
            float vBot = reg.v1;
            if (zPlusNeighbor == this && h > 0) vBot = reg.v1 - (nHeightZPlus / h) * (reg.v1 - reg.v0);

            float sl0 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z+1, 1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z+1, -1, 0, 0, 0, 1, 0, cm);
            chunk.addFace(x,yBot,z+1,1, x+1,yBot,z+1,1, x+1,yTop,z+1,1, x,yTop,z+1,1,reg.u0, reg.v0, reg.u1, vBot, reg.layer, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // Z- (Hinten)
        float nHeightZMinus = getWaterHeight(x, y, z - 1, chunk, cm);
        Block zMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x, y, z - 1, cm));
        if (shouldRenderFaceAgainst(zMinusNeighbor, h, nHeightZMinus)) {
            float yBot = (zMinusNeighbor == this) ? y + nHeightZMinus : y;
            float vBot = reg.v1;
            if (zMinusNeighbor == this && h > 0) vBot = reg.v1 - (nHeightZMinus / h) * (reg.v1 - reg.v0);

            float sl0 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float sl1 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float sl2 = chunk.getSmoothSkyLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float sl3 = chunk.getSmoothSkyLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);
            float bl0 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, -1, 0, cm);
            float bl1 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, -1, 0, cm);
            float bl2 = chunk.getSmoothBlockLight(x, y, z-1, -1, 0, 0, 0, 1, 0, cm);
            float bl3 = chunk.getSmoothBlockLight(x, y, z-1, 1, 0, 0, 0, 1, 0, cm);
            chunk.addFace(x+1,yBot,z,1, x,yBot,z,1, x,yTop,z,1, x+1,yTop,z,1,reg.u0, reg.v0, reg.u1, vBot, reg.layer, lightFrontBack, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X- (Links)
        float nHeightXMinus = getWaterHeight(x - 1, y, z, chunk, cm);
        Block xMinusNeighbor = BlockRegistry.get(chunk.getBlockAt(x - 1, y, z, cm));
        if (shouldRenderFaceAgainst(xMinusNeighbor, h, nHeightXMinus)) {
            float yBot = (xMinusNeighbor == this) ? y + nHeightXMinus : y;
            float vBot = reg.v1;
            if (xMinusNeighbor == this && h > 0) vBot = reg.v1 - (nHeightXMinus / h) * (reg.v1 - reg.v0);

            float sl0 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl1 = chunk.getSmoothSkyLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl2 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float sl3 = chunk.getSmoothSkyLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl0 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl1 = chunk.getSmoothBlockLight(x-1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl2 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl3 = chunk.getSmoothBlockLight(x-1, y, z, 0, 1, 0, 0, 0, -1, cm);
            chunk.addFace(x,yBot,z,1, x,yBot,z+1,1, x,yTop,z+1,1, x,yTop,z,1,reg.u0, reg.v0, reg.u1, vBot, reg.layer, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }

        // X+ (Rechts)
        float nHeightXPlus = getWaterHeight(x + 1, y, z, chunk, cm);
        Block xPlusNeighbor = BlockRegistry.get(chunk.getBlockAt(x + 1, y, z, cm));
        if (shouldRenderFaceAgainst(xPlusNeighbor, h, nHeightXPlus)) {
            float yBot = (xPlusNeighbor == this) ? y + nHeightXPlus : y;
            float vBot = reg.v1;
            if (xPlusNeighbor == this && h > 0) vBot = reg.v1 - (nHeightXPlus / h) * (reg.v1 - reg.v0);

            float sl0 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float sl1 = chunk.getSmoothSkyLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float sl2 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float sl3 = chunk.getSmoothSkyLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);
            float bl0 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, 1, cm);
            float bl1 = chunk.getSmoothBlockLight(x+1, y, z, 0, -1, 0, 0, 0, -1, cm);
            float bl2 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, -1, cm);
            float bl3 = chunk.getSmoothBlockLight(x+1, y, z, 0, 1, 0, 0, 0, 1, cm);
            chunk.addFace(x+1,yBot,z+1,1, x+1,yBot,z,1, x+1,yTop,z,1, x+1,yTop,z+1,1,reg.u0, reg.v0, reg.u1, vBot, reg.layer, lightLeftRight, this, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
        }
    }

    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return true;
    }
}