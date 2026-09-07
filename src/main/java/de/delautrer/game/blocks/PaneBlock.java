package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
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

public class PaneBlock extends CubeBlock {

    @SuppressWarnings("this-escape")
    public PaneBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false, isRaycastable=true
        setSoundMaterialName("glass");
        setHardness(0.3f);
        this.lootTable = null; // Glass panes do not drop when mined
        this.mesher = (state, x, y, z, chunk, cm) -> {
            boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
            boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
            boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
            boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

            // Thin edge UV range: first pixel row/column (0.0 to 0.0625)
            float eU0 = 0.0f, eV0 = 0.0f, eU1 = 0.0625f, eV1 = 0.0625f;
            float eSideU0 = 0.0f, eSideV0 = 0.0f, eSideU1 = 0.0625f, eSideV1 = 1.0f;

            boolean isEWPresent = e || w;
            boolean isNSPresent = n || s;

            // 1. Center Post (x: 0.4375..0.5625, z: 0.4375..0.5625)
            renderBox(state, x, y, z, 0.4375f, 0.0f, 0.4375f, 0.5625f, 1.0f, 0.5625f,
                    true, true, !n, !s, !e, !w, false, chunk, cm,
                    0, 0, 0, 0, 0, 0, false, false, false, false, false, false,
                    eU0, eV0, eU1, eV1, // Top edge
                    eU0, eV0, eU1, eV1, // Bot edge
                    (!n && isEWPresent) ? -1 : eSideU0, (!n && isEWPresent) ? -1 : eSideV0, (!n && isEWPresent) ? -1 : eSideU1, (!n && isEWPresent) ? -1 : eSideV1, // North
                    (!s && isEWPresent) ? -1 : eSideU0, (!s && isEWPresent) ? -1 : eSideV0, (!s && isEWPresent) ? -1 : eSideU1, (!s && isEWPresent) ? -1 : eSideV1, // South
                    (!e && isNSPresent) ? -1 : eSideU0, (!e && isNSPresent) ? -1 : eSideV0, (!e && isNSPresent) ? -1 : eSideU1, (!e && isNSPresent) ? -1 : eSideV1, // East
                    (!w && isNSPresent) ? -1 : eSideU0, (!w && isNSPresent) ? -1 : eSideV0, (!w && isNSPresent) ? -1 : eSideU1, (!w && isNSPresent) ? -1 : eSideV1  // West
            );

            // 2. North Arm (x: 0.4375..0.5625, z: 0.0..0.4375)
            if (n) {
                renderBox(state, x, y, z, 0.4375f, 0.0f, 0.0f, 0.5625f, 1.0f, 0.4375f,
                        true, true, true, false, true, true, false, chunk, cm,
                        0, 0, 0, 0, 0, 0, false, false, false, false, false, false,
                        eU0, eV0, eU1, eV1, // Top
                        eU0, eV0, eU1, eV1, // Bot
                        eSideU0, eSideV0, eSideU1, eSideV1, // North end edge
                        -1, -1, -1, -1, // South (internal connection)
                        -1, -1, -1, -1, // East (wide face)
                        -1, -1, -1, -1  // West (wide face)
                );
            }

            // 3. South Arm (x: 0.4375..0.5625, z: 0.5625..1.0)
            if (s) {
                renderBox(state, x, y, z, 0.4375f, 0.0f, 0.5625f, 0.5625f, 1.0f, 1.0f,
                        true, true, false, true, true, true, false, chunk, cm,
                        0, 0, 0, 0, 0, 0, false, false, false, false, false, false,
                        eU0, eV0, eU1, eV1, // Top
                        eU0, eV0, eU1, eV1, // Bot
                        -1, -1, -1, -1, // North (internal connection)
                        eSideU0, eSideV0, eSideU1, eSideV1, // South end edge
                        -1, -1, -1, -1, // East (wide face)
                        -1, -1, -1, -1  // West (wide face)
                );
            }

            // 4. West Arm (x: 0.0..0.4375, z: 0.4375..0.5625)
            if (w) {
                renderBox(state, x, y, z, 0.0f, 0.0f, 0.4375f, 0.4375f, 1.0f, 0.5625f,
                        true, true, true, true, false, true, false, chunk, cm,
                        0, 0, 0, 0, 0, 0, false, false, false, false, false, false,
                        eU0, eV0, eU1, eV1, // Top
                        eU0, eV0, eU1, eV1, // Bot
                        -1, -1, -1, -1, // North (wide face)
                        -1, -1, -1, -1, // South (wide face)
                        -1, -1, -1, -1, // East (internal connection)
                        eSideU0, eSideV0, eSideU1, eSideV1 // West end edge
                );
            }

            // 5. East Arm (x: 0.5625..1.0, z: 0.4375..0.5625)
            if (e) {
                renderBox(state, x, y, z, 0.5625f, 0.0f, 0.4375f, 1.0f, 1.0f, 0.5625f,
                        true, true, true, true, true, false, false, chunk, cm,
                        0, 0, 0, 0, 0, 0, false, false, false, false, false, false,
                        eU0, eV0, eU1, eV1, // Top
                        eU0, eV0, eU1, eV1, // Bot
                        -1, -1, -1, -1, // North (wide face)
                        -1, -1, -1, -1, // South (wide face)
                        eSideU0, eSideV0, eSideU1, eSideV1, // East end edge
                        -1, -1, -1, -1  // West (internal connection)
                );
            }
        };
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        Block nBlock = neighborState.getBlock();
        if (nBlock == null || nBlock.isAir()) return true;

        // Render faces against other PaneBlocks so stacked panes with differing shapes
        // or top/bottom edges render cleanly without see-through holes.
        if (nBlock instanceof PaneBlock) return true;

        return nBlock.isTransparent;
    }

    public static boolean canPaneConnectTo(World world, int x, int y, int z, Direction dir) {
        int dx = dir == Direction.EAST ? 1 : (dir == Direction.WEST ? -1 : 0);
        int dz = dir == Direction.SOUTH ? 1 : (dir == Direction.NORTH ? -1 : 0);
        int nx = x + dx;
        int ny = y;
        int nz = z + dz;

        Block nBlock = world.getBlock(nx, ny, nz);
        if (nBlock == null || nBlock.isAir()) return false;

        if (nBlock instanceof PaneBlock) return true;

        Tag<Block> panesTag = TagRegistry.getBlockTag("veinstride:panes");
        if (panesTag != null && panesTag.contains(nBlock)) return true;

        Tag<Block> connectableTag = TagRegistry.getBlockTag("veinstride:pane_connectable");
        if (connectableTag != null && connectableTag.contains(nBlock)) return true;

        NamespacedKey key = Registries.BLOCKS.getKey(nBlock);
        String keyName = key != null ? key.getKey() : "";

        if (keyName.contains("glass") || nBlock instanceof LeavesBlock) return true;

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

        if (nBlock instanceof PlantBlock || nBlock instanceof CarpetBlock || nBlock instanceof LayerBlock || nBlock instanceof TorchBlock) {
            return false;
        }

        if (nBlock.isSolid) {
            return true;
        }

        return false;
    }

    public static BlockState updatePaneConnections(World world, int x, int y, int z, BlockState state) {
        if (state == null) return state;

        boolean n = canPaneConnectTo(world, x, y, z, Direction.NORTH);
        boolean e = canPaneConnectTo(world, x, y, z, Direction.EAST);
        boolean s = canPaneConnectTo(world, x, y, z, Direction.SOUTH);
        boolean w = canPaneConnectTo(world, x, y, z, Direction.WEST);

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
        return getCollisionBoxes(state);
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
        boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
        boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
        boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);

        List<AABB> boxes = new ArrayList<>();
        // Center post
        boxes.add(new AABB(new Vector3f(0.4375f, 0.0f, 0.4375f), new Vector3f(0.5625f, 1.0f, 0.5625f)));
        if (n) boxes.add(new AABB(new Vector3f(0.4375f, 0.0f, 0.0f), new Vector3f(0.5625f, 1.0f, 0.4375f)));
        if (s) boxes.add(new AABB(new Vector3f(0.4375f, 0.0f, 0.5625f), new Vector3f(0.5625f, 1.0f, 1.0f)));
        if (w) boxes.add(new AABB(new Vector3f(0.0f, 0.0f, 0.4375f), new Vector3f(0.4375f, 1.0f, 0.5625f)));
        if (e) boxes.add(new AABB(new Vector3f(0.5625f, 0.0f, 0.4375f), new Vector3f(1.0f, 1.0f, 0.5625f)));

        return boxes;
    }

    private boolean isCellOccupied(BlockState state, int cx, int cz) {
        if (cx < 0 || cx > 2 || cz < 0 || cz > 2) return false;
        if (cx == 1 && cz == 1) return true; // Center post is always occupied
        boolean n = state.contains(ConnectedState.NORTH) && state.getValue(ConnectedState.NORTH);
        boolean e = state.contains(ConnectedState.EAST) && state.getValue(ConnectedState.EAST);
        boolean s = state.contains(ConnectedState.SOUTH) && state.getValue(ConnectedState.SOUTH);
        boolean w = state.contains(ConnectedState.WEST) && state.getValue(ConnectedState.WEST);
        if (cx == 1 && cz == 0) return n;
        if (cx == 1 && cz == 2) return s;
        if (cx == 0 && cz == 1) return w;
        if (cx == 2 && cz == 1) return e;
        return false;
    }

    @Override
    public float[] getHighlightVertices(BlockState state) {
        float[] xCoords = {0.0f, 0.4375f, 0.5625f, 1.0f};
        float[] yCoords = {0.0f, 1.0f};
        float[] zCoords = {0.0f, 0.4375f, 0.5625f, 1.0f};

        float[] verts = new float[4 * 2 * 4 * 3];
        int idx = 0;
        for (int gz = 0; gz < 4; gz++) {
            for (int gy = 0; gy < 2; gy++) {
                for (int gx = 0; gx < 4; gx++) {
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
        List<Integer> inds = new ArrayList<>();

        // 1. Lines parallel to X axis (gx to gx+1)
        for (int gz = 0; gz < 4; gz++) {
            for (int gy = 0; gy < 2; gy++) {
                for (int gx = 0; gx < 3; gx++) {
                    boolean cellBelow = isCellOccupied(state, gx, gz - 1);
                    boolean cellAbove = isCellOccupied(state, gx, gz);
                    if (cellBelow != cellAbove) {
                        int v0 = gx + gy * 4 + gz * 8;
                        int v1 = (gx + 1) + gy * 4 + gz * 8;
                        inds.add(v0);
                        inds.add(v1);
                    }
                }
            }
        }

        // 2. Lines parallel to Z axis (gz to gz+1)
        for (int gx = 0; gx < 4; gx++) {
            for (int gy = 0; gy < 2; gy++) {
                for (int gz = 0; gz < 3; gz++) {
                    boolean cellLeft = isCellOccupied(state, gx - 1, gz);
                    boolean cellRight = isCellOccupied(state, gx, gz);
                    if (cellLeft != cellRight) {
                        int v0 = gx + gy * 4 + gz * 8;
                        int v1 = gx + gy * 4 + (gz + 1) * 8;
                        inds.add(v0);
                        inds.add(v1);
                    }
                }
            }
        }

        // 3. Lines parallel to Y axis (gy=0 to gy=1)
        for (int gz = 0; gz < 4; gz++) {
            for (int gx = 0; gx < 4; gx++) {
                boolean q00 = isCellOccupied(state, gx - 1, gz - 1);
                boolean q01 = isCellOccupied(state, gx - 1, gz);
                boolean q10 = isCellOccupied(state, gx, gz - 1);
                boolean q11 = isCellOccupied(state, gx, gz);

                int count = (q00 ? 1 : 0) + (q01 ? 1 : 0) + (q10 ? 1 : 0) + (q11 ? 1 : 0);
                boolean drawLine = false;
                if (count == 1 || count == 3) {
                    drawLine = true;
                } else if (count == 2) {
                    drawLine = (q00 == q11);
                }

                if (drawLine) {
                    int v0 = gx + 0 * 4 + gz * 8;
                    int v1 = gx + 1 * 4 + gz * 8;
                    inds.add(v0);
                    inds.add(v1);
                }
            }
        }

        int[] result = new int[inds.size()];
        for (int i = 0; i < inds.size(); i++) {
            result[i] = inds.get(i);
        }
        return result;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        BlockState state = getDefaultState();
        return updatePaneConnections(world, hitPos.x, hitPos.y, hitPos.z, state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        BlockState state = world.getBlockState(x, y, z);
        if (state != null && state.getBlock() == this) {
            BlockState newState = updatePaneConnections(world, x, y, z, state);
            if (newState != state) {
                world.setBlockWithState(x, y, z, this, newState.getStateId(), true);
            }
        }
    }
}
