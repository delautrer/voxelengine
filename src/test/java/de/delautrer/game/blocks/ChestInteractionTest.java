package de.delautrer.game.blocks;

import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChestInteractionTest {

    public static void main(String[] args) {
        ChestInteractionTest test = new ChestInteractionTest();
        test.testStructureChestInteraction();
        System.out.println("ChestInteractionTest: Structure chest interaction and loot generation verified!");
    }

    @Test
    public void testStructureChestInteraction() {
        Registries.init();
        World world = new World(null, null, null, 12345L, "world_test", "world_test_save", "DEFAULT", "", null, false);

        Vector3i pos = new Vector3i(10, 64, 10);
        Block oakChest = Registries.BLOCKS.get("veinstride:oak_chest");
        Assertions.assertNotNull(oakChest, "oak_chest block must exist!");

        Chunk chunk = new Chunk(pos.x >> 4, pos.z >> 4);
        chunk.setPalette(world.getBlockPalette());
        world.getChunkManager().addChunk(chunk);

        // Set chest block in world
        world.setBlock(pos.x, pos.y, pos.z, oakChest, (byte) 0);

        int lx = pos.x & 15;
        int lz = pos.z & 15;
        CompoundTag nbt = new CompoundTag();
        nbt.putString("LootTable", "veinstride:chests/desert_camp");
        chunk.setBlockEntityTag(lx, pos.y, lz, nbt);

        // Remove block entity from world.blockEntities to simulate uninstantiated world-gen chest
        world.setBlockEntity(pos, null);

        BlockEntity be = world.getBlockEntity(pos);
        Assertions.assertNotNull(be, "World.getBlockEntity must lazily instantiate ChestBlockEntity!");
        Assertions.assertTrue(be instanceof ChestBlockEntity, "BlockEntity must be instance of ChestBlockEntity!");

        ChestBlockEntity chest = (ChestBlockEntity) be;
        boolean hasLoot = false;
        for (int s = 0; s < chest.getInventory().getSize(); s++) {
            if (chest.getInventory().getStack(s) != null) {
                hasLoot = true;
                break;
            }
        }
        Assertions.assertTrue(hasLoot, "Chest inventory should contain generated loot");
    }
}
