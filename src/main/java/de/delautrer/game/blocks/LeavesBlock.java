package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.BooleanProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.entity.player.Player;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class LeavesBlock extends CubeBlock {

    public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");

    public LeavesBlock() {
        super(true, true);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(PERSISTENT);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        return getDefaultState().with(PERSISTENT, true);
    }

    @Override
    protected float getColorTint() {
        return 0.65f;
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (this.getId() == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "oak_leaves").getId()) return true;
        return super.shouldRenderFaceAgainst(neighborBlock, myHeight, neighborHeight);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        super.generateMesh(x, y, z, chunk, cm);
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockProperties.BlockFace face) {
        if (this.getId() == neighborState.getBlock().getId()) return true;
        return super.shouldRenderFaceAgainstState(myState, neighborState, face);
    }

    @Override
    public int getOpacity(BlockState state) {
        return 1;
    }
}

