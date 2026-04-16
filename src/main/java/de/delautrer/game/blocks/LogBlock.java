package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class LogBlock extends CubeBlock {
    public static final EnumProperty<Axis> AXIS = EnumProperty.create("axis", Axis.class);

    public LogBlock(int texTop, int texSide) {
        super(true, false, texTop, texSide, texTop);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        Axis axis = Axis.Y;
        if (hitFace.x != 0) axis = Axis.X;
        else if (hitFace.z != 0) axis = Axis.Z;
        return getDefaultState().with(AXIS, axis);
    }

    @Override
    public int getTextureForFace(BlockState state, BlockFace face) {
        Axis axis = state.getValue(AXIS);
        if (axis == Axis.Y) return (face == BlockFace.UP || face == BlockFace.DOWN) ? texTop : texSide;
        if (axis == Axis.X) return (face == BlockFace.EAST || face == BlockFace.WEST) ? texTop : texSide;
        if (axis == Axis.Z) return (face == BlockFace.NORTH || face == BlockFace.SOUTH) ? texTop : texSide;
        return texSide;
    }
}