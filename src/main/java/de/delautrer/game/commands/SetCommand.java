package de.delautrer.game.commands;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.LeavesBlock;
import de.delautrer.game.blocks.state.*;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetCommand implements ICommand {

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getUsage() {
        return "/set <block|cube|room|circle|sphere|cylinder> ... <block_id[state]>";
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
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 2) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "block":
                    handleBlock(player, world, args, commandManager);
                    break;
                case "cube":
                    handleCube(player, world, args, commandManager, false);
                    break;
                case "room":
                    handleCube(player, world, args, commandManager, true);
                    break;
                case "circle":
                    handleCircle(player, world, args, commandManager);
                    break;
                case "sphere":
                    handleSphere(player, world, args, commandManager);
                    break;
                case "cylinder":
                    handleCylinder(player, world, args, commandManager);
                    break;
                default:
                    commandManager.sendMessageInChat("Unknown subcommand: " + subCommand);
                    break;
            }
        } catch (Exception e) {
            commandManager.sendMessageInChat("Error: " + e.getMessage());
        }
    }

    private void handleBlock(LocalPlayer player, World world, String[] args, CommandManager commandManager) throws Exception {
        if (args.length != 5) {
            throw new Exception("Usage: /set block <x> <y> <z> <block>");
        }
        int x = parseCoord(args[1], player.position.x);
        int y = parseCoord(args[2], player.position.y);
        int z = parseCoord(args[3], player.position.z);
        BlockState state = parseBlockState(args[4]);
        world.setBlockState(x, y, z, state);
        commandManager.sendMessageInChat("Block set at " + x + ", " + y + ", " + z);
    }

    private void handleCube(LocalPlayer player, World world, String[] args, CommandManager commandManager, boolean room) throws Exception {
        if (args.length != 8) {
            throw new Exception("Usage: /set " + (room ? "room" : "cube") + " <x1> <y1> <z1> <x2> <y2> <z2> <block>");
        }
        int x1 = parseCoord(args[1], player.position.x);
        int y1 = parseCoord(args[2], player.position.y);
        int z1 = parseCoord(args[3], player.position.z);
        int x2 = parseCoord(args[4], player.position.x);
        int y2 = parseCoord(args[5], player.position.y);
        int z2 = parseCoord(args[6], player.position.z);
        BlockState state = parseBlockState(args[7]);

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
                    if (room) {
                        boolean isEdge = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                        if (!isEdge) continue;
                    }
                    world.setBlockWithState(x, y, z, state.getBlock().getId(), state.getStateId(), false);
                    count++;
                }
            }
        }
        commandManager.sendMessageInChat((room ? "Room" : "Cube") + " generated with " + count + " blocks.");
    }

    private void handleCircle(LocalPlayer player, World world, String[] args, CommandManager commandManager) throws Exception {
        if (args.length != 3) {
            throw new Exception("Usage: /set circle <radius> <block>");
        }
        int radius = Integer.parseInt(args[1]);
        BlockState state = parseBlockState(args[2]);
        
        int cx = (int) Math.floor(player.position.x);
        int cy = (int) Math.floor(player.position.y);
        int cz = (int) Math.floor(player.position.z);
        
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x*x + z*z);
                if (dist <= radius) {
                    world.setBlockWithState(cx + x, cy, cz + z, state.getBlock().getId(), state.getStateId(), false);
                    count++;
                }
            }
        }
        commandManager.sendMessageInChat("Circle (Disk) generated with " + count + " blocks.");
    }

    private void handleSphere(LocalPlayer player, World world, String[] args, CommandManager commandManager) throws Exception {
        if (args.length != 4) {
            throw new Exception("Usage: /set sphere <radius> <hollow> <block>");
        }
        int radius = Integer.parseInt(args[1]);
        boolean hollow = Boolean.parseBoolean(args[2]);
        BlockState state = parseBlockState(args[3]);
        
        int cx = (int) Math.floor(player.position.x);
        int cy = (int) Math.floor(player.position.y);
        int cz = (int) Math.floor(player.position.z);
        
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist <= radius) {
                        if (hollow && dist < radius - 1) continue;
                        world.setBlockWithState(cx + x, cy + y, cz + z, state.getBlock().getId(), state.getStateId(), false);
                        count++;
                    }
                }
            }
        }
        commandManager.sendMessageInChat("Sphere generated with " + count + " blocks.");
    }

    private void handleCylinder(LocalPlayer player, World world, String[] args, CommandManager commandManager) throws Exception {
        if (args.length != 5) {
            throw new Exception("Usage: /set cylinder <radius> <height> <hollow> <block>");
        }
        int radius = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        boolean hollow = Boolean.parseBoolean(args[3]);
        BlockState state = parseBlockState(args[4]);
        
        int cx = (int) Math.floor(player.position.x);
        int cy = (int) Math.floor(player.position.y);
        int cz = (int) Math.floor(player.position.z);
        
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x*x + z*z);
                if (dist <= radius) {
                    for (int y = 0; y < height; y++) {
                        if (hollow && dist < radius - 1 && y > 0 && y < height - 1) continue;
                        world.setBlockWithState(cx + x, cy + y, cz + z, state.getBlock().getId(), state.getStateId(), false);
                        count++;
                    }
                }
            }
        }
        commandManager.sendMessageInChat("Cylinder generated with " + count + " blocks.");
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"block", "cube", "room", "circle", "sphere", "cylinder"};
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            return completions;
        }

        String subCommand = args[0].toLowerCase();
        int blockIdx = -1;
        
        switch (subCommand) {
            case "block": if (args.length <= 4) addCoordCompletion(completions, player, args.length - 1); else blockIdx = 5; break;
            case "cube":
            case "room": if (args.length <= 7) addCoordCompletion(completions, player, (args.length - 1) % 3 == 0 ? 3 : (args.length - 1) % 3); else blockIdx = 8; break;
            case "circle": if (args.length == 2) completions.add("5"); else blockIdx = 3; break;
            case "sphere": 
                if (args.length == 2) completions.add("5"); 
                else if (args.length == 3) { completions.add("true"); completions.add("false"); }
                else blockIdx = 4; 
                break;
            case "cylinder":
                if (args.length == 2) completions.add("5");
                else if (args.length == 3) completions.add("10");
                else if (args.length == 4) { completions.add("true"); completions.add("false"); }
                else blockIdx = 5;
                break;
        }

        if (args.length == blockIdx) {
            String input = args[args.length - 1].toLowerCase();
            for (String key : BlockRegistry.getAll().keySet()) {
                String shortKey = key.startsWith(Constants.NAMESPACE + ":") ? key.substring(Constants.NAMESPACE.length() + 1) : key;
                if (key.toLowerCase().startsWith(input)) completions.add(key);
                else if (shortKey.toLowerCase().startsWith(input)) completions.add(shortKey);
            }
        }

        List<String> filtered = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();
        for (String c : completions) if (c.toLowerCase().startsWith(current)) filtered.add(c);
        return filtered;
    }

    private void addCoordCompletion(List<String> completions, LocalPlayer player, int axis) {
        completions.add("~");
        
        Vector3i targetPos = player.getInteraction().getSelectedBlockPos();
        Vector3d pos = player.position;
        
        if (targetPos != null) {
            if (axis == 1 || axis == 0) completions.add(String.valueOf(targetPos.x));
            if (axis == 2) completions.add(String.valueOf(targetPos.y));
            if (axis == 3) completions.add(String.valueOf(targetPos.z));
        } else {
            if (axis == 1 || axis == 0) completions.add(String.valueOf((int)Math.floor(pos.x)));
            if (axis == 2) completions.add(String.valueOf((int)Math.floor(pos.y)));
            if (axis == 3) completions.add(String.valueOf((int)Math.floor(pos.z)));
        }
    }
}
