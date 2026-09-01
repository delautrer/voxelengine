package de.delautrer.game.commands;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.DoorBlock;
import de.delautrer.game.blocks.LogBlock;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.biome.TreeFeature;
import de.delautrer.game.world.generation.biome.Biome;
import java.util.ArrayList;
import java.util.List;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.generation.feature.ConfiguredFeature;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import de.delautrer.Constants;
import de.delautrer.game.blocks.state.BlockProperties.Axis;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.engine.input.InputManager;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.Map;

public class DebugCommand implements ICommand {
    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getUsage() {
        return "/debug [blocks|trees|chunk|sound|block] - General world/chunk debug info or grids";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        if (args.length == 0) {
            sendGeneralDebug(player, world, manager);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "blocks":
                generateBlocks(player, world, manager);
                break;
            case "trees":
                String specific = args.length > 1 ? args[1].toLowerCase() : null;
                generateTrees(player, world, manager, specific);
                break;
            case "chunk":
                sendChunkDebug(player, world, manager);
                break;
            case "sound":
                toggleSoundDebug(manager);
                break;
            case "block":
                debugBlock(player, world, manager);
                break;
            default:
                manager.sendMessageInChat("Unknown debug subcommand: " + sub);
                break;
        }
    }

    private void debugBlock(LocalPlayer player, World world, CommandManager manager) {
        Vector3d pos = player.getCamera().getPosition();
        Vector3f camPos = new Vector3f((float)pos.x, (float)pos.y, (float)pos.z);
        Vector3f camFront = player.getCamera().getFront();
        
        Raycaster.RaycastResult result = Raycaster.raycast(world, camPos, camFront, 10.0f);
        
        if (result == null) {
            manager.sendMessageInChat("No block in range!");
            return;
        }

        BlockState state = world.getBlockState(result.hitPos.x, result.hitPos.y, result.hitPos.z);
        Block block = state.getBlock();
        String blockName = BlockRegistry.REGISTRY.getKey(block).toString();
        
        manager.sendMessageInChat("Type: " + blockName);
        manager.sendMessageInChat("Pos: " + result.hitPos.x + ", " + result.hitPos.y + ", " + result.hitPos.z);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Block: ").append(blockName).append("\n");
        sb.append("Pos: ").append(result.hitPos.x).append(", ").append(result.hitPos.y).append(", ").append(result.hitPos.z).append("\n");
        sb.append("States:\n");

        Map<Property<?>, Comparable<?>> props = state.getProperties();
        if (props.isEmpty()) {
            manager.sendMessageInChat("No states available.");
            sb.append("- none");
        } else {
            for (Map.Entry<Property<?>, Comparable<?>> entry : props.entrySet()) {
                String line = entry.getKey().getName() + ": " + entry.getValue().toString();
                manager.sendMessageInChat("- " + line);
                sb.append("- ").append(line).append("\n");
            }
        }

        if (InputManager.INSTANCE != null) {
            InputManager.INSTANCE.setClipboardString(sb.toString());
            manager.sendMessageInChat("Block info copied to clipboard!");
        }
    }

    private void toggleSoundDebug(CommandManager manager) {
        boolean current = de.delautrer.game.settings.SettingsManager.get().soundDebug;
        de.delautrer.game.settings.SettingsManager.get().soundDebug = !current;
        de.delautrer.game.settings.SettingsManager.save();
        manager.sendMessageInChat("Sound Debug: " + (!current ? "ON" : "OFF"));
    }

    private void sendGeneralDebug(LocalPlayer player, World world, CommandManager manager) {
        manager.sendMessageInChat("--- World Debug ---");
        manager.sendMessageInChat("Seed: " + world.getSeed());
        manager.sendMessageInChat("Time: " + String.format("%.2f", world.getSkyManager().getTimeOfDay()));
        manager.sendMessageInChat("Pos: " + String.format("%.1f, %.1f, %.1f", player.position.x, player.position.y, player.position.z));
        manager.sendMessageInChat("Chunks loaded: " + world.getChunkManager().getLoadedChunks().size());
        manager.sendMessageInChat("Entities: " + world.getEntities().size());
    }

    private void sendChunkDebug(LocalPlayer player, World world, CommandManager manager) {
        int cx = (int) Math.floor(player.position.x / Chunk.SIZE);
        int cz = (int) Math.floor(player.position.z / Chunk.SIZE);
        Chunk chunk = world.getChunkManager().getChunkAtBlock((int)Math.floor(player.position.x), 0, (int)Math.floor(player.position.z));
        
        manager.sendMessageInChat("--- Chunk Debug [" + cx + ", " + cz + "] ---");
        if (chunk == null) {
            manager.sendMessageInChat("Chunk not loaded!");
            return;
        }
        
        int lx = (int) Math.floor(player.position.x) % Chunk.SIZE;
        if (lx < 0) lx += Chunk.SIZE;
        int lz = (int) Math.floor(player.position.z) % Chunk.SIZE;
        if (lz < 0) lz += Chunk.SIZE;
        
        Biome biome = chunk.getBiome(lx, lz);
        manager.sendMessageInChat("Biome at Player: " + (biome != null ? biome.getName() : "null"));
        Object meshPair = world.getChunkManager().getMeshes().get(new Vector2i(cx, cz));
        manager.sendMessageInChat("Mesh Status: " + (meshPair != null ? "Generated" : "Pending"));
        manager.sendMessageInChat("Dirty: " + chunk.isDirty());
    }

    private void generateBlocks(LocalPlayer player, World world, CommandManager manager) {
        int startX = (int) Math.floor(player.position.x);
        int startZ = (int) Math.floor(player.position.z);
        int targetY = 128;

        player.position.set(startX, targetY + 2.0f, startZ);
        player.velocity.set(0);

        Block[] allBlocks = BlockRegistry.getAll().values().toArray(new Block[0]);
        int count = allBlocks.length;
        int gridSize = (int) Math.ceil(Math.sqrt(count));

        Block air = Registries.BLOCKS.get("veinstride:air");
        Block floor = Registries.BLOCKS.get(Constants.NAMESPACE + ":grass_block");

        for (int gx = -1; gx <= gridSize * 2; gx++) {
            for (int gz = -1; gz <= gridSize * 2; gz++) {
                int bx = startX + gx;
                int bz = startZ + gz;
                world.setBlock(bx, targetY - 1, bz, floor);
                for (int y = 0; y < 5; y++) world.setBlock(bx, targetY + y, bz, air);
            }
        }

        int blockIndex = 0;
        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                if (blockIndex >= count) break;
                int bx = startX + (gx * 2);
                int bz = startZ + (gz * 2);
                Block b = allBlocks[blockIndex++];
                if (b.isAir()) continue;
                
                if (b instanceof LogBlock) {
                    BlockState logState = b.getDefaultState().with(LogBlock.AXIS, Axis.Y);
                    world.setBlockState(bx, targetY, bz, logState);
                } else if (b instanceof DoorBlock) {
                    BlockState bottomState = b.getDefaultState().with(DoorBlock.HALF, Half.BOTTOM);
                    world.setBlockState(bx, targetY, bz, bottomState);
                    
                    BlockState topState = b.getDefaultState().with(DoorBlock.HALF, Half.TOP);
                    world.setBlockState(bx, targetY + 1, bz, topState);
                } else {
                    world.setBlock(bx, targetY, bz, b);
                }
            }
        }
        manager.sendMessageInChat("Debug-Blockgrid (" + count + " Blocks) generated!");
    }

    private void generateTrees(LocalPlayer player, World world, CommandManager manager, String specificType) {
        String[][] treeTypes = {
            {"oak", "oak_log", "oak_leaves", "STANDARD"},
            {"birch", "birch_log", "birch_leaves", "STANDARD"},
            {"pine", "pine_log", "pine_leaves", "PINE"},
            {"tall_pine", "pine_log", "pine_leaves", "TALL_PINE"},
            {"willow", "willow_log", "willow_leaves", "WILLOW"},
            {"baobab", "baobab_log", "baobab_leaves", "BAOBAB"},
            {"mahogany", "mahogany_log", "mahogany_leaves", "MAHOGANY"},
            {"palm", "palm_log", "palm_leaves", "PALM"}
        };

        int startX = (int) Math.floor(player.position.x);
        int startZ = (int) Math.floor(player.position.z);
        int targetY = (int) Math.floor(player.position.y);
        Block floorBlock = Registries.BLOCKS.get("veinstride:grass_block");

        if (specificType != null) {
            String[] target = null;
            for (String[] td : treeTypes) {
                if (td[0].equalsIgnoreCase(specificType)) {
                    target = td;
                    break;
                }
            }
            if (target == null) {
                manager.sendMessageInChat("Unknown tree type: " + specificType);
                return;
            }

            for (int i = 0; i < 6; i++) {
                int bx = startX + (i % 3 * 10);
                int bz = startZ + (i / 3 * 10);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        world.setBlock(bx + dx, targetY - 1, bz + dz, floorBlock);
                    }
                }
                NamespacedKey featKey = NamespacedKey.fromString("veinstride:" + target[0]);
                ConfiguredFeature feature = FeatureRegistry.getConfiguredFeature(featKey);
                if (feature != null) {
                    feature.generate(world, bx, targetY, bz, world.getSeed() + (i * 999));
                } else {
                    Block logBlock = Registries.BLOCKS.get("veinstride:" + target[1]);
                    Block leavesBlock = Registries.BLOCKS.get("veinstride:" + target[2]);
                    TreeFeature.TreeShape shape = TreeFeature.TreeShape.valueOf(target[3]);
                    TreeFeature.generate(world, bx, targetY, bz, world.getSeed() + (i * 999), shape, logBlock, leavesBlock, 4, 3);
                }
            }
            manager.sendMessageInChat("Debug-Trees (6x " + target[0] + ") generated!");
        } else {
            int spacing = 12;
            int gridSize = (int) Math.ceil(Math.sqrt(treeTypes.length));

            for (int i = 0; i < treeTypes.length; i++) {
                int bx = startX + (i % gridSize * spacing);
                int bz = startZ + (i / gridSize * spacing);
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        world.setBlock(bx + dx, targetY - 1, bz + dz, floorBlock);
                    }
                }
                NamespacedKey featKey = NamespacedKey.fromString("veinstride:" + treeTypes[i][0]);
                ConfiguredFeature feature = FeatureRegistry.getConfiguredFeature(featKey);
                if (feature != null) {
                    feature.generate(world, bx, targetY, bz, world.getSeed() + (i * 999));
                } else {
                    Block logBlock = Registries.BLOCKS.get("veinstride:" + treeTypes[i][1]);
                    Block leavesBlock = Registries.BLOCKS.get("veinstride:" + treeTypes[i][2]);
                    TreeFeature.TreeShape shape = TreeFeature.TreeShape.valueOf(treeTypes[i][3]);
                    TreeFeature.generate(world, bx, targetY, bz, world.getSeed() + (i * 999), shape, logBlock, leavesBlock, 4, 3);
                }
            }
            manager.sendMessageInChat("Debug-Trees (All) generated!");
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            String[] subs = {"blocks", "trees", "chunk", "sound", "block"};
            for (String s : subs) if (s.startsWith(current)) completions.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("trees")) {
            String[] types = {"oak", "birch", "pine", "willow", "baobab", "mahogany", "palm", "tall_oak", "tall_birch", "tall_pine"};
            for (String type : types) if (type.startsWith(current)) completions.add(type);
        }
        return completions;
    }
}
