package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.registry.Tag;
import de.delautrer.game.registry.TagRegistry;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;

public class FenceBlock extends CubeBlock {

    @SuppressWarnings("this-escape")
    public FenceBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false, isRaycastable=true
        setSoundMaterialName("wood");
        setHardness(2.0f);

        this.mesher = (state, x, y, z, chunk, cm) -> {
            boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
            boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
            boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
            boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

            // Center post
            renderBox(state, x, y, z, 0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f,
                    true, true, true, true, true, true, false, chunk, cm);

            // Arms (Upper rail: y 0.75..0.9375, Lower rail: y 0.375..0.5625)
            if (n) {
                renderBox(state, x, y, z, 0.4375f, 0.75f, 0.0f, 0.5625f, 0.9375f, 0.375f, true, true, true, false, true, true, false, chunk, cm);
                renderBox(state, x, y, z, 0.4375f, 0.375f, 0.0f, 0.5625f, 0.5625f, 0.375f, true, true, true, false, true, true, false, chunk, cm);
            }
            if (s) {
                renderBox(state, x, y, z, 0.4375f, 0.75f, 0.625f, 0.5625f, 0.9375f, 1.0f, true, true, false, true, true, true, false, chunk, cm);
                renderBox(state, x, y, z, 0.4375f, 0.375f, 0.625f, 0.5625f, 0.5625f, 1.0f, true, true, false, true, true, true, false, chunk, cm);
            }
            if (w) {
                renderBox(state, x, y, z, 0.0f, 0.75f, 0.4375f, 0.375f, 0.9375f, 0.5625f, true, true, true, true, false, true, false, chunk, cm);
                renderBox(state, x, y, z, 0.0f, 0.375f, 0.4375f, 0.375f, 0.5625f, 0.5625f, true, true, true, true, false, true, false, chunk, cm);
            }
            if (e) {
                renderBox(state, x, y, z, 0.625f, 0.75f, 0.4375f, 1.0f, 0.9375f, 0.5625f, true, true, true, true, true, false, false, chunk, cm);
                renderBox(state, x, y, z, 0.625f, 0.375f, 0.4375f, 1.0f, 0.5625f, 0.5625f, true, true, true, true, true, false, false, chunk, cm);
            }
        };
    }

    public static boolean canFenceConnectTo(World world, int x, int y, int z, Direction dir) {
        int dx = dir == Direction.EAST ? 1 : (dir == Direction.WEST ? -1 : 0);
        int dz = dir == Direction.SOUTH ? 1 : (dir == Direction.NORTH ? -1 : 0);
        int nx = x + dx;
        int ny = y;
        int nz = z + dz;

        Block nBlock = world.getBlock(nx, ny, nz);
        if (nBlock == null || nBlock.isAir()) return false;

        if (nBlock instanceof FenceBlock) return true;

        Tag<Block> fencesTag = TagRegistry.getBlockTag("veinstride:fences");
        if (fencesTag != null && fencesTag.contains(nBlock)) return true;

        Tag<Block> gatesTag = TagRegistry.getBlockTag("veinstride:fence_gates");
        if (gatesTag != null && gatesTag.contains(nBlock)) return true;

        NamespacedKey key = Registries.BLOCKS.getKey(nBlock);
        String keyName = key != null ? key.getKey() : "";

        if (keyName.contains("fence")) return true;

        if (nBlock instanceof StairBlock) {
            BlockState nState = world.getBlockState(nx, ny, nz);
            if (nState != null && nState.contains(StairBlock.FACING)) {
                Direction stairFacing = nState.getValue(StairBlock.FACING);
                if (stairFacing.getOpposite() == dir) {
                    return true;
                }
            }
            return false;
        }

        if (nBlock instanceof PlantBlock || nBlock instanceof CarpetBlock || nBlock instanceof LayerBlock || nBlock instanceof PaneBlock || nBlock instanceof TorchBlock) {
            return false;
        }

        if (nBlock.isSolid && !nBlock.isPassable) {
            return true;
        }

        return false;
    }

    public static BlockState updateFenceConnections(World world, int x, int y, int z, BlockState state) {
        if (state == null) return state;

        boolean n = canFenceConnectTo(world, x, y, z, Direction.NORTH);
        boolean e = canFenceConnectTo(world, x, y, z, Direction.EAST);
        boolean s = canFenceConnectTo(world, x, y, z, Direction.SOUTH);
        boolean w = canFenceConnectTo(world, x, y, z, Direction.WEST);

        BlockState newState = state;
        if (state.contains(ConnectedState.NORTH)) newState = newState.with(ConnectedState.NORTH, n);
        if (state.contains(ConnectedState.EAST)) newState = newState.with(ConnectedState.EAST, e);
        if (state.contains(ConnectedState.SOUTH)) newState = newState.with(ConnectedState.SOUTH, s);
        if (state.contains(ConnectedState.WEST)) newState = newState.with(ConnectedState.WEST, w);

        return newState;
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        super.appendProperties(properties);
        properties.add(ConnectedState.NORTH);
        properties.add(ConnectedState.EAST);
        properties.add(ConnectedState.SOUTH);
        properties.add(ConnectedState.WEST);
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
        boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
        boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
        boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

        List<AABB> boxes = new ArrayList<>();
        // Center post
        boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.375f), new Vector3f(0.625f, 1.0f, 0.625f)));
        if (n) boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.0f), new Vector3f(0.625f, 1.0f, 0.375f)));
        if (s) boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.625f), new Vector3f(0.625f, 1.0f, 1.0f)));
        if (w) boxes.add(new AABB(new Vector3f(0.0f, 0.0f, 0.375f), new Vector3f(0.375f, 1.0f, 0.625f)));
        if (e) boxes.add(new AABB(new Vector3f(0.625f, 0.0f, 0.375f), new Vector3f(1.0f, 1.0f, 0.625f)));

        return boxes;
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
        boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
        boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
        boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

        List<AABB> boxes = new ArrayList<>();
        // Collision height is 1.5f so players/mobs cannot jump over fences
        boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.375f), new Vector3f(0.625f, 1.5f, 0.625f)));
        if (n) boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.0f), new Vector3f(0.625f, 1.5f, 0.375f)));
        if (s) boxes.add(new AABB(new Vector3f(0.375f, 0.0f, 0.625f), new Vector3f(0.625f, 1.5f, 1.0f)));
        if (w) boxes.add(new AABB(new Vector3f(0.0f, 0.0f, 0.375f), new Vector3f(0.375f, 1.5f, 0.625f)));
        if (e) boxes.add(new AABB(new Vector3f(0.625f, 0.0f, 0.375f), new Vector3f(1.0f, 1.5f, 0.625f)));

        return boxes;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        BlockState state = getDefaultState();
        return updateFenceConnections(world, hitPos.x, hitPos.y, hitPos.z, state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        BlockState state = world.getBlockState(x, y, z);
        if (state != null && state.getBlock() == this) {
            BlockState newState = updateFenceConnections(world, x, y, z, state);
            if (newState != state) {
                world.setBlockWithState(x, y, z, this, newState.getStateId(), true);
            }
        }
    }

    private boolean isFenceCellOccupied(BlockState state, int gx, int gz) {
        if (gx < 0 || gx > 2 || gz < 0 || gz > 2) return false;
        if (gx == 1 && gz == 1) return true; // Center post
        boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
        boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
        boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
        boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);
        if (gx == 1 && gz == 0) return n;
        if (gx == 1 && gz == 2) return s;
        if (gx == 0 && gz == 1) return w;
        if (gx == 2 && gz == 1) return e;
        return false;
    }

    private boolean isFenceCellOccupied3D(BlockState state, int gx, int gy, int gz) {
        if (gy != 0) return false;
        return isFenceCellOccupied(state, gx, gz);
    }

    @Override
    public float[] getHighlightVertices(BlockState state) {
        float[] xCoords = {0.0f, 0.375f, 0.625f, 1.0f};
        float[] yCoords = {0.0f, 1.0f};
        float[] zCoords = {0.0f, 0.375f, 0.625f, 1.0f};

        int nx = xCoords.length, ny = yCoords.length, nz = zCoords.length;
        float[] verts = new float[nx * ny * nz * 3];
        int idx = 0;
        for (int gz = 0; gz < nz; gz++) {
            for (int gy = 0; gy < ny; gy++) {
                for (int gx = 0; gx < nx; gx++) {
                    verts[idx++] = xCoords[gx];
                    verts[idx++] = yCoords[gy];
                    verts[idx++] = zCoords[gz];
                }
            }
        }
        return verts;
    }

    @Override
    public int[] getHighlightIndices(BlockState state) {
        float[] xCoords = {0.0f, 0.375f, 0.625f, 1.0f};
        float[] yCoords = {0.0f, 1.0f};
        float[] zCoords = {0.0f, 0.375f, 0.625f, 1.0f};

        int nx = xCoords.length;
        int ny = yCoords.length;
        int nz = zCoords.length;

        List<Integer> inds = new ArrayList<>();

        // 1. Lines parallel to X axis
        for (int gz = 0; gz < nz; gz++) {
            for (int gy = 0; gy < ny; gy++) {
                for (int gx = 0; gx < nx - 1; gx++) {
                    boolean c00 = isFenceCellOccupied3D(state, gx, gy - 1, gz - 1);
                    boolean c01 = isFenceCellOccupied3D(state, gx, gy - 1, gz);
                    boolean c10 = isFenceCellOccupied3D(state, gx, gy, gz - 1);
                    boolean c11 = isFenceCellOccupied3D(state, gx, gy, gz);
                    int count = (c00 ? 1 : 0) + (c01 ? 1 : 0) + (c10 ? 1 : 0) + (c11 ? 1 : 0);
                    if (count == 1 || count == 3 || (count == 2 && c00 == c11)) {
                        int v0 = gx + gy * nx + gz * (nx * ny);
                        int v1 = (gx + 1) + gy * nx + gz * (nx * ny);
                        inds.add(v0);
                        inds.add(v1);
                    }
                }
            }
        }

        // 2. Lines parallel to Y axis
        for (int gz = 0; gz < nz; gz++) {
            for (int gy = 0; gy < ny - 1; gy++) {
                for (int gx = 0; gx < nx; gx++) {
                    boolean c00 = isFenceCellOccupied3D(state, gx - 1, gy, gz - 1);
                    boolean c01 = isFenceCellOccupied3D(state, gx - 1, gy, gz);
                    boolean c10 = isFenceCellOccupied3D(state, gx, gy, gz - 1);
                    boolean c11 = isFenceCellOccupied3D(state, gx, gy, gz);
                    int count = (c00 ? 1 : 0) + (c01 ? 1 : 0) + (c10 ? 1 : 0) + (c11 ? 1 : 0);
                    if (count == 1 || count == 3 || (count == 2 && c00 == c11)) {
                        int v0 = gx + gy * nx + gz * (nx * ny);
                        int v1 = gx + (gy + 1) * nx + gz * (nx * ny);
                        inds.add(v0);
                        inds.add(v1);
                    }
                }
            }
        }

        // 3. Lines parallel to Z axis
        for (int gz = 0; gz < nz - 1; gz++) {
            for (int gy = 0; gy < ny; gy++) {
                for (int gx = 0; gx < nx; gx++) {
                    boolean c00 = isFenceCellOccupied3D(state, gx - 1, gy - 1, gz);
                    boolean c01 = isFenceCellOccupied3D(state, gx - 1, gy, gz);
                    boolean c10 = isFenceCellOccupied3D(state, gx, gy - 1, gz);
                    boolean c11 = isFenceCellOccupied3D(state, gx, gy, gz);
                    int count = (c00 ? 1 : 0) + (c01 ? 1 : 0) + (c10 ? 1 : 0) + (c11 ? 1 : 0);
                    if (count == 1 || count == 3 || (count == 2 && c00 == c11)) {
                        int v0 = gx + gy * nx + gz * (nx * ny);
                        int v1 = gx + gy * nx + (gz + 1) * (nx * ny);
                        inds.add(v0);
                        inds.add(v1);
                    }
                }
            }
        }

        int[] result = new int[inds.size()];
        for (int i = 0; i < inds.size(); i++) {
            result[i] = inds.get(i);
        }
        return result;
    }
}
