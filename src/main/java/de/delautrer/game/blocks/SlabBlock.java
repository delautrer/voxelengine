package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import de.delautrer.game.items.BlockItem;

public class SlabBlock extends CubeBlock {
    public static final EnumProperty<SlabType> TYPE = EnumProperty.create("type", SlabType.class);

    public SlabBlock(boolean isSolid, boolean isTransparent) {
        super(isSolid, isTransparent);
        this.mesher = new de.delautrer.engine.graphics.meshing.SlabMesher(this);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) { properties.add(TYPE); }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        if (item.block != this) return false;

        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE) return false;

        if (type == SlabType.BOTTOM && hitFace.y == 1) return true;
        if (type == SlabType.TOP && hitFace.y == -1) return true;

        return false;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        BlockState currentState = world.getBlockState(hitPos.x, hitPos.y, hitPos.z);
        if (currentState.getBlock() == this) return currentState.with(TYPE, SlabType.DOUBLE);
        if (hitFace.y == 1) return getDefaultState().with(TYPE, SlabType.BOTTOM);
        if (hitFace.y == -1) return getDefaultState().with(TYPE, SlabType.TOP);
        float yFrac = exactHit.y - (float)Math.floor(exactHit.y);
        return getDefaultState().with(TYPE, yFrac > 0.5f ? SlabType.TOP : SlabType.BOTTOM);
    }



    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE) return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)));

        float minY = (type == SlabType.TOP) ? 0.5f : 0.0f;
        float maxY = (type == SlabType.BOTTOM) ? 0.5f : 1.0f;
        return List.of(new AABB(new Vector3f(0, minY, 0), new Vector3f(1, maxY, 1)));
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        Block nBlock = neighborState.getBlock();
        if (nBlock == null || nBlock.isAir()) return true;

        if (nBlock == this) {
            SlabType myType = myState.getValue(TYPE);
            SlabType nType = neighborState.getValue(TYPE);

            if (myType == SlabType.DOUBLE && nType == SlabType.DOUBLE) return false;
            if (myType == SlabType.BOTTOM && nType == SlabType.BOTTOM && face != BlockFace.UP && face != BlockFace.DOWN) return false;
            if (myType == SlabType.TOP && nType == SlabType.TOP && face != BlockFace.UP && face != BlockFace.DOWN) return false;
            if (myType == SlabType.BOTTOM && face == BlockFace.UP && nType == SlabType.TOP) return false;
            if (myType == SlabType.TOP && face == BlockFace.DOWN && nType == SlabType.BOTTOM) return false;

            return true;
        }
        return nBlock.isTransparent;
    }
}
