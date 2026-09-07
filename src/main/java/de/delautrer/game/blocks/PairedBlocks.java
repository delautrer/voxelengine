package de.delautrer.game.blocks;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.events.BlockNeighborUpdateEvent;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.particle.ParticleSpawner;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class PairedBlocks {

    public static void placePair(World world, Vector3i bottomPos, BlockState bottomState) {
        if (bottomState == null || !(bottomState.getBlock() instanceof IPairedBlock pairedBlock)) return;

        Block block = bottomState.getBlock();
        BlockState topState = bottomState.with(DoorBlock.HALF, Half.TOP);

        world.setBlockWithState(bottomPos.x, bottomPos.y, bottomPos.z, block, bottomState.getStateId(), false, false);
        world.setBlockWithState(bottomPos.x, bottomPos.y + 1, bottomPos.z, block, topState.getStateId(), false, false);

        notifyCellPlaced(world, bottomPos, block);
        notifyCellPlaced(world, new Vector3i(bottomPos.x, bottomPos.y + 1, bottomPos.z), block);
    }

    private static void notifyCellPlaced(World world, Vector3i pos, Block newBlock) {
        if (world.getEventBus() != null) {
            int[][] dirs = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            for (int[] dir : dirs) {
                Vector3i nPos = new Vector3i(pos.x + dir[0], pos.y + dir[1], pos.z + dir[2]);
                world.getEventBus().publish(new BlockNeighborUpdateEvent(nPos, pos, newBlock));
            }
            Block blockBelow = world.getBlock(pos.x, pos.y - 1, pos.z);
            newBlock.onNeighborChanged(world, pos.x, pos.y, pos.z, new Vector3i(pos.x, pos.y - 1, pos.z), blockBelow);
        }
    }

    public static void breakPair(World world, Vector3i pos, BlockState state) {
        if (state == null || !(state.getBlock() instanceof IPairedBlock pairedBlock)) return;

        Vector3i partnerPos = pairedBlock.getPartnerPos(pos, state);
        BlockState partnerState = world.getBlockState(partnerPos.x, partnerPos.y, partnerPos.z);

        Block airBlock = Registries.BLOCKS.get(de.delautrer.Constants.NAMESPACE + ":air");

        if (partnerState != null && pairedBlock.isValidPartner(state, partnerState)) {
            world.setBlockWithState(partnerPos.x, partnerPos.y, partnerPos.z, airBlock, (byte) 0, false, false);
        }

        BlockState current = world.getBlockState(pos.x, pos.y, pos.z);
        if (current.getBlock() == state.getBlock()) {
            world.setBlockWithState(pos.x, pos.y, pos.z, airBlock, (byte) 0, false, false);
        }
    }

    public static void dropAndBreakPair(World world, Vector3i primaryPos, BlockState state) {
        if (state == null || !(state.getBlock() instanceof IPairedBlock)) return;

        Block block = state.getBlock();

        if (world != null && block != null) {
            ParticleSpawner.spawnBreak(world, primaryPos.x, primaryPos.y, primaryPos.z, block);
            if (block.getSoundMaterialName() != null) {
                SoundManager.playEvent(block.getSoundMaterialName(), "break", 1.0f, 1.0f, 1.0f, "Block");
            }
            dropAsItem(world, primaryPos, block);
        }

        breakPair(world, primaryPos, state);
    }

    public static void dropAsItem(World world, Vector3i pos, Block block) {
        if (world == null || block == null) return;
        String lootPath = block.getLootTable();
        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();
                for (ItemStack stack : drops) {
                    Vector3d dropPos = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
                    Vector3f dropVel = new Vector3f(
                        (float) (Math.random() - 0.5) * 2.0f,
                        2.0f,
                        (float) (Math.random() - 0.5) * 2.0f
                    );
                    ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }

    public static boolean validateOrDrop(World world, int x, int y, int z, BlockState state) {
        if (state == null || !(state.getBlock() instanceof IPairedBlock pairedBlock)) return true;

        Vector3i pos = new Vector3i(x, y, z);
        Half half = state.getValue(DoorBlock.HALF);

        Vector3i bottomPos = (half == Half.BOTTOM) ? pos : pairedBlock.getPartnerPos(pos, state);
        Block blockBelow = world.getBlock(bottomPos.x, bottomPos.y - 1, bottomPos.z);
        
        boolean missingSupport = (blockBelow == null || !blockBelow.isSolid || blockBelow.isPassable);
        Vector3i partnerPos = pairedBlock.getPartnerPos(pos, state);
        BlockState partnerState = world.getBlockState(partnerPos.x, partnerPos.y, partnerPos.z);
        boolean invalidPartner = !pairedBlock.isValidPartner(state, partnerState);

        if (missingSupport || invalidPartner) {
            dropAndBreakPair(world, bottomPos, state);
            return false;
        }

        return true;
    }
}
