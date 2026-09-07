package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class CarpetBlock extends CubeBlock {

    @SuppressWarnings("this-escape")
    public CarpetBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false (for 1px step-up), isRaycastable=true
        setSoundMaterialName("grass");
        setHardness(0.1f);
        this.mesher = (state, x, y, z, chunk, cm) -> {
            renderBox(state, x, y, z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f / 16.0f, 1.0f, true, true, true, true, true, true, false, chunk, cm);
        };
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return false;
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        return List.of(new AABB(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f, 1.0f / 16.0f, 1.0f)));
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        return getBoundingBoxes(state);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        if (isSupportBlock(world, hitPos.x, hitPos.y - 1, hitPos.z)) {
            return getDefaultState();
        }
        return null;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        if (!isSupportBlock(world, x, y - 1, z)) {
            dropAsItem(world, x, y, z);
            world.setBlock(x, y, z, de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air"));
        }
    }

    private boolean isSupportBlock(World world, int x, int y, int z) {
        Block blockBelow = world.getBlock(x, y, z);
        if (blockBelow == null || blockBelow.isAir()) return false;
        if (blockBelow instanceof CarpetBlock) return true;
        de.delautrer.game.registry.Tag<Block> carpetsTag = de.delautrer.game.registry.TagRegistry.getBlockTag("veinstride:carpets");
        if (carpetsTag != null && carpetsTag.contains(blockBelow)) return true;
        return blockBelow.isSolid && !blockBelow.isPassable;
    }

    private void dropAsItem(World world, int x, int y, int z) {
        String lootPath = this.getLootTable();
        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();
                for (ItemStack stack : drops) {
                    Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    Vector3f dropVel = new Vector3f((float) (Math.random() - 0.5) * 2.0f, 2.0f, (float) (Math.random() - 0.5) * 2.0f);
                    ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }
}
