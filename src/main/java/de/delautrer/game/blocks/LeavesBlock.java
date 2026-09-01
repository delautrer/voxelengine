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
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

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
        if (this == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "oak_leaves")) return true;
        return super.shouldRenderFaceAgainst(neighborBlock, myHeight, neighborHeight);
    }



    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockProperties.BlockFace face) {
        if (this == neighborState.getBlock()) return true;
        return super.shouldRenderFaceAgainstState(myState, neighborState, face);
    }

    @Override
    public int getOpacity(BlockState state) {
        return 1;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
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
            world.setBlock(x, y, z, de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air"));
        }
    }

    private boolean isLogNearby(World world, int startX, int startY, int startZ, int maxDistance) {
        Queue<Vector3i> queue = new LinkedList<>();
        Set<Vector3i> visited = new HashSet<>();
        
        Vector3i start = new Vector3i(startX, startY, startZ);
        queue.add(start);
        visited.add(start);
        
        int currentDistance = 0;
        int nodesInCurrentLevel = 1;
        int nodesInNextLevel = 0;
        
        int[][] dirs = {{0,1,0},{0,-1,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        
        while (!queue.isEmpty() && currentDistance <= maxDistance) {
            Vector3i current = queue.poll();
            nodesInCurrentLevel--;
            
            Block b = world.getBlockState(current.x, current.y, current.z).getBlock();
            if (b instanceof LogBlock) {
                return true;
            }
            
            // Allow tracing through the start block and any leaves block.
            if (current.equals(start) || b instanceof LeavesBlock) {
                for (int[] dir : dirs) {
                    Vector3i next = new Vector3i(current.x + dir[0], current.y + dir[1], current.z + dir[2]);
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                        nodesInNextLevel++;
                    }
                }
            }
            
            if (nodesInCurrentLevel == 0) {
                currentDistance++;
                nodesInCurrentLevel = nodesInNextLevel;
                nodesInNextLevel = 0;
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
