package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class WaterBlock extends Block {

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public WaterBlock() {
        super(false, true, true, false);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        world.getTickScheduler().scheduleTick(new Vector3i(x, y, z), this, 5);
    }

    @Override
    public void scheduledTick(World world, int x, int y, int z) {
        Chunk chunk = world.getChunkManager().getChunkAtBlock(x, y, z);
        if (chunk == null) return;

        byte currentState = chunk.getState(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
        byte expectedState = calculateExpectedState(world, x, y, z, currentState);

        if (expectedState == 0) {
            world.setBlock(x, y, z, (byte) 0);
            return;
        } else if (expectedState != currentState) {
            world.setBlockWithState(x, y, z, getId(), expectedState);
            currentState = expectedState;
        }

        if (currentState > 0) {
            tryFlow(world, x, y, z, currentState);
        }
    }

    private void tryFlow(World world, int x, int y, int z, byte currentState) {
        byte airId = BlockRegistry.AIR.getId();
        byte waterId = getId();

        byte blockBelow = world.getBlockAt(x, y - 1, z);
        byte stateBelow = getWaterState(world, x, y - 1, z);

        // 1. Fallen hat höchste Priorität
        if (blockBelow == airId || (blockBelow == waterId && stateBelow < 8)) {
            world.setBlockWithState(x, y - 1, z, waterId, (byte) 7);
            return; // Wenn es fällt, breitet es sich NICHT zur Seite aus!
        }

        // 2. Deine originale Loch-Such-Logik!
        if (currentState > 1) {
            for (int i = 0; i < 4; i++) {
                int nx = x + DIRS[i][0];
                int nz = z + DIRS[i][1];

                if (world.getBlockAt(nx, y, nz) == airId) {
                    if (canFlowInto(world, x, y, z, i, waterId, airId)) {
                        world.setBlockWithState(nx, y, nz, waterId, (byte) (currentState - 1));
                    }
                }
            }
        }
    }

    private byte calculateExpectedState(World world, int x, int y, int z, byte currentState) {
        byte airId = BlockRegistry.AIR.getId();
        byte waterId = getId();

        if (currentState == 8) return 8;

        if (world.getBlockAt(x, y + 1, z) == waterId) return 7;

        int sources = 0;
        for (int[] dir : DIRS) {
            if (getWaterState(world, x + dir[0], y, z + dir[1]) == 8) sources++;
        }
        if (sources >= 2) {
            byte blockBelow = world.getBlockAt(x, y - 1, z);
            if (blockBelow != airId && blockBelow != waterId) {
                return 8;
            }
        }

        int maxFlowLevel = 0;
        for (int i = 0; i < 4; i++) {
            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];

            if (world.getBlockAt(nx, y, nz) == waterId) {
                byte ns = getWaterState(world, nx, y, nz);
                byte neighborBelow = world.getBlockAt(nx, y - 1, nz);
                byte neighborBelowState = getWaterState(world, nx, y - 1, nz);

                // WICHTIG: Das hatte ich vergessen. Wasser fließt horizontal NICHT aus fallendem Wasser!
                boolean neighborIsFalling = (neighborBelow == airId) || (neighborBelow == waterId && neighborBelowState < 8);

                if (!neighborIsFalling) {
                    // Der "canFlowInto"-Check muss hier prüfen, ob der Nachbar (nx, nz) in unsere Richtung (opposite) fließen darf!
                    if (ns > 1 && canFlowInto(world, nx, y, nz, opposite(i), waterId, airId)) {
                        if (ns > maxFlowLevel) {
                            maxFlowLevel = ns;
                        }
                    }
                }
            }
        }

        return maxFlowLevel > 1 ? (byte) (maxFlowLevel - 1) : 0;
    }

    private boolean canFlowInto(World world, int wx, int wy, int wz, int dirToUs, byte waterId, byte airId) {
        int[] costs = new int[4];
        int minCost = 999;

        for (int i = 0; i < 4; i++) {
            int nx = wx + DIRS[i][0];
            int nz = wz + DIRS[i][1];

            if (isSolid(world, nx, wy, nz, waterId, airId)) {
                costs[i] = 999;
            } else if (!isSolid(world, nx, wy - 1, nz, waterId, airId)) {
                costs[i] = 0;
            } else {
                costs[i] = calculateDropCost(world, nx, wy, nz, 1, opposite(i), waterId, airId);
            }

            if (costs[i] < minCost) {
                minCost = costs[i];
            }
        }

        if (minCost > 4) {
            return !isSolid(world, wx + DIRS[dirToUs][0], wy, wz + DIRS[dirToUs][1], waterId, airId);
        }

        return costs[dirToUs] == minCost;
    }

    private int calculateDropCost(World world, int x, int y, int z, int distance, int incomingDir, byte waterId, byte airId) {
        if (distance > 4 || isSolid(world, x, y, z, waterId, airId)) return 999;
        if (!isSolid(world, x, y - 1, z, waterId, airId)) return distance;

        int minCost = 999;
        for (int i = 0; i < 4; i++) {
            if (i == incomingDir) continue;
            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];
            int cost = calculateDropCost(world, nx, y, nz, distance + 1, opposite(i), waterId, airId);
            if (cost < minCost) minCost = cost;
        }

        return minCost;
    }

    private boolean isSolid(World world, int x, int y, int z, byte waterId, byte airId) {
        if (y < 0 || y >= Chunk.HEIGHT) return true;
        byte b = world.getBlockAt(x, y, z);
        return b != airId && b != waterId;
    }

    private int opposite(int dir) {
        return switch (dir) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            default -> 2;
        };
    }

    private byte getWaterState(World world, int x, int y, int z) {
        if (world.getBlockAt(x, y, z) != getId()) return 0;
        Chunk c = world.getChunkManager().getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getState(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
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