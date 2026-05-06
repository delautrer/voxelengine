package de.delautrer.game.blocks;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.game.blocks.models.BlockModelData;
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

    public LogBlock() {
        super(true, false);
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
    public AtlasRegion getTextureForFace(BlockState state, BlockFace face) {
        BlockModelData model = getModel();
        if (model == null) return null;

        AtlasRegion texEnd = model.top;
        AtlasRegion texBark = model.north;

        Axis axis = state.getValue(AXIS);

        if (axis == Axis.Y) return (face == BlockFace.UP || face == BlockFace.DOWN) ? texEnd : texBark;
        if (axis == Axis.X) return (face == BlockFace.EAST || face == BlockFace.WEST) ? texEnd : texBark;
        if (axis == Axis.Z) return (face == BlockFace.NORTH || face == BlockFace.SOUTH) ? texEnd : texBark;

        return texBark;
    }
}
