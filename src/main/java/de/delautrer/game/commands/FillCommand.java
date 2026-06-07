package de.delautrer.game.commands;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.LeavesBlock;
import de.delautrer.game.blocks.state.*;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.World;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FillCommand implements ICommand {

    @Override
    public String getName() {
        return "fill";
    }

    @Override
    public String getUsage() {
        return "/fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [replace|keep|outline|hollow|destroy] [filter_block]";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 7) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        try {
            int x1 = parseCoord(args[0], player.position.x);
            int y1 = parseCoord(args[1], player.position.y);
            int z1 = parseCoord(args[2], player.position.z);
            int x2 = parseCoord(args[3], player.position.x);
            int y2 = parseCoord(args[4], player.position.y);
            int z2 = parseCoord(args[5], player.position.z);
            
            BlockState fillState = parseBlockState(args[6]);
            
            String mode = "replace";
            BlockState filterState = null;
            
            if (args.length >= 8) {
                mode = args[7].toLowerCase();
            }
            if (mode.equals("replace") && args.length >= 9) {
                filterState = parseBlockState(args[8]);
            }

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            int count = 0;
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean isEdge = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                        
                        byte currentBlockId = world.getBlockAt(x, y, z);
                        BlockState currentState = world.getBlockState(new Vector3i(x, y, z));
                        
                        boolean shouldPlace = false;
                        boolean shouldDestroy = false;
                        boolean shouldPlaceAir = false;

                        switch (mode) {
                            case "replace":
                                if (filterState != null) {
                                    if (currentState.getBlock().getId() == filterState.getBlock().getId()) {
                                        shouldPlace = true;
                                    }
                                } else {
                                    shouldPlace = true;
                                }
                                break;
                            case "keep":
                                if (currentBlockId == 0) { // Air
                                    shouldPlace = true;
                                }
                                break;
                            case "outline":
                                if (isEdge) {
                                    shouldPlace = true;
                                } else {
                                    // inside does not change
                                }
                                break;
                            case "hollow":
                                if (isEdge) {
                                    shouldPlace = true;
                                } else {
                                    shouldPlaceAir = true;
                                }
                                break;
                            case "destroy":
                                shouldPlace = true;
                                if (currentBlockId != 0) {
                                    shouldDestroy = true;
                                }
                                break;
                            default:
                                commandManager.sendMessageInChat("Unknown mode: " + mode);
                                return;
                        }

                        if (shouldDestroy) {
                            Block currentBlock = BlockRegistry.get(currentBlockId);
                            if (currentBlock != null) {
                                String lootPath = currentBlock.getLootTable();
                                if (lootPath != null) {
                                    LootTable table = LootTableManager.load(lootPath);
                                    if (table != null) {
                                        List<ItemStack> drops = table.generateLoot();
                                        for (ItemStack stack : drops) {
                                            Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                                            Vector3f dropVel = new Vector3f((float)(Math.random() - 0.5) * 2f, 2.0f, (float)(Math.random() - 0.5) * 2f);
                                            ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                                            world.spawnEntity(entity);
                                        }
                                    }
                                }
                            }
                        }

                        if (shouldPlace) {
                            world.setBlockWithState(x, y, z, fillState.getBlock().getId(), fillState.getStateId(), false);
                            count++;
                        } else if (shouldPlaceAir) {
                            world.setBlock(x, y, z, (byte) 0);
                            count++;
                        }
                    }
                }
            }
            commandManager.sendMessageInChat("Successfully filled " + count + " blocks.");
        } catch (Exception e) {
            commandManager.sendMessageInChat("Error: " + e.getMessage());
        }
    }

    private int parseCoord(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return (int) Math.floor(current);
            return (int) Math.floor(current + Double.parseDouble(arg.substring(1)));
        }
        return Integer.parseInt(arg);
    }

    private BlockState parseBlockState(String input) throws Exception {
        String blockPart = input;
        String statePart = null;

        if (input.contains("[") && input.endsWith("]")) {
            blockPart = input.substring(0, input.indexOf("["));
            statePart = input.substring(input.indexOf("[") + 1, input.length() - 1);
        }

        if (!blockPart.contains(":")) {
            blockPart = Constants.NAMESPACE + ":" + blockPart;
        }

        Block block = BlockRegistry.REGISTRY.get(blockPart);
        if (block == null) {
            throw new Exception("Unknown block: " + blockPart);
        }

        BlockState state = block.getDefaultState();
        
        // Special case for leaves (persist by default when placed by command)
        if (blockPart.contains("leaves")) {
            state = state.with(LeavesBlock.PERSISTENT, true);
        }

        if (statePart != null) {
            String[] pairs = statePart.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length != 2) throw new Exception("Invalid state format: " + pair);
                String key = kv[0].trim();
                String val = kv[1].trim();
                
                state = applyProperty(state, key, val);
            }
        }

        return state;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private BlockState applyProperty(BlockState state, String key, String val) throws Exception {
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            Property prop = entry.getKey();
            if (prop.getName().equalsIgnoreCase(key)) {
                Comparable value = parseValue(prop, val);
                return state.with(prop, value);
            }
        }
        throw new Exception("Property '" + key + "' not found for block " + state.getBlock().getClass().getSimpleName());
    }

    private Comparable<?> parseValue(Property<?> prop, String val) throws Exception {
        if (prop instanceof BooleanProperty) {
            return Boolean.parseBoolean(val);
        } else if (prop instanceof IntProperty) {
            return Integer.parseInt(val);
        } else if (prop instanceof EnumProperty<?> ep) {
            for (Object enumVal : ep.getAllowedValues()) {
                if (enumVal.toString().equalsIgnoreCase(val)) {
                    return (Comparable<?>) enumVal;
                }
            }
        }
        throw new Exception("Cannot parse value '" + val + "' for property " + prop.getName());
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length >= 1 && args.length <= 6) {
            addCoordCompletion(completions, player, (args.length - 1) % 3);
        } else if (args.length == 7 || (args.length == 9 && args[7].equalsIgnoreCase("replace"))) {
            String input = args[args.length - 1].toLowerCase();
            for (String key : BlockRegistry.getAll().keySet()) {
                String shortKey = key.startsWith(Constants.NAMESPACE + ":") ? key.substring(Constants.NAMESPACE.length() + 1) : key;
                if (key.toLowerCase().startsWith(input)) completions.add(key);
                else if (shortKey.toLowerCase().startsWith(input)) completions.add(shortKey);
            }
        } else if (args.length == 8) {
            String[] modes = {"replace", "keep", "outline", "hollow", "destroy"};
            for (String m : modes) {
                if (m.startsWith(args[7].toLowerCase())) completions.add(m);
            }
        }

        List<String> filtered = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(current)) filtered.add(c);
        }
        return filtered;
    }

    private void addCoordCompletion(List<String> completions, LocalPlayer player, int axis) {
        completions.add("~");
        
        Vector3i targetPos = player.getInteraction().getSelectedBlockPos();
        Vector3d pos = player.position;
        
        if (targetPos != null) {
            if (axis == 0) completions.add(String.valueOf(targetPos.x));
            if (axis == 1) completions.add(String.valueOf(targetPos.y));
            if (axis == 2) completions.add(String.valueOf(targetPos.z));
        } else {
            if (axis == 0) completions.add(String.valueOf((int)Math.floor(pos.x)));
            if (axis == 1) completions.add(String.valueOf((int)Math.floor(pos.y)));
            if (axis == 2) completions.add(String.valueOf((int)Math.floor(pos.z)));
        }
    }
}
