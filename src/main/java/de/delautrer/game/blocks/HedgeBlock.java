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

public class HedgeBlock extends CubeBlock {

    @SuppressWarnings("this-escape")
    public HedgeBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false, isRaycastable=true
        setSoundMaterialName("grass");
        setHardness(0.2f);

        this.mesher = (state, x, y, z, chunk, cm) -> {
            boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
            boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
            boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
            boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

            // Center core (6x16x6, y: 0.0..1.0)
            renderBox(state, x, y, z, 0.3125f, 0.0f, 0.3125f, 0.6875f, 1.0f, 0.6875f,
                    true, true, true, true, true, true, false, chunk, cm);

            // Arms (Height: y 0.0..1.0, Width: 6px from 0.3125 to 0.6875)
            if (n) {
                renderBox(state, x, y, z, 0.3125f, 0.0f, 0.0f, 0.6875f, 1.0f, 0.3125f,
                        true, true, true, false, true, true, false, chunk, cm);
            }
            if (s) {
                renderBox(state, x, y, z, 0.3125f, 0.0f, 0.6875f, 0.6875f, 1.0f, 1.0f,
                        true, true, false, true, true, true, false, chunk, cm);
            }
            if (w) {
                renderBox(state, x, y, z, 0.0f, 0.0f, 0.3125f, 0.3125f, 1.0f, 0.6875f,
                        true, true, true, true, false, true, false, chunk, cm);
            }
            if (e) {
                renderBox(state, x, y, z, 0.6875f, 0.0f, 0.3125f, 1.0f, 1.0f, 0.6875f,
                        true, true, true, true, true, false, false, chunk, cm);
            }
        };
    }

    public static boolean canHedgeConnectTo(World world, int x, int y, int z, Direction dir) {
        int dx = dir == Direction.EAST ? 1 : (dir == Direction.WEST ? -1 : 0);
        int dz = dir == Direction.SOUTH ? 1 : (dir == Direction.NORTH ? -1 : 0);
        int nx = x + dx;
        int ny = y;
        int nz = z + dz;

        Block nBlock = world.getBlock(nx, ny, nz);
        if (nBlock == null || nBlock.isAir()) return false;

        if (nBlock instanceof HedgeBlock || nBlock instanceof LeavesBlock) return true;

        Tag<Block> hedgesTag = TagRegistry.getBlockTag("veinstride:hedges");
        if (hedgesTag != null && hedgesTag.contains(nBlock)) return true;

        Tag<Block> leavesTag = TagRegistry.getBlockTag("veinstride:leaves");
        if (leavesTag != null && leavesTag.contains(nBlock)) return true;

        NamespacedKey key = Registries.BLOCKS.getKey(nBlock);
        String keyName = key != null ? key.getKey() : "";

        if (keyName.contains("hedge") || keyName.contains("leaves")) return true;

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

        if (nBlock instanceof PlantBlock || nBlock instanceof CarpetBlock || nBlock instanceof LayerBlock || nBlock instanceof PaneBlock || nBlock instanceof TorchBlock || nBlock instanceof FenceBlock) {
            return false;
        }

        if (nBlock.isSolid && !nBlock.isPassable) {
            return true;
        }

        return false;
    }

    public static boolean isValidSupport(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        if (below == null || below.isAir()) return false;
        if (below instanceof HedgeBlock) return false; // Cannot stack on top of another HedgeBlock!
        return below.isSolid && !below.isPassable;
    }

    public static BlockState updateHedgeConnections(World world, int x, int y, int z, BlockState state) {
        if (state == null) return state;

        boolean n = canHedgeConnectTo(world, x, y, z, Direction.NORTH);
        boolean e = canHedgeConnectTo(world, x, y, z, Direction.EAST);
        boolean s = canHedgeConnectTo(world, x, y, z, Direction.SOUTH);
        boolean w = canHedgeConnectTo(world, x, y, z, Direction.WEST);

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
        boxes.add(new AABB(new Vector3f(0.3125f, 0.0f, 0.3125f), new Vector3f(0.6875f, 1.0f, 0.6875f)));
        if (n) boxes.add(new AABB(new Vector3f(0.3125f, 0.0f, 0.0f), new Vector3f(0.6875f, 1.0f, 0.3125f)));
        if (s) boxes.add(new AABB(new Vector3f(0.3125f, 0.0f, 0.6875f), new Vector3f(0.6875f, 1.0f, 1.0f)));
        if (w) boxes.add(new AABB(new Vector3f(0.0f, 0.0f, 0.3125f), new Vector3f(0.3125f, 1.0f, 0.6875f)));
        if (e) boxes.add(new AABB(new Vector3f(0.6875f, 0.0f, 0.3125f), new Vector3f(1.0f, 1.0f, 0.6875f)));

        return boxes;
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        return getBoundingBoxes(state);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        if (!isValidSupport(world, hitPos.x, hitPos.y, hitPos.z)) {
            return null; // Reject placement if not supported by solid block or if placed on another HedgeBlock!
        }
        BlockState state = getDefaultState();
        return updateHedgeConnections(world, hitPos.x, hitPos.y, hitPos.z, state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        if (!isValidSupport(world, x, y, z)) {
            dropAsItem(world, x, y, z);
            world.setBlock(x, y, z, Registries.BLOCKS.get("veinstride:air"));
            return;
        }
        BlockState state = world.getBlockState(x, y, z);
        if (state != null && state.getBlock() == this) {
            BlockState newState = updateHedgeConnections(world, x, y, z, state);
            if (newState != state) {
                world.setBlockWithState(x, y, z, this, newState.getStateId(), true);
            }
        }
    }

    private void dropAsItem(World world, int x, int y, int z) {
        String lootPath = this.getLootTable();
        if (lootPath != null) {
            de.delautrer.game.loot.LootTable table = de.delautrer.game.loot.LootTableManager.load(lootPath);
            if (table != null) {
                java.util.List<de.delautrer.game.items.ItemStack> drops = table.generateLoot();
                for (de.delautrer.game.items.ItemStack stack : drops) {
                    org.joml.Vector3d dropPos = new org.joml.Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    Vector3f dropVel = new Vector3f(
                            (float) (Math.random() - 0.5) * 2.0f,
                            2.0f,
                            (float) (Math.random() - 0.5) * 2.0f);
                    de.delautrer.game.entity.ItemEntity entity = new de.delautrer.game.entity.ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }

    private boolean isHedgeCellOccupied(BlockState state, int gx, int gz) {
        if (gx < 0 || gx > 2 || gz < 0 || gz > 2) return false;
        if (gx == 1 && gz == 1) return true; // Center core
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

    private boolean isHedgeCellOccupied3D(BlockState state, int gx, int gy, int gz) {
        if (gy != 0) return false;
        return isHedgeCellOccupied(state, gx, gz);
    }

    @Override
    public float[] getHighlightVertices(BlockState state) {
        float[] xCoords = {0.0f, 0.3125f, 0.6875f, 1.0f};
        float[] yCoords = {0.0f, 1.0f};
        float[] zCoords = {0.0f, 0.3125f, 0.6875f, 1.0f};

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
        float[] xCoords = {0.0f, 0.3125f, 0.6875f, 1.0f};
        float[] yCoords = {0.0f, 1.0f};
        float[] zCoords = {0.0f, 0.3125f, 0.6875f, 1.0f};

        int nx = xCoords.length;
        int ny = yCoords.length;
        int nz = zCoords.length;

        List<Integer> inds = new ArrayList<>();

        // 1. Lines parallel to X axis
        for (int gz = 0; gz < nz; gz++) {
            for (int gy = 0; gy < ny; gy++) {
                for (int gx = 0; gx < nx - 1; gx++) {
                    boolean c00 = isHedgeCellOccupied3D(state, gx, gy - 1, gz - 1);
                    boolean c01 = isHedgeCellOccupied3D(state, gx, gy - 1, gz);
                    boolean c10 = isHedgeCellOccupied3D(state, gx, gy, gz - 1);
                    boolean c11 = isHedgeCellOccupied3D(state, gx, gy, gz);
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
                    boolean c00 = isHedgeCellOccupied3D(state, gx - 1, gy, gz - 1);
                    boolean c01 = isHedgeCellOccupied3D(state, gx - 1, gy, gz);
                    boolean c10 = isHedgeCellOccupied3D(state, gx, gy, gz - 1);
                    boolean c11 = isHedgeCellOccupied3D(state, gx, gy, gz);
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
                    boolean c00 = isHedgeCellOccupied3D(state, gx - 1, gy - 1, gz);
                    boolean c01 = isHedgeCellOccupied3D(state, gx - 1, gy, gz);
                    boolean c10 = isHedgeCellOccupied3D(state, gx, gy - 1, gz);
                    boolean c11 = isHedgeCellOccupied3D(state, gx, gy, gz);
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
