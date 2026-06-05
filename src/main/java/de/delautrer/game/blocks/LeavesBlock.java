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
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.entity.ItemEntity;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import java.util.Random;

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

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        BlockState state = world.getBlockState(x, y, z);
        if (state.getValue(PERSISTENT)) return;

        // Schedule decay check with a small random delay (2 to 6 ticks)
        int delay = 2 + new Random().nextInt(5);
        world.getTickScheduler().scheduleTick(new Vector3i(x, y, z), this, delay);
    }

    @Override
    public void scheduledTick(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(x, y, z);
        if (state.getBlock() != this || state.getValue(PERSISTENT)) return;

        if (!isLogNearby(world, x, y, z, 6)) {
            dropBlockAsItem(world, x, y, z, state);
            world.setBlock(x, y, z, (byte) 0);
        }
    }

    private boolean isLogNearby(World world, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius + 2) continue;

                    Block b = world.getBlockState(x + dx, y + dy, z + dz).getBlock();
                    if (b instanceof LogBlock) return true;
                }
            }
        }
        return false;
    }

    private void dropBlockAsItem(World world, int x, int y, int z, BlockState state) {
        Block block = state.getBlock();
        String lootPath = block.getLootTable();

        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();
                for (ItemStack stack : drops) {
                    Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    Vector3f dropVel = new Vector3f(
                            (float) (Math.random() - 0.5) * 1.5f,
                            1.5f,
                            (float) (Math.random() - 0.5) * 1.5f);
                    world.spawnEntity(new ItemEntity(stack, dropPos, dropVel));
                }
            }
        }
    }
}
