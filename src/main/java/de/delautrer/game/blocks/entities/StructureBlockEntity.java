package de.delautrer.game.blocks.entities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.engine.utils.GamePaths;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.TagIo;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;
import de.delautrer.game.world.generation.structure.dto.StructureTemplateDTO;
import org.joml.Vector3i;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StructureBlockEntity extends BlockEntity {

    private String mode = "save";
    private String name = "";
    private int sizeX = 5, sizeY = 5, sizeZ = 5;
    private int offX = 1, offY = 0, offZ = 1;
    private boolean showBounds = true;

    public StructureBlockEntity(World world, Vector3i pos) {
        super(world, pos);
    }

    @Override
    public BlockEntityType<?> getType() {
        return BlockEntityTypeRegistry.STRUCTURE_BLOCK;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if ("load".equalsIgnoreCase(mode)) {
            this.mode = "load";
        } else {
            this.mode = "save";
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
        if (!this.name.isEmpty()) {
            String cleanName = this.name.toLowerCase();
            NamespacedKey key = cleanName.contains(":") ? NamespacedKey.fromString(cleanName) : NamespacedKey.fromString("veinstride:" + cleanName);
            StructureTemplate template = StructureRegistry.getTemplate(key);
            if (template != null) {
                this.sizeX = template.getSizeX();
                this.sizeY = template.getSizeY();
                this.sizeZ = template.getSizeZ();
            }
        }
    }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    public void setSize(int x, int y, int z) {
        this.sizeX = Math.max(1, Math.min(48, x));
        this.sizeY = Math.max(1, Math.min(48, y));
        this.sizeZ = Math.max(1, Math.min(48, z));
    }

    public int getOffX() { return offX; }
    public int getOffY() { return offY; }
    public int getOffZ() { return offZ; }

    public void setOffset(int x, int y, int z) {
        this.offX = Math.max(-48, Math.min(48, x));
        this.offY = Math.max(-48, Math.min(48, y));
        this.offZ = Math.max(-48, Math.min(48, z));
    }

    public boolean isShowBounds() { return showBounds; }
    public void setShowBounds(boolean showBounds) { this.showBounds = showBounds; }

    @Override
    public void writeTag(CompoundTag tag) {
        super.writeTag(tag);
        tag.putString("mode", mode);
        tag.putString("name", name);
        tag.putInt("sizeX", sizeX);
        tag.putInt("sizeY", sizeY);
        tag.putInt("sizeZ", sizeZ);
        tag.putInt("offX", offX);
        tag.putInt("offY", offY);
        tag.putInt("offZ", offZ);
        tag.putBoolean("showBounds", showBounds);
    }

    @Override
    public void readTag(CompoundTag tag) {
        super.readTag(tag);
        if (tag.contains("mode")) setMode(tag.getString("mode"));
        if (tag.contains("name")) setName(tag.getString("name"));
        if (tag.contains("sizeX") && tag.contains("sizeY") && tag.contains("sizeZ")) {
            setSize(tag.getInt("sizeX"), tag.getInt("sizeY"), tag.getInt("sizeZ"));
        }
        if (tag.contains("offX") && tag.contains("offY") && tag.contains("offZ")) {
            setOffset(tag.getInt("offX"), tag.getInt("offY"), tag.getInt("offZ"));
        }
        if (tag.contains("showBounds")) setShowBounds(tag.getBoolean("showBounds"));
    }

    public String executeSave(World world) {
        if (world == null) return "§cNo world context";
        String cleanName = name.toLowerCase();
        if (!cleanName.matches("^[a-z0-9_]+$")) {
            return "§cInvalid structure name: " + name + ". Must match [a-z0-9_]";
        }

        if ((long) sizeX * sizeY * sizeZ > 48 * 48 * 48) {
            return "§cStructure volume exceeds max size 48x48x48: " + sizeX + "x" + sizeY + "x" + sizeZ;
        }

        int minX = pos.x + offX;
        int minY = pos.y + offY;
        int minZ = pos.z + offZ;

        List<BlockSaveEntry> entries = new ArrayList<>();

        for (int dy = 0; dy < sizeY; dy++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                for (int dx = 0; dx < sizeX; dx++) {
                    int wx = minX + dx;
                    int wy = minY + dy;
                    int wz = minZ + dz;

                    BlockState blockState = world.getBlockState(wx, wy, wz);
                    Block block = blockState != null ? blockState.getBlock() : null;
                    if (block == null || block.isAir()) {
                        continue; // NEVER save air in structure templates!
                    }
                    byte stateId = blockState.getStateId();

                    CompoundTag nbtTag = null;
                    Vector3i blockPos = new Vector3i(wx, wy, wz);
                    BlockEntity be = world.getBlockEntity(blockPos);
                    if (be != null && be != this) {
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

                    entries.add(new BlockSaveEntry(dx, dy, dz, block, stateId, nbtTag, isDependentBlock(block)));
                }
            }
        }

        if (entries.isEmpty()) {
            return "§cNothing to save";
        }

        // Sort entries: solid support blocks first, dependent blocks (torches, plants, doors, etc.) last
        entries.sort((a, b) -> Boolean.compare(a.isDependent, b.isDependent));

        List<String> palette = new ArrayList<>();
        List<StructureTemplateDTO.BlockElementDTO> dtoList = new ArrayList<>();
        List<StructureTemplate.StructureBlock> templateBlocks = new ArrayList<>();

        for (BlockSaveEntry entry : entries) {
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
            Path primaryPath = GamePaths.STRUCTURES_DIR.resolve(cleanName + ".json");
            Files.writeString(primaryPath, jsonStr, StandardCharsets.UTF_8);

            Path devDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "assets", "data", "veinstride", "worldgen", "structure", "template");
            if (Files.exists(devDir)) {
                Files.writeString(devDir.resolve(cleanName + ".json"), jsonStr, StandardCharsets.UTF_8);
            }

            NamespacedKey key = NamespacedKey.fromString("veinstride:" + cleanName);
            StructureTemplate template = new StructureTemplate(key, sizeX, sizeY, sizeZ, templateBlocks);
            StructureRegistry.registerTemplate(key, template);

            return "§aSaved structure veinstride:" + cleanName;
        } catch (Exception e) {
            e.printStackTrace();
            return "§cFailed to save structure: " + e.getMessage();
        }
    }

    public String executeLoad(World world) {
        if (world == null) return "§cNo world context";
        String cleanName = name.toLowerCase();
        if (cleanName.isEmpty()) return "§cStructure name is empty";

        NamespacedKey key = cleanName.contains(":") ? NamespacedKey.fromString(cleanName) : NamespacedKey.fromString("veinstride:" + cleanName);
        StructureTemplate template = StructureRegistry.getTemplate(key);

        if (template == null) {
            return "§cUnknown template: " + name;
        }

        int ox = pos.x + offX;
        int oy = pos.y + offY;
        int oz = pos.z + offZ;

        template.place(world, ox, oy, oz);
        return "§aLoaded structure " + key + " at " + ox + " " + oy + " " + oz;
    }

    public static boolean isDependentBlock(Block block) {
        if (block == null || block.isAir()) return false;
        if (!block.isSolid) return true;
        if (block instanceof de.delautrer.game.blocks.TorchBlock) return true;
        if (block instanceof de.delautrer.game.blocks.PlantBlock) return true;
        String name = block.getClass().getSimpleName();
        return name.contains("Torch") || name.contains("Plant") || name.contains("Door") 
            || name.contains("Button") || name.contains("Lever") || name.contains("Ladder") 
            || name.contains("PressurePlate") || name.contains("Sign") || name.contains("Banner")
            || name.contains("Rail") || name.contains("Redstone") || name.contains("Flower")
            || name.contains("Grass");
    }

    private static class BlockSaveEntry {
        final int dx, dy, dz;
        final Block block;
        final byte stateId;
        final CompoundTag nbtTag;
        final boolean isDependent;

        BlockSaveEntry(int dx, int dy, int dz, Block block, byte stateId, CompoundTag nbtTag, boolean isDependent) {
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
