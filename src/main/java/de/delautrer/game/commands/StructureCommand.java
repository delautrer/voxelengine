package de.delautrer.game.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.engine.utils.GamePaths;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.StructureBlockEntity;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.TagIo;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.ui.chat.ChatComponent;
import de.delautrer.game.ui.chat.ClickEvent;
import de.delautrer.game.ui.chat.Style;
import de.delautrer.game.ui.chat.TextRun;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;
import de.delautrer.game.world.generation.structure.dto.StructureTemplateDTO;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructureCommand implements ICommand {

    @Override
    public String getName() {
        return "structure";
    }

    @Override
    public String getUsage() {
        return "/structure <save|place|load|vsnbt> ...";
    }

    private int parseCoord(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return (int) Math.floor(current);
            return (int) Math.floor(current + Double.parseDouble(arg.substring(1)));
        }
        return Integer.parseInt(arg);
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length == 0) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("place")) {
            executePlace(player, world, args, commandManager);
        } else if (sub.equals("save")) {
            executeSave(player, world, args, commandManager);
        } else if (sub.equals("load")) {
            executeLoadCommand(player, world, args, commandManager);
        } else if (sub.equals("vsnbt")) {
            executeVsnbtCommand(player, world, args, commandManager);
        } else {
            commandManager.sendMessageInChat("Usage: " + getUsage());
        }
    }

    private void executeLoadCommand(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 2) {
            commandManager.sendMessageInChat("Usage: /structure load <name> [x] [y] [z]");
            return;
        }

        String rawId = args[1];
        String name = rawId.toLowerCase();
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        if (!name.matches("^[a-z0-9_]+$")) {
            commandManager.sendMessageInChat("§cInvalid structure name: " + name + ". Must match [a-z0-9_]");
            return;
        }

        Vector3i targetPos = player.getInteraction() != null ? player.getInteraction().getSelectedBlockPos() : null;
        int tx = targetPos != null ? targetPos.x : (int) Math.floor(player.position.x);
        int ty = targetPos != null ? targetPos.y : (int) Math.floor(player.position.y) + 1;
        int tz = targetPos != null ? targetPos.z : (int) Math.floor(player.position.z);

        if (args.length >= 5) {
            try {
                tx = parseCoord(args[2], player.position.x);
                ty = parseCoord(args[3], player.position.y);
                tz = parseCoord(args[4], player.position.z);
            } catch (NumberFormatException e) {
                commandManager.sendMessageInChat("§cInvalid coordinates");
                return;
            }
        }

        Block structBlock = Registries.BLOCKS.get("veinstride:structure_block");
        if (structBlock == null) {
            commandManager.sendMessageInChat("§cStructure block not registered!");
            return;
        }

        world.setBlock(tx, ty, tz, structBlock);
        Vector3i structPos = new Vector3i(tx, ty, tz);
        BlockEntity be = world.getBlockEntity(structPos);

        if (be instanceof de.delautrer.game.blocks.entities.StructureBlockEntity sbe) {
            sbe.setMode("load");
            sbe.setName(name);
            sbe.setOffset(1, 0, 0);

            NamespacedKey key = NamespacedKey.fromString("veinstride:" + name);
            StructureTemplate template = StructureRegistry.getTemplate(key);
            if (template != null) {
                sbe.setSize(template.getSizeX(), template.getSizeY(), template.getSizeZ());
            } else {
                sbe.setSize(5, 5, 5);
            }
        }

        ChatComponent feedback = ChatComponent.parseLegacy("§aStructure block (load) for veinstride:" + name + " at " + tx + " " + ty + " " + tz);
        commandManager.sendMessageInChat(feedback);
    }

    private void executePlace(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 2) {
            commandManager.sendMessageInChat("Usage: /structure place <id> [x] [y] [z]");
            return;
        }

        String rawId = args[1];
        NamespacedKey key = rawId.contains(":") ? NamespacedKey.fromString(rawId) : NamespacedKey.fromString("veinstride:" + rawId);
        StructureTemplate template = StructureRegistry.getTemplate(key);

        if (template == null) {
            commandManager.sendMessageInChat("§cUnknown template: " + rawId);
            return;
        }

        int ox = (int) Math.floor(player.position.x);
        int oy = (int) Math.floor(player.position.y);
        int oz = (int) Math.floor(player.position.z);

        if (args.length >= 5) {
            try {
                ox = parseCoord(args[2], player.position.x);
                oy = parseCoord(args[3], player.position.y);
                oz = parseCoord(args[4], player.position.z);
            } catch (NumberFormatException e) {
                commandManager.sendMessageInChat("§cInvalid coordinates");
                return;
            }
        }

        template.place(world, ox, oy, oz);

        ChatComponent feedback = ChatComponent.parseLegacy("§aPlaced " + key + " at " + ox + " " + oy + " " + oz + " (" +
                template.getSizeX() + "x" + template.getSizeY() + "x" + template.getSizeZ() + ")");
        commandManager.sendMessageInChat(feedback);
    }

    private void executeSave(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 8) {
            commandManager.sendMessageInChat("Usage: /structure save <name> <x1> <y1> <z1> <x2> <y2> <z2>");
            return;
        }

        String name = args[1].toLowerCase();
        if (!name.matches("^[a-z0-9_]+$")) {
            commandManager.sendMessageInChat("§cInvalid structure name: " + name + ". Must match [a-z0-9_]");
            return;
        }

        int x1, y1, z1, x2, y2, z2;
        try {
            x1 = parseCoord(args[2], player.position.x);
            y1 = parseCoord(args[3], player.position.y);
            z1 = parseCoord(args[4], player.position.z);
            x2 = parseCoord(args[5], player.position.x);
            y2 = parseCoord(args[6], player.position.y);
            z2 = parseCoord(args[7], player.position.z);
        } catch (NumberFormatException e) {
            commandManager.sendMessageInChat("§cInvalid coordinates");
            return;
        }

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX > 48 || sizeY > 48 || sizeZ > 48 || (long) sizeX * sizeY * sizeZ > 48 * 48 * 48) {
            commandManager.sendMessageInChat("§cStructure volume exceeds max size 48x48x48: " + sizeX + "x" + sizeY + "x" + sizeZ);
            return;
        }

        List<CmdBlockSaveEntry> entries = new ArrayList<>();

        for (int dy = 0; dy < sizeY; dy++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                for (int dx = 0; dx < sizeX; dx++) {
                    int wx = minX + dx;
                    int wy = minY + dy;
                    int wz = minZ + dz;

                    BlockState blockState = world.getBlockState(wx, wy, wz);
                    Block block = blockState != null ? blockState.getBlock() : null;
                    if (block == null || block.isAir()) {
                        continue; // NEVER save air!
                    }
                    byte stateId = blockState.getStateId();

                    CompoundTag nbtTag = null;
                    Vector3i blockPos = new Vector3i(wx, wy, wz);
                    BlockEntity be = world.getBlockEntity(blockPos);
                    if (be != null) {
                        nbtTag = new CompoundTag();
                        be.writeTag(nbtTag);
                    }

                    Chunk chunk = world.getChunkManager().getChunkAtBlock(wx, wy, wz);
                    if (chunk != null) {
                        CompoundTag chunkTag = chunk.getBlockEntityTag(blockPos);
                        if (chunkTag != null) {
                            if (nbtTag == null) {
                                nbtTag = chunkTag;
                            } else {
                                if (chunkTag.contains("LootTable") && !nbtTag.contains("LootTable")) {
                                    nbtTag.putString("LootTable", chunkTag.getString("LootTable"));
                                }
                                if (chunkTag.contains("LootTableSeed") && !nbtTag.contains("LootTableSeed")) {
                                    nbtTag.putLong("LootTableSeed", chunkTag.getLong("LootTableSeed"));
                                }
                            }
                        }
                    }

                    entries.add(new CmdBlockSaveEntry(dx, dy, dz, block, stateId, nbtTag, StructureBlockEntity.isDependentBlock(block)));
                }
            }
        }

        if (entries.isEmpty()) {
            commandManager.sendMessageInChat("§cNothing to save");
            return;
        }

        // Sort entries: solid support blocks first, dependent blocks (torches, plants, doors, etc.) last
        entries.sort((a, b) -> Boolean.compare(a.isDependent, b.isDependent));

        List<String> palette = new ArrayList<>();
        List<StructureTemplateDTO.BlockElementDTO> dtoList = new ArrayList<>();
        List<StructureTemplate.StructureBlock> templateBlocks = new ArrayList<>();

        for (CmdBlockSaveEntry entry : entries) {
            NamespacedKey blockKey = Registries.BLOCKS.getKey(entry.block);
            String keyStr = blockKey != null ? blockKey.toString() : "veinstride:air";
            int paletteIdx = palette.indexOf(keyStr);
            if (paletteIdx == -1) {
                palette.add(keyStr);
                paletteIdx = palette.size() - 1;
            }

            StructureTemplateDTO.BlockElementDTO elem = new StructureTemplateDTO.BlockElementDTO();
            elem.pos = new int[]{entry.dx, entry.dy, entry.dz};
            elem.state = paletteIdx;
            elem.stateId = entry.stateId & 0xFF;
            if (entry.nbtTag != null) {
                elem.nbt = TagIo.toJson(entry.nbtTag);
            }

            dtoList.add(elem);
            templateBlocks.add(new StructureTemplate.StructureBlock(entry.dx, entry.dy, entry.dz, entry.block, entry.stateId, entry.nbtTag));
        }

        StructureTemplateDTO dto = new StructureTemplateDTO();
        dto.size = new int[]{sizeX, sizeY, sizeZ};
        dto.palette = palette;
        dto.blocks = dtoList;

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String jsonStr = gson.toJson(dto);

        try {
            GamePaths.initDirectories();
            Path primaryPath = GamePaths.STRUCTURES_DIR.resolve(name + ".json");
            Files.writeString(primaryPath, jsonStr, StandardCharsets.UTF_8);

            Path devDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "assets", "data", "veinstride", "worldgen", "structure", "template");
            if (Files.exists(devDir)) {
                Files.writeString(devDir.resolve(name + ".json"), jsonStr, StandardCharsets.UTF_8);
            }

            NamespacedKey key = NamespacedKey.fromString("veinstride:" + name);
            StructureTemplate template = new StructureTemplate(key, sizeX, sizeY, sizeZ, templateBlocks);
            StructureRegistry.registerTemplate(key, template);

            String folderPath = primaryPath.getParent().toAbsolutePath().toString();
            ChatComponent feedback = ChatComponent.parseLegacy("§aSaved " + key + " → ")
                    .append(new TextRun(primaryPath.getFileName().toString(), Style.EMPTY.withColor(0x55FF55).withUnderline(true).withClick(ClickEvent.Action.OPEN_FILE, folderPath)));
            commandManager.sendMessageInChat(feedback);
        } catch (Exception e) {
            commandManager.sendMessageInChat("§cFailed to save structure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addCoordCompletion(List<String> completions, LocalPlayer player, int axis) {
        completions.add("~");

        Vector3i targetPos = player.getInteraction() != null ? player.getInteraction().getSelectedBlockPos() : null;
        Vector3d pos = player.position;

        if (targetPos != null) {
            if (axis == 1 || axis == 0) completions.add(String.valueOf(targetPos.x));
            if (axis == 2) completions.add(String.valueOf(targetPos.y));
            if (axis == 3) completions.add(String.valueOf(targetPos.z));
        } else {
            if (axis == 1 || axis == 0) completions.add(String.valueOf((int) Math.floor(pos.x)));
            if (axis == 2) completions.add(String.valueOf((int) Math.floor(pos.y)));
            if (axis == 3) completions.add(String.valueOf((int) Math.floor(pos.z)));
        }
    }

    private void executeVsnbtCommand(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 2) {
            commandManager.sendMessageInChat("Usage: /structure vsnbt <loottable_id> [x] [y] [z]");
            return;
        }

        String rawTable = args[1];
        Vector3i targetPos = player.getInteraction() != null ? player.getInteraction().getSelectedBlockPos() : null;
        int tx = targetPos != null ? targetPos.x : (int) Math.floor(player.position.x);
        int ty = targetPos != null ? targetPos.y : (int) Math.floor(player.position.y);
        int tz = targetPos != null ? targetPos.z : (int) Math.floor(player.position.z);

        if (args.length >= 5) {
            try {
                tx = parseCoord(args[2], player.position.x);
                ty = parseCoord(args[3], player.position.y);
                tz = parseCoord(args[4], player.position.z);
            } catch (NumberFormatException e) {
                commandManager.sendMessageInChat("§cInvalid coordinates");
                return;
            }
        }

        Vector3i blockPos = new Vector3i(tx, ty, tz);
        Block block = world.getBlock(blockPos);

        if (block == null || block.isAir()) {
            commandManager.sendMessageInChat("§cNo block at " + tx + " " + ty + " " + tz);
            return;
        }

        BlockEntity be = world.getBlockEntity(blockPos);
        boolean isContainer = (be instanceof de.delautrer.game.blocks.entities.ChestBlockEntity
                || be instanceof de.delautrer.game.blocks.entities.FurnaceBlockEntity
                || block.hasBlockEntity());

        if (!isContainer && be == null) {
            commandManager.sendMessageInChat("§cBlock " + (Registries.BLOCKS.getKey(block) != null ? Registries.BLOCKS.getKey(block) : block.getClass().getSimpleName()) + " at " + tx + " " + ty + " " + tz + " has no inventory / BlockEntity!");
            return;
        }

        String resolvedLootTable = rawTable;
        if (!resolvedLootTable.contains(":")) {
            if (resolvedLootTable.startsWith("chests/") || resolvedLootTable.startsWith("blocks/")) {
                resolvedLootTable = "veinstride:" + resolvedLootTable;
            } else {
                resolvedLootTable = "veinstride:chests/" + resolvedLootTable;
            }
        }

        Chunk chunk = world.getChunkManager().getChunkAtBlock(tx, ty, tz);
        if (chunk != null) {
            CompoundTag tag = chunk.getBlockEntityTag(blockPos);
            if (tag == null) tag = new CompoundTag();
            tag.putString("LootTable", resolvedLootTable);
            chunk.setBlockEntityTag(tx, ty, tz, tag);
        }

        if (be != null) {
            CompoundTag beTag = new CompoundTag();
            be.writeTag(beTag);
            beTag.putString("LootTable", resolvedLootTable);
            be.readTag(beTag);
        }

        ChatComponent feedback = ChatComponent.parseLegacy("§aSet LootTable " + resolvedLootTable + " for block at " + tx + " " + ty + " " + tz);
        commandManager.sendMessageInChat(feedback);
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("save".startsWith(input)) results.add("save");
            if ("place".startsWith(input)) results.add("place");
            if ("load".startsWith(input)) results.add("load");
            if ("vsnbt".startsWith(input)) results.add("vsnbt");
            return results;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("vsnbt")) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                for (String key : LootTableManager.getAvailableLootTableKeys()) {
                    if (key.toLowerCase().startsWith(prefix)) {
                        results.add(key);
                    }
                }
            } else if (args.length >= 3 && args.length <= 5) {
                int axis = args.length - 2;
                addCoordCompletion(results, player, axis);
            }
        } else if (sub.equals("place") || sub.equals("load")) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                Set<NamespacedKey> keys = StructureRegistry.getTemplateKeys();
                for (NamespacedKey key : keys) {
                    String keyStr = key.toString();
                    String keyPath = key.getKey();
                    if (keyStr.toLowerCase().startsWith(prefix)) {
                        results.add(keyStr);
                    } else if (keyPath.toLowerCase().startsWith(prefix)) {
                        results.add(keyPath);
                    }
                }
            } else if (args.length >= 3 && args.length <= 5) {
                int axis = args.length - 2;
                addCoordCompletion(results, player, axis);
            }
        } else if (sub.equals("save")) {
            if (args.length >= 3 && args.length <= 8) {
                int axis = (args.length - 2) % 3 == 0 ? 3 : (args.length - 2) % 3;
                addCoordCompletion(results, player, axis);
            }
        }
        return results;
    }

    private static class CmdBlockSaveEntry {
        final int dx, dy, dz;
        final Block block;
        final byte stateId;
        final CompoundTag nbtTag;
        final boolean isDependent;

        CmdBlockSaveEntry(int dx, int dy, int dz, Block block, byte stateId, CompoundTag nbtTag, boolean isDependent) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.block = block;
            this.stateId = stateId;
            this.nbtTag = nbtTag;
            this.isDependent = isDependent;
        }
    }
}
