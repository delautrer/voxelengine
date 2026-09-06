package de.delautrer.game.world.generation.structure;

import de.delautrer.engine.events.EventBus;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.commands.CommandManager;
import de.delautrer.game.commands.StructureCommand;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

import java.util.List;

public class StructureRegistryTest {

    public static void main(String[] args) throws Exception {
        StructureRegistryTest test = new StructureRegistryTest();
        test.testStructureLoading();
        test.testStructureSaveAndPlace();
        System.out.println("StructureRegistryTest: All structure templates, structures, and structure save/place roundtrip verified!");
    }

    public void testStructureLoading() {
        Registries.init();
        if (StructureRegistry.getTemplatesCount() < 2) {
            throw new IllegalStateException("Expected at least 2 templates, got " + StructureRegistry.getTemplatesCount());
        }
        if (StructureRegistry.getStructuresCount() < 2) {
            throw new IllegalStateException("Expected at least 2 structures, got " + StructureRegistry.getStructuresCount());
        }
        if (StructureRegistry.getStructureSetsCount() < 2) {
            throw new IllegalStateException("Expected at least 2 structure sets, got " + StructureRegistry.getStructureSetsCount());
        }
    }

    public void testStructureSaveAndPlace() {
        Registries.init();
        World world = new World(null, null, null, 12345L, "world_test", "world_test_save", "DEFAULT", "", null, true);
        world.setCheatsAllowed(true);
        Chunk chunk = new Chunk(0, 0);
        chunk.setPalette(world.getBlockPalette());
        world.getChunkManager().addChunk(chunk);

        LocalPlayer player = new LocalPlayer(new org.joml.Vector3d(0, 5, 0));
        CommandManager cm = new CommandManager(new EventBus());

        Block sandstone = Registries.BLOCKS.get("veinstride:sandstone");
        Block stairs = Registries.BLOCKS.get("veinstride:sandstone_stairs");
        Block chestBlock = Registries.BLOCKS.get("veinstride:oak_chest");
        Block air = Registries.BLOCKS.get("veinstride:air");

        byte stairStateId = (byte) 2;

        // Set blocks with stateId (including rotated chest)
        world.setBlock(0, 5, 0, sandstone);
        world.setBlockWithState(1, 5, 0, stairs, stairStateId, false);
        world.setBlockWithState(2, 5, 0, chestBlock, (byte) 2, false);

        byte savedStairStateId = world.getBlockState(1, 5, 0).getStateId();
        byte savedChestStateId = world.getBlockState(2, 5, 0).getStateId();

        CompoundTag chestTag = new CompoundTag();
        chestTag.putString("LootTable", "veinstride:chests/desert_camp");
        chunk.setBlockEntityTag(2, 5, 0, chestTag);

        // Test tab completions for /structure save
        StructureCommand structCmd = new StructureCommand();
        List<String> saveCompletions = structCmd.getTabCompletions(player, new String[]{"save", "testhut", ""});
        if (saveCompletions.isEmpty() || !saveCompletions.contains("~")) {
            throw new IllegalStateException("Failed to get coordinate tab completions for /structure save!");
        }

        // Save structure via command
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"save", "test_hut_v2", "0", "5", "0", "2", "5", "0"}, player, world));

        NamespacedKey key = NamespacedKey.fromString("veinstride:test_hut_v2");
        StructureTemplate template = StructureRegistry.getTemplate(key);
        if (template == null || template.getSizeX() != 3 || template.getSizeY() != 1 || template.getSizeZ() != 1) {
            throw new IllegalStateException("Failed to save and hot-register test_hut_v2 structure template! Got template: " + template);
        }

        // Clear area
        for (int x = 0; x < 3; x++) {
            world.setBlock(x, 5, 0, air);
            world.setBlockEntity(new Vector3i(x, 5, 0), null);
            chunk.setBlockEntityTag(x, 5, 0, null);
        }

        // Place structure via /structure place subcommand
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"place", "test_hut_v2", "0", "5", "0"}, player, world));

        Block currentSandstone = world.getBlock(0, 5, 0);
        if (currentSandstone != sandstone) {
            throw new IllegalStateException("Failed to place sandstone via /structure place command!");
        }

        BlockState stairState = world.getBlockState(1, 5, 0);
        if (stairState == null || stairState.getBlock() != stairs || stairState.getStateId() != savedStairStateId) {
            throw new IllegalStateException("Failed to preserve stair stateId (expected " + savedStairStateId + ", got " + (stairState != null ? stairState.getStateId() : null) + ")!");
        }

        BlockState chestState = world.getBlockState(2, 5, 0);
        if (chestState == null || chestState.getBlock() != chestBlock || chestState.getStateId() != savedChestStateId) {
            throw new IllegalStateException("Failed to preserve chest rotation stateId (expected " + savedChestStateId + ", got " + (chestState != null ? chestState.getStateId() : null) + ")!");
        }

        BlockEntity placedChestBe = world.getBlockEntity(new Vector3i(2, 5, 0));
        boolean hasItems = false;
        if (placedChestBe instanceof ChestBlockEntity placedChest) {
            for (int s = 0; s < placedChest.getInventory().getSize(); s++) {
                if (placedChest.getInventory().getStack(s) != null) {
                    hasItems = true;
                    break;
                }
            }
        }
        if (!hasItems) {
            throw new IllegalStateException("Failed to generate chest loot on /structure place!");
        }

        // Test Phase 6: structure_void, structure_block, /give, CreativeContainer filter, /structure load
        testStructureBlockAndVoid(player, world, cm);
    }

    private void testStructureBlockAndVoid(LocalPlayer player, World world, CommandManager cm) {
        // 1. Test /give command for structure_void and structure_block
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("give", new String[]{"structure_void", "1"}, player, world));
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("give", new String[]{"structure_block", "1"}, player, world));

        boolean foundVoid = false, foundBlock = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack != null && stack.type != null) {
                String id = de.delautrer.game.items.ItemRegistry.getId(stack.type);
                if ("veinstride:structure_void".equals(id)) foundVoid = true;
                if ("veinstride:structure_block".equals(id)) foundBlock = true;
            }
        }
        if (!foundVoid || !foundBlock) {
            throw new IllegalStateException("Failed to give structure_void or structure_block via /give command!");
        }

        // 2. Test CreativeContainer filter
        de.delautrer.game.ui.gui.container.CreativeContainer cc = new de.delautrer.game.ui.gui.container.CreativeContainer(player.getInventory());
        for (de.delautrer.game.items.Item item : cc.getAllItems()) {
            if (item != null) {
                String id = de.delautrer.game.items.ItemRegistry.getId(item);
                if ("veinstride:structure_void".equals(id) || "veinstride:structure_block".equals(id)) {
                    throw new IllegalStateException("Structure items must not appear in CreativeContainer grid: " + id);
                }
            }
        }

        // 3. Test /structure load command
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"load", "test_hut_v2", "0", "10", "0"}, player, world));

        Block loadedBlock = world.getBlock(0, 10, 0);
        if (!(loadedBlock instanceof de.delautrer.game.blocks.StructureBlock)) {
            throw new IllegalStateException("Failed to place structure_block via /structure load command!");
        }

        BlockEntity be = world.getBlockEntity(new Vector3i(0, 10, 0));
        if (!(be instanceof de.delautrer.game.blocks.entities.StructureBlockEntity sbe)) {
            throw new IllegalStateException("Failed to create StructureBlockEntity on /structure load!");
        }

        if (!"load".equalsIgnoreCase(sbe.getMode()) || !"test_hut_v2".equals(sbe.getName()) || sbe.getSizeX() != 3) {
            throw new IllegalStateException("StructureBlockEntity misconfigured: mode=" + sbe.getMode() + ", name=" + sbe.getName() + ", sizeX=" + sbe.getSizeX());
        }

        // 5. Test Raycaster pass-through for structure_void
        Block structVoidBlock = Registries.BLOCKS.get("veinstride:structure_void");
        world.setBlock(0, 15, 0, structVoidBlock);

        org.joml.Vector3f eyePos = new org.joml.Vector3f(0.5f, 16.5f, 0.5f);
        org.joml.Vector3f lookDir = new org.joml.Vector3f(0.0f, -1.0f, 0.0f);

        de.delautrer.engine.physics.Raycaster.RaycastResult resultWithoutVoid = de.delautrer.engine.physics.Raycaster.raycast(world, eyePos, lookDir, 5.0f, false);
        if (resultWithoutVoid != null && resultWithoutVoid.hitPos.equals(new Vector3i(0, 15, 0))) {
            throw new IllegalStateException("Raycast hit structure_void when NOT holding structure_void item!");
        }

        de.delautrer.engine.physics.Raycaster.RaycastResult resultWithVoid = de.delautrer.engine.physics.Raycaster.raycast(world, eyePos, lookDir, 5.0f, true);
        if (resultWithVoid == null || !resultWithVoid.hitPos.equals(new Vector3i(0, 15, 0))) {
            throw new IllegalStateException("Raycast failed to hit structure_void when holding structure_void item!");
        }

        // 6. Test /give tab completions returning veinstride:... keys
        de.delautrer.game.commands.GiveCommand giveCmd = new de.delautrer.game.commands.GiveCommand();
        List<String> completions = giveCmd.getTabCompletions(player, new String[]{"dir"});
        if (completions.isEmpty() || !completions.contains("veinstride:dirt") || completions.contains("dirt")) {
            throw new IllegalStateException("GiveCommand tab completions should return veinstride:dirt, got: " + completions);
        }

        // 7. Test UIInputField bounds safety on empty text
        de.delautrer.game.ui.elements.UIInputField field = new de.delautrer.game.ui.elements.UIInputField(0, 0, 100, 20, "Test", 10);
        field.setText("abc");
        field.setFocused(true);
        field.setText("");
        field.backspace();
        field.delete();
        field.deleteSelection();
        if (!field.getText().isEmpty()) {
            throw new IllegalStateException("UIInputField bounds safety test failed!");
        }

        // 8. Test auto-template size loading in StructureBlockEntity
        sbe.setName("test_hut_v2");
        if (sbe.getSizeX() != 3 || sbe.getSizeY() != 1 || sbe.getSizeZ() != 1) {
            throw new IllegalStateException("StructureBlockEntity failed to auto-load template size: " + sbe.getSizeX() + "x" + sbe.getSizeY() + "x" + sbe.getSizeZ());
        }

        // 9. Test /structure vsnbt subcommand
        Block chestBlock = Registries.BLOCKS.get("veinstride:oak_chest");
        world.setBlock(5, 5, 5, chestBlock);
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"vsnbt", "desert_camp", "5", "5", "5"}, player, world));

        de.delautrer.game.world.Chunk targetChunk = world.getChunkManager().getChunkAtBlock(5, 5, 5);
        if (targetChunk == null) throw new IllegalStateException("Chunk null for vsnbt test");
        de.delautrer.game.nbt.CompoundTag chestTag = targetChunk.getBlockEntityTag(new Vector3i(5, 5, 5));
        if (chestTag == null || !"veinstride:chests/desert_camp".equals(chestTag.getString("LootTable"))) {
            throw new IllegalStateException("Failed to set LootTable via /structure vsnbt! Got tag: " + chestTag);
        }

        // 10. Test Jigsaw Block, Template Pools, and /structure jigsaw
        Block jigsawBlock = Registries.BLOCKS.get("veinstride:jigsaw");
        if (jigsawBlock == null) {
            throw new IllegalStateException("Jigsaw block veinstride:jigsaw not found in BLOCKS registry!");
        }

        if (de.delautrer.game.worldgen.pool.TemplatePoolRegistry.getPool(NamespacedKey.fromString("veinstride:desert_camp/starts")) == null) {
            throw new IllegalStateException("TemplatePool veinstride:desert_camp/starts not found!");
        }

        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"jigsaw", "desert_camp/starts", "10", "10", "10"}, player, world));

        // 11. Test Jigsaw Block FACING, BE Orientation Sync, and Interaction
        de.delautrer.game.blocks.JigsawBlock jBlock = (de.delautrer.game.blocks.JigsawBlock) jigsawBlock;
        world.setBlockState(12, 10, 12, jBlock.getDefaultState().with(de.delautrer.game.blocks.JigsawBlock.FACING, de.delautrer.game.blocks.state.BlockProperties.Direction.EAST));
        jBlock.onBlockPlaced(world, new Vector3i(12, 10, 12), world.getBlockState(12, 10, 12), player);

        de.delautrer.game.blocks.entities.JigsawBlockEntity jbe = (de.delautrer.game.blocks.entities.JigsawBlockEntity) world.getBlockEntity(new Vector3i(12, 10, 12));
        if (jbe == null || !"east".equals(jbe.getOrientation())) {
            throw new IllegalStateException("Jigsaw BE orientation failed to sync with block state FACING! Got: " + (jbe != null ? jbe.getOrientation() : "null"));
        }

        // Test normal interact opens JigsawInventory
        jBlock.onInteract(world, new Vector3i(12, 10, 12), player);
        if (!(player.getOpenedInventory() instanceof de.delautrer.game.inventory.JigsawInventory)) {
            throw new IllegalStateException("JigsawBlock onInteract failed to open JigsawInventory!");
        }

        // Test sneak interact rotates FACING (EAST -> SOUTH)
        player.setSneaking(true);
        jBlock.onInteract(world, new Vector3i(12, 10, 12), player);
        if (world.getBlockState(12, 10, 12).getValue(de.delautrer.game.blocks.JigsawBlock.FACING) != de.delautrer.game.blocks.state.BlockProperties.Direction.SOUTH) {
            throw new IllegalStateException("Sneak interact failed to rotate Jigsaw FACING!");
        }
        player.setSneaking(false);

        // 12. Test Village Jigsaw Pools, Structure, and /structure jigsaw veinstride:village/centers
        if (de.delautrer.game.worldgen.pool.TemplatePoolRegistry.getPool(NamespacedKey.fromString("veinstride:village/centers")) == null) {
            throw new IllegalStateException("TemplatePool veinstride:village/centers not found!");
        }
        cm.onEvent(new de.delautrer.game.events.CommandExecutedEvent("structure", new String[]{"jigsaw", "village/centers", "20", "10", "20"}, player, world));
    }
}
