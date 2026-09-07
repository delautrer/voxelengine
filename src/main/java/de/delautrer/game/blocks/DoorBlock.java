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
import de.delautrer.game.world.World;
import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;

public class DoorBlock extends CubeBlock implements IInteractable, IPairedBlock {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<DoorHinge> HINGE = EnumProperty.create("hinge", DoorHinge.class);
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);

    @SuppressWarnings("this-escape")
    public DoorBlock() {
        super(false, true); // Nicht voll-solid, transparent
        setSoundMaterialName("wood");
        setHardness(2.0f);
        this.mesher = new de.delautrer.engine.graphics.meshing.DoorMesher(this);
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
        if (hitPos.y >= Chunk.MAX_Y - 1) return null;
        if (!world.getBlock(hitPos.x, hitPos.y + 1, hitPos.z).isAir()) return null;

        Block blockBelow = world.getBlock(hitPos.x, hitPos.y - 1, hitPos.z);
        if (blockBelow == null || !blockBelow.isSolid || blockBelow.isPassable) return null;

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
            PairedBlocks.placePair(world, pos, state);
        }
    }

    @Override
    public void onBlockRemoved(World world, Vector3i pos, BlockState state) {
        PairedBlocks.breakPair(world, pos, state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block neighborBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, neighborBlock);
        PairedBlocks.validateOrDrop(world, x, y, z, world.getBlockState(x, y, z));
    }

    @Override
    public Vector3i getPartnerPos(Vector3i pos, BlockState state) {
        if (state.getValue(HALF) == Half.BOTTOM) {
            return new Vector3i(pos.x, pos.y + 1, pos.z);
        } else {
            return new Vector3i(pos.x, pos.y - 1, pos.z);
        }
    }

    @Override
    public Vector3i getPrimaryPos(Vector3i pos, BlockState state) {
        if (state.getValue(HALF) == Half.TOP) {
            return new Vector3i(pos.x, pos.y - 1, pos.z);
        }
        return new Vector3i(pos);
    }

    @Override
    public boolean isValidPartner(BlockState self, BlockState other) {
        if (self == null || other == null) return false;
        if (self.getBlock() != other.getBlock()) return false;
        if (self.getValue(HALF) == other.getValue(HALF)) return false;
        return self.getValue(FACING) == other.getValue(FACING) &&
               self.getValue(HINGE) == other.getValue(HINGE) &&
               self.getValue(OPEN) == other.getValue(OPEN);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        if (state.getBlock() != this) return false;
        
        if (!PairedBlocks.validateOrDrop(world, pos.x, pos.y, pos.z, state)) {
            return false;
        }

        boolean newOpen = !state.getValue(OPEN);
        toggleDoor(world, pos, state, newOpen);
        
        Vector3i sisterPos = getSisterPos(pos, state);
        BlockState sisterState = world.getBlockState(sisterPos.x, sisterPos.y, sisterPos.z);
        if (sisterState.getBlock() == this && 
            sisterState.getValue(FACING) == state.getValue(FACING) && 
            sisterState.getValue(HINGE) != state.getValue(HINGE)) {
            if (PairedBlocks.validateOrDrop(world, sisterPos.x, sisterPos.y, sisterPos.z, sisterState)) {
                toggleDoor(world, sisterPos, sisterState, newOpen);
            }
        }
        
        return true;
    }

    private void toggleDoor(World world, Vector3i pos, BlockState state, boolean open) {
        Vector3i partnerPos = getPartnerPos(pos, state);
        BlockState partnerState = world.getBlockState(partnerPos.x, partnerPos.y, partnerPos.z);
        if (isValidPartner(state, partnerState)) {
            BlockState newSelf = state.with(OPEN, open);
            BlockState newPartner = partnerState.with(OPEN, open);

            world.setBlockWithState(pos.x, pos.y, pos.z, this, newSelf.getStateId(), true, false);
            world.setBlockWithState(partnerPos.x, partnerPos.y, partnerPos.z, this, newPartner.getStateId(), false, false);

            notifyCellChanged(world, pos, this);
            notifyCellChanged(world, partnerPos, this);
        }
    }

    private void notifyCellChanged(World world, Vector3i pos, Block newBlock) {
        if (world.getEventBus() != null) {
            int[][] dirs = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            for (int[] dir : dirs) {
                Vector3i nPos = new Vector3i(pos.x + dir[0], pos.y + dir[1], pos.z + dir[2]);
                world.getEventBus().publish(new de.delautrer.game.events.BlockNeighborUpdateEvent(nPos, pos, newBlock));
            }
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
        List<AABB> currentBoxes = getBoundingBoxes(state);
        if (currentBoxes.isEmpty()) return currentBoxes;
        
        AABB local = currentBoxes.get(0);
        
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
}
