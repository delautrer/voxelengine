package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.JigsawBlockEntity;
import de.delautrer.game.blocks.models.BlockModelData;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.inventory.JigsawInventory;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class JigsawBlock extends CubeBlock implements IInteractable {

    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    @SuppressWarnings("this-escape")
    public JigsawBlock() {
        super(true, false);
        setHardness(-1.0f);
        setLootTable(null);
        this.mesher = new de.delautrer.engine.graphics.meshing.JigsawMesher(this);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        float pitch = ((LocalPlayer) player).getCamera().getPitch();
        float yaw = ((LocalPlayer) player).getCamera().getYaw();
        yaw = (yaw % 360 + 360) % 360;

        Direction facing;
        if (pitch < -45.0f) {
            facing = Direction.UP;
        } else if (pitch > 45.0f) {
            facing = Direction.DOWN;
        } else if (yaw >= 45 && yaw < 135) {
            facing = Direction.NORTH;
        } else if (yaw >= 135 && yaw < 225) {
            facing = Direction.EAST;
        } else if (yaw >= 225 && yaw < 315) {
            facing = Direction.SOUTH;
        } else {
            facing = Direction.WEST;
        }

        return getDefaultState().with(FACING, facing);
    }

    @Override
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, Player player) {
        super.onBlockPlaced(world, pos, state, player);
        if (world != null && pos != null && state != null && state.contains(FACING)) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof JigsawBlockEntity jbe) {
                jbe.setOrientation(state.getValue(FACING).name().toLowerCase());
            }
        }
    }

    @Override
    public AtlasRegion getTextureForFace(BlockState state, BlockFace face) {
        BlockModelData model = getModel();
        if (model == null) return null;

        Direction facing = (state != null && state.contains(FACING)) ? state.getValue(FACING) : Direction.NORTH;
        BlockFace frontFace = toBlockFace(facing);
        BlockFace backFace = toBlockFace(facing.getOpposite());

        if (face == frontFace || face == backFace || face == BlockFace.UP || face == BlockFace.DOWN) {
            return model.top;
        }
        return model.north;
    }

    private static BlockFace toBlockFace(Direction dir) {
        return switch (dir) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case EAST  -> BlockFace.EAST;
            case WEST  -> BlockFace.WEST;
            case UP    -> BlockFace.UP;
            case DOWN  -> BlockFace.DOWN;
        };
    }

    @Override
    public boolean hasBlockEntity() {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(World world, Vector3i pos) {
        return new JigsawBlockEntity(world, pos);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof JigsawBlockEntity jbe)) {
            return false;
        }

        if (player != null && player.isSneaking()) {
            BlockState currentState = world.getBlockState(pos);
            if (currentState != null && currentState.contains(FACING)) {
                Direction currentFacing = currentState.getValue(FACING);
                Direction newFacing;
                switch (currentFacing) {
                    case NORTH -> newFacing = Direction.EAST;
                    case EAST  -> newFacing = Direction.SOUTH;
                    case SOUTH -> newFacing = Direction.WEST;
                    case WEST  -> newFacing = Direction.NORTH;
                    default    -> newFacing = Direction.NORTH;
                }

                BlockState newState = currentState.with(FACING, newFacing);
                world.setBlockState(pos.x, pos.y, pos.z, newState);
                jbe.setOrientation(newFacing.name().toLowerCase());
                return true;
            }
        }

        player.openInventory(new JigsawInventory(jbe));
        return true;
    }
}
