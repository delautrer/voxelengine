package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.events.InventoryOpenedEvent;

import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class ChestBlock extends CubeBlock implements IInteractable {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public ChestBlock() {
        super(true, true);
        this.mesher = new de.delautrer.engine.graphics.meshing.ChestMesher(this);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace,
            Vector3f exactHit) {
        float yaw = ((LocalPlayer) player).getCamera().getYaw();
        yaw = (yaw % 360 + 360) % 360;
        Direction facing;
        if (yaw >= 45 && yaw < 135)
            facing = Direction.NORTH;
        else if (yaw >= 135 && yaw < 225)
            facing = Direction.EAST;
        else if (yaw >= 225 && yaw < 315)
            facing = Direction.SOUTH;
        else
            facing = Direction.WEST;

        return getDefaultState().with(FACING, facing);
    }

    @Override
    public de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion getTextureForFace(BlockState state,
            BlockFace face) {
        de.delautrer.game.blocks.models.BlockModelData model = getModel();
        if (model == null)
            return null;

        if (face == BlockFace.UP)
            return model.top;
        if (face == BlockFace.DOWN)
            return model.bottom;

        Direction facing = state.getValue(FACING);

        // Map the world face to a model face based on block rotation
        // We assume the 'front' of the chest model is model.north
        BlockFace relativeFace = face;

        if (facing == Direction.NORTH) {
            // Default
        } else if (facing == Direction.SOUTH) {
            if (face == BlockFace.NORTH) relativeFace = BlockFace.SOUTH;
            else if (face == BlockFace.SOUTH) relativeFace = BlockFace.NORTH;
            else if (face == BlockFace.EAST) relativeFace = BlockFace.WEST;
            else if (face == BlockFace.WEST) relativeFace = BlockFace.EAST;
        } else if (facing == Direction.EAST) {
            if (face == BlockFace.NORTH) relativeFace = BlockFace.WEST;
            else if (face == BlockFace.SOUTH) relativeFace = BlockFace.EAST;
            else if (face == BlockFace.EAST) relativeFace = BlockFace.NORTH;
            else if (face == BlockFace.WEST) relativeFace = BlockFace.SOUTH;
        } else if (facing == Direction.WEST) {
            if (face == BlockFace.NORTH) relativeFace = BlockFace.EAST;
            else if (face == BlockFace.SOUTH) relativeFace = BlockFace.WEST;
            else if (face == BlockFace.EAST) relativeFace = BlockFace.SOUTH;
            else if (face == BlockFace.WEST) relativeFace = BlockFace.NORTH;
        }

        if (relativeFace == BlockFace.NORTH) return model.north;
        if (relativeFace == BlockFace.SOUTH) return model.south;
        if (relativeFace == BlockFace.EAST) return model.east;
        return model.west;
    }

    @Override
    public boolean hasBlockEntity() {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(World world, Vector3i pos) {
        return new ChestBlockEntity(world, pos);
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        float offset = 1.0f / 16.0f;
        return List.of(new AABB(new Vector3f(offset, 0.0f, offset), new Vector3f(1.0f - offset, 14.0f / 16.0f, 1.0f - offset)));
    }



    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        // Immer rendern, da die Kiste kleiner als ein voller Block ist (14x14x14)
        return true;
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof ChestBlockEntity chestEntity) {
            player.openInventory(chestEntity.getInventory());
            player.getInteraction().getEventBus().publish(new InventoryOpenedEvent(player, chestEntity.getInventory()));
            return true;
        }
        return false;
    }
}
