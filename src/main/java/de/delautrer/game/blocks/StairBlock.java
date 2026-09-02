package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import java.util.ArrayList;

public class StairBlock extends CubeBlock {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final EnumProperty<StairShape> SHAPE = EnumProperty.create("shape", StairShape.class);

    @SuppressWarnings("this-escape")
    public StairBlock(boolean isSolid, boolean isTransparent) {
        super(isSolid, isTransparent);
        this.mesher = new de.delautrer.engine.graphics.meshing.StairMesher(this);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(FACING); properties.add(HALF); properties.add(SHAPE);
    }

    // ==========================================
    // 1. PLATZIERUNG & ECKEN-LOGIK
    // ==========================================

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        float yaw = ((LocalPlayer) player).getCamera().getYaw();
        yaw = (yaw % 360 + 360) % 360;
        Direction facing;
        if (yaw >= 45 && yaw < 135) facing = Direction.SOUTH;
        else if (yaw >= 135 && yaw < 225) facing = Direction.WEST;
        else if (yaw >= 225 && yaw < 315) facing = Direction.NORTH;
        else facing = Direction.EAST;

        Half half = Half.BOTTOM;
        if (hitFace.y == -1 || (hitFace.y == 0 && (exactHit.y - Math.floor(exactHit.y) > 0.5f))) {
            half = Half.TOP;
        }

        return updateShape(world, hitPos, getDefaultState().with(FACING, facing).with(HALF, half));
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        BlockState currentState = world.getBlockState(x, y, z);
        BlockState newState = updateShape(world, new Vector3i(x,y,z), currentState);
        if (currentState != newState) world.setBlockState(x, y, z, newState);
    }

    private BlockState updateShape(World world, Vector3i pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Half half = state.getValue(HALF);

        Vector3i front = getOffset(pos, facing);
        BlockState frontState = world.getBlockState(front.x, front.y, front.z);
        if (frontState.getBlock() == this && frontState.getValue(HALF) == half) {
            Direction frontFacing = frontState.getValue(FACING);
            if (frontFacing != facing && frontFacing != getOpposite(facing)) {
                return state.with(SHAPE, isLeft(facing, frontFacing) ? StairShape.OUTER_RIGHT : StairShape.OUTER_LEFT);
            }
        }

        Vector3i back = getOffset(pos, getOpposite(facing));
        BlockState backState = world.getBlockState(back.x, back.y, back.z);
        if (backState.getBlock() == this && backState.getValue(HALF) == half) {
            Direction backFacing = backState.getValue(FACING);
            if (backFacing != facing && backFacing != getOpposite(facing)) {
                return state.with(SHAPE, isLeft(facing, backFacing) ? StairShape.INNER_RIGHT : StairShape.INNER_LEFT);
            }
        }
        return state.with(SHAPE, StairShape.STRAIGHT);
    }

    private Vector3i getOffset(Vector3i pos, Direction dir) {
        if (dir == Direction.NORTH) return new Vector3i(pos.x, pos.y, pos.z - 1);
        if (dir == Direction.SOUTH) return new Vector3i(pos.x, pos.y, pos.z + 1);
        if (dir == Direction.EAST) return new Vector3i(pos.x + 1, pos.y, pos.z);
        return new Vector3i(pos.x - 1, pos.y, pos.z);
    }

    private Direction getOpposite(Direction dir) {
        if (dir == Direction.NORTH) return Direction.SOUTH;
        if (dir == Direction.SOUTH) return Direction.NORTH;
        if (dir == Direction.EAST) return Direction.WEST;
        return Direction.EAST;
    }

    private boolean isLeft(Direction myDir, Direction otherDir) {
        if (myDir == Direction.NORTH) return otherDir == Direction.WEST;
        if (myDir == Direction.SOUTH) return otherDir == Direction.EAST;
        if (myDir == Direction.EAST) return otherDir == Direction.NORTH;
        return otherDir == Direction.SOUTH;
    }

    // ==========================================
    // 2. PHYSIK & RAYCAST (Fehlte bei dir!)
    // ==========================================

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        List<AABB> boxes = new ArrayList<>();
        Direction facing = state.getValue(FACING);
        Half half = state.getValue(HALF);
        StairShape shape = state.getValue(SHAPE);

        float baseYMin = half == Half.BOTTOM ? 0.0f : 0.5f;
        float baseYMax = half == Half.BOTTOM ? 0.5f : 1.0f;
        float stepYMin = half == Half.BOTTOM ? 0.5f : 0.0f;
        float stepYMax = half == Half.BOTTOM ? 1.0f : 0.5f;

        // Basis
        boxes.add(new AABB(new Vector3f(0, baseYMin, 0), new Vector3f(1, baseYMax, 1)));

        boolean nw=false, ne=false, sw=false, se=false;
        if (facing == Direction.NORTH) { nw=true; ne=true; }
        else if (facing == Direction.SOUTH) { sw=true; se=true; }
        else if (facing == Direction.WEST) { nw=true; sw=true; }
        else if (facing == Direction.EAST) { ne=true; se=true; }

        if (shape == StairShape.INNER_LEFT) {
            if (facing == Direction.NORTH) se=true; if (facing == Direction.SOUTH) nw=true;
            if (facing == Direction.WEST) ne=true; if (facing == Direction.EAST) sw=true;
        } else if (shape == StairShape.INNER_RIGHT) {
            if (facing == Direction.NORTH) sw=true; if (facing == Direction.SOUTH) ne=true;
            if (facing == Direction.WEST) se=true; if (facing == Direction.EAST) nw=true;
        } else if (shape == StairShape.OUTER_LEFT) {
            if (facing == Direction.NORTH) nw=false; if (facing == Direction.SOUTH) se=false;
            if (facing == Direction.WEST) sw=false; if (facing == Direction.EAST) ne=false;
        } else if (shape == StairShape.OUTER_RIGHT) {
            if (facing == Direction.NORTH) ne=false; if (facing == Direction.SOUTH) sw=false;
            if (facing == Direction.WEST) nw=false; if (facing == Direction.EAST) se=false;
        }

        // Quadranten für Raycast & Kollision
        if (nw) boxes.add(new AABB(new Vector3f(0, stepYMin, 0), new Vector3f(0.5f, stepYMax, 0.5f)));
        if (ne) boxes.add(new AABB(new Vector3f(0.5f, stepYMin, 0), new Vector3f(1, stepYMax, 0.5f)));
        if (sw) boxes.add(new AABB(new Vector3f(0, stepYMin, 0.5f), new Vector3f(0.5f, stepYMax, 1)));
        if (se) boxes.add(new AABB(new Vector3f(0.5f, stepYMin, 0.5f), new Vector3f(1, stepYMax, 1)));

        return boxes;
    }

    // ==========================================
    // 3. OPTISCHER HIGHLIGHTER (Minecraft-Silhouette)
    // ==========================================

    @Override
    public float[] getHighlightVertices(BlockState state) {
        float[] verts = new float[27 * 3];
        int idx = 0;
        for (int gz = 0; gz < 3; gz++) {
            for (int gy = 0; gy < 3; gy++) {
                for (int gx = 0; gx < 3; gx++) {
                    verts[idx++] = gx * 0.5f;
                    verts[idx++] = gy * 0.5f;
                    verts[idx++] = gz * 0.5f;
                }
            }
        }
        return verts;
    }

    @Override
    public int[] getHighlightIndices(BlockState state) {
        boolean[][][] q = getHighlightQuadrants(state);
        List<Integer> inds = new ArrayList<>();

        for (int gx = 0; gx < 2; gx++) {
            for (int gy = 0; gy < 3; gy++) {
                for (int gz = 0; gz < 3; gz++) {
                    if (shouldDrawHighlightLine(qVal(q, gx, gy-1, gz-1), qVal(q, gx, gy-1, gz), qVal(q, gx, gy, gz-1), qVal(q, gx, gy, gz))) {
                        inds.add(gx + gy*3 + gz*9); inds.add((gx+1) + gy*3 + gz*9);
                    }
                }
            }
        }
        for (int gx = 0; gx < 3; gx++) {
            for (int gy = 0; gy < 2; gy++) {
                for (int gz = 0; gz < 3; gz++) {
                    if (shouldDrawHighlightLine(qVal(q, gx-1, gy, gz-1), qVal(q, gx-1, gy, gz), qVal(q, gx, gy, gz-1), qVal(q, gx, gy, gz))) {
                        inds.add(gx + gy*3 + gz*9); inds.add(gx + (gy+1)*3 + gz*9);
                    }
                }
            }
        }
        for (int gx = 0; gx < 3; gx++) {
            for (int gy = 0; gy < 3; gy++) {
                for (int gz = 0; gz < 2; gz++) {
                    if (shouldDrawHighlightLine(qVal(q, gx-1, gy-1, gz), qVal(q, gx-1, gy, gz), qVal(q, gx, gy-1, gz), qVal(q, gx, gy, gz))) {
                        inds.add(gx + gy*3 + gz*9); inds.add(gx + gy*3 + (gz+1)*9);
                    }
                }
            }
        }

        int[] result = new int[inds.size()];
        for (int i = 0; i < inds.size(); i++) result[i] = inds.get(i);
        return result;
    }

    private boolean qVal(boolean[][][] q, int x, int y, int z) {
        if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return false;
        return q[x][y][z];
    }

    private boolean shouldDrawHighlightLine(boolean q00, boolean q01, boolean q10, boolean q11) {
        int count = (q00 ? 1 : 0) + (q01 ? 1 : 0) + (q10 ? 1 : 0) + (q11 ? 1 : 0);
        if (count == 1 || count == 3) return true;
        if (count == 2) return q00 == q11;
        return false;
    }

    private boolean[][][] getHighlightQuadrants(BlockState state) {
        boolean[][][] q = new boolean[2][2][2];
        Half half = state.getValue(HALF);
        Direction facing = state.getValue(FACING);
        StairShape shape = state.getValue(SHAPE);

        int baseY = (half == Half.BOTTOM) ? 0 : 1;
        int stepY = (half == Half.BOTTOM) ? 1 : 0;

        q[0][baseY][0] = true; q[1][baseY][0] = true;
        q[0][baseY][1] = true; q[1][baseY][1] = true;

        boolean nw=false, ne=false, sw=false, se=false;
        if (facing == Direction.NORTH) { nw=true; ne=true; }
        else if (facing == Direction.SOUTH) { sw=true; se=true; }
        else if (facing == Direction.WEST) { nw=true; sw=true; }
        else if (facing == Direction.EAST) { ne=true; se=true; }

        if (shape == StairShape.INNER_LEFT) {
            if (facing == Direction.NORTH) se=true; if (facing == Direction.SOUTH) nw=true;
            if (facing == Direction.WEST) ne=true; if (facing == Direction.EAST) sw=true;
        } else if (shape == StairShape.INNER_RIGHT) {
            if (facing == Direction.NORTH) sw=true; if (facing == Direction.SOUTH) ne=true;
            if (facing == Direction.WEST) se=true; if (facing == Direction.EAST) nw=true;
        } else if (shape == StairShape.OUTER_LEFT) {
            if (facing == Direction.NORTH) nw=false; if (facing == Direction.SOUTH) se=false;
            if (facing == Direction.WEST) sw=false; if (facing == Direction.EAST) ne=false;
        } else if (shape == StairShape.OUTER_RIGHT) {
            if (facing == Direction.NORTH) ne=false; if (facing == Direction.SOUTH) sw=false;
            if (facing == Direction.WEST) nw=false; if (facing == Direction.EAST) se=false;
        }

        q[0][stepY][0] = nw; q[1][stepY][0] = ne;
        q[0][stepY][1] = sw; q[1][stepY][1] = se;

        return q;
    }

    // ==========================================
    // 4. RENDERING DES BLOCKS
    // ==========================================

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        Block nBlock = neighborState.getBlock();
        if (nBlock == null || nBlock.isAir()) return true;

        if (nBlock == this) {
            return true;
        }
        return nBlock.isTransparent;
    }
}