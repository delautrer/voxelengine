package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.BooleanProperty;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;

public class DoorBlock extends CubeBlock implements IInteractable {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<DoorHinge> HINGE = EnumProperty.create("hinge", DoorHinge.class);
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);

    @SuppressWarnings("this-escape")
    public DoorBlock() {
        super(false, true); // Nicht voll-solid, transparent
        setSoundMaterialName("wood");
        setHardness(2.0f);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return false;
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(FACING);
        properties.add(OPEN);
        properties.add(HINGE);
        properties.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        if (hitPos.y >= Chunk.HEIGHT - 1) return null;
        if (world.getBlockAt(hitPos.x, hitPos.y + 1, hitPos.z) != 0) return null;

        float yaw = ((LocalPlayer) player).getCamera().getYaw();
        yaw = (yaw % 360 + 360) % 360;
        Direction facing;
        if (yaw >= 45 && yaw < 135) facing = Direction.SOUTH;
        else if (yaw >= 135 && yaw < 225) facing = Direction.WEST;
        else if (yaw >= 225 && yaw < 315) facing = Direction.NORTH;
        else facing = Direction.EAST;

        DoorHinge hinge = DoorHinge.LEFT;
        Vector3i leftPos = getSidePos(hitPos, facing, false);
        BlockState leftState = world.getBlockState(leftPos.x, leftPos.y, leftPos.z);
        if (leftState.getBlock() == this && leftState.getValue(FACING) == facing) {
            hinge = DoorHinge.RIGHT;
        } else {
            Vector3i rightPos = getSidePos(hitPos, facing, true);
            BlockState rightState = world.getBlockState(rightPos.x, rightPos.y, rightPos.z);
            if (rightState.getBlock() == this && rightState.getValue(FACING) == facing) {
                hinge = DoorHinge.LEFT;
            }
        }

        return getDefaultState().with(FACING, facing).with(OPEN, false).with(HINGE, hinge).with(HALF, Half.BOTTOM);
    }

    @Override
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, Player player) {
        if (state.getValue(HALF) == Half.BOTTOM) {
            world.setBlockState(pos.x, pos.y + 1, pos.z, state.with(HALF, Half.TOP));
        }
    }

    @Override
    public void onBlockRemoved(World world, Vector3i pos, BlockState state) {
        if (state.getValue(HALF) == Half.BOTTOM) {
            Vector3i topPos = new Vector3i(pos.x, pos.y + 1, pos.z);
            BlockState topState = world.getBlockState(topPos.x, topPos.y, topPos.z);
            if (topState.getBlock() == this && topState.getValue(HALF) == Half.TOP) {
                world.setBlock(topPos.x, topPos.y, topPos.z, (byte) 0);
            }
        } else {
            Vector3i botPos = new Vector3i(pos.x, pos.y - 1, pos.z);
            BlockState botState = world.getBlockState(botPos.x, botPos.y, botPos.z);
            if (botState.getBlock() == this && botState.getValue(HALF) == Half.BOTTOM) {
                world.setBlock(botPos.x, botPos.y, botPos.z, (byte) 0);
            }
        }
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        if (state.getBlock() != this) return false;
        
        boolean newOpen = !state.getValue(OPEN);
        toggleDoor(world, pos, state, newOpen);
        
        Vector3i sisterPos = getSisterPos(pos, state);
        BlockState sisterState = world.getBlockState(sisterPos.x, sisterPos.y, sisterPos.z);
        if (sisterState.getBlock() == this && 
            sisterState.getValue(FACING) == state.getValue(FACING) && 
            sisterState.getValue(HINGE) != state.getValue(HINGE)) {
            toggleDoor(world, sisterPos, sisterState, newOpen);
        }
        
        return true;
    }

    private void toggleDoor(World world, Vector3i pos, BlockState state, boolean open) {
        int otherY = state.getValue(HALF) == Half.BOTTOM ? pos.y + 1 : pos.y - 1;
        world.setBlockState(pos.x, pos.y, pos.z, state.with(OPEN, open));
        
        BlockState otherState = world.getBlockState(pos.x, otherY, pos.z);
        if (otherState.getBlock() == this && otherState.getValue(HALF) != state.getValue(HALF)) {
            world.setBlockState(pos.x, otherY, pos.z, otherState.with(OPEN, open));
        }
    }

    private Vector3i getSisterPos(Vector3i pos, BlockState state) {
        return getSidePos(pos, state.getValue(FACING), state.getValue(HINGE) == DoorHinge.LEFT);
    }

    private Vector3i getSidePos(Vector3i pos, Direction facing, boolean right) {
        int dx = 0, dz = 0;
        if (facing == Direction.NORTH) dx = right ? 1 : -1;
        else if (facing == Direction.SOUTH) dx = right ? -1 : 1;
        else if (facing == Direction.EAST) dz = right ? 1 : -1;
        else if (facing == Direction.WEST) dz = right ? -1 : 1;
        return new Vector3i(pos.x + dx, pos.y, pos.z + dz);
    }

    @Override
    public List<AABB> getHighlightBoxes(BlockState state) {
        // Da der Raycaster jetzt immer die UNTERE Position für Türen zurückgibt (siehe Raycaster.java),
        // können wir hier immer eine 2-Block hohe Box ab dem Nullpunkt zurückgeben.
        List<AABB> currentBoxes = getBoundingBoxes(state);
        if (currentBoxes.isEmpty()) return currentBoxes;
        
        AABB local = currentBoxes.get(0);
        
        // Immer von 0 bis 2 (da hitPos vom Raycaster auf die untere Hälfte korrigiert wird)
        float minY = 0.0f;
        float maxY = 2.0f;
        
        float e = 0.002f;
        return List.of(new AABB(
            new Vector3f(local.min.x - e, minY - e, local.min.z - e), 
            new Vector3f(local.max.x + e, maxY + e, local.max.z + e)
        ));
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        List<AABB> boxes = getBoundingBoxes(state);
        List<AABB> collisionBoxes = new ArrayList<>();
        float thickness = 0.12f; 
        
        for (AABB b : boxes) {
            Vector3f min = new Vector3f(b.min);
            Vector3f max = new Vector3f(b.max);
            
            if (max.x - min.x < 0.9f) {
                float center = (min.x + max.x) / 2f;
                min.x = center - (thickness / 2f);
                max.x = center + (thickness / 2f);
            }
            if (max.z - min.z < 0.9f) {
                float center = (min.z + max.z) / 2f;
                min.z = center - (thickness / 2f);
                max.z = center + (thickness / 2f);
            }
            collisionBoxes.add(new AABB(min, max));
        }
        return collisionBoxes;
    }

    @Override
    public AtlasRegion getTextureForFace(BlockState state, BlockFace face) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        Half half = state.getValue(HALF);
        
        boolean isWideFace = false;
        if (!open) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                isWideFace = (face == BlockFace.NORTH || face == BlockFace.SOUTH);
            } else {
                isWideFace = (face == BlockFace.EAST || face == BlockFace.WEST);
            }
        } else {
            // Wenn offen, sind die weiten Seiten um 90 Grad gedreht (perpendikular zum originalen facing)
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                isWideFace = (face == BlockFace.EAST || face == BlockFace.WEST);
            } else {
                isWideFace = (face == BlockFace.NORTH || face == BlockFace.SOUTH);
            }
        }

        if (isWideFace) {
            AtlasRegion tex = (half == Half.BOTTOM ? getModel().side_bottom : getModel().side_top);
            if (tex != null) return tex;
        }
        
        return super.getTextureForFace(state, face);
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        DoorHinge hinge = state.getValue(HINGE);
        float t = 0.1875f;

        if (!open) {
            if (facing == Direction.NORTH) return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, t)));
            if (facing == Direction.SOUTH) return List.of(new AABB(new Vector3f(0, 0, 1 - t), new Vector3f(1, 1, 1)));
            if (facing == Direction.WEST)  return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(t, 1, 1)));
            return List.of(new AABB(new Vector3f(1 - t, 0, 0), new Vector3f(1, 1, 1)));
        } else {
            if (facing == Direction.NORTH) {
                return List.of(hinge == DoorHinge.LEFT 
                    ? new AABB(new Vector3f(0, 0, 0), new Vector3f(t, 1, 1)) 
                    : new AABB(new Vector3f(1 - t, 0, 0), new Vector3f(1, 1, 1)));
            }
            if (facing == Direction.SOUTH) {
                return List.of(hinge == DoorHinge.LEFT 
                    ? new AABB(new Vector3f(1 - t, 0, 0), new Vector3f(1, 1, 1)) 
                    : new AABB(new Vector3f(0, 0, 0), new Vector3f(t, 1, 1)));
            }
            if (facing == Direction.WEST) {
                return List.of(hinge == DoorHinge.LEFT 
                    ? new AABB(new Vector3f(0, 0, 1 - t), new Vector3f(1, 1, 1)) 
                    : new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, t)));
            }
            if (facing == Direction.EAST) {
                return List.of(hinge == DoorHinge.LEFT 
                    ? new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, t)) 
                    : new AABB(new Vector3f(0, 0, 1 - t), new Vector3f(1, 1, 1)));
            }
        }
        return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)));
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        return true; 
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        BlockState state = chunk.getBlockState(x, y, z);
        AABB b = getBoundingBoxes(state).get(0);
        renderBox(state, x, y, z, b.min.x, b.min.y, b.min.z, b.max.x, b.max.y, b.max.z, true, true, true, true, true, true, false, chunk, cm);
    }
}
