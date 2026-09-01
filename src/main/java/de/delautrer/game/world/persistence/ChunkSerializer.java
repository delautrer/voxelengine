package de.delautrer.game.world.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.BlockEntityType;
import de.delautrer.game.blocks.entities.BlockEntityTypeRegistry;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.BlockPos;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkSection;
import de.delautrer.game.world.TickScheduler;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.Biome;
import org.joml.Vector3d;
import org.joml.Vector3i;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkSerializer {

    public static final int MAGIC = 0x564E5343; // 'VNSC'
    public static final int CURRENT_VERSION = 1;

    private static final Map<Byte, NamespacedKey> LEGACY_V0_MAP = new HashMap<>();

    static {
        loadLegacyV0Map();
    }

    private static void loadLegacyV0Map() {
        try {
            Reader reader = ResourceUtils.readResourceToReader("legacy/block_ids_v0.json");
            Gson gson = new Gson();
            Map<String, String> map = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    byte id = (byte) Integer.parseInt(entry.getKey());
                    LEGACY_V0_MAP.put(id, NamespacedKey.fromString(entry.getValue()));
                }
            }
        } catch (Exception e) {
            System.err.println("[ChunkSerializer] Warning: Could not load legacy/block_ids_v0.json: " + e.getMessage());
        }
    }

    public static byte[] serialize(Chunk chunk, World world) throws IOException {
        if (world == null) {
            throw new IllegalArgumentException("World parameter cannot be null when serializing chunk!");
        }

        WorldPalette worldPalette = world.getBlockPalette();
        BiomePalette biomePalette = world.getBiomePalette();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {

            // Header
            dos.writeInt(MAGIC);
            dos.writeInt(CURRENT_VERSION);
            dos.writeInt(chunk.getWorldX());
            dos.writeInt(chunk.getWorldZ());
            dos.writeInt(Chunk.MIN_Y);
            dos.writeInt(Chunk.MAX_Y);
            dos.writeInt(Chunk.NUM_SECTIONS);

            // Write Sections (World Palette short indices)
            ChunkSection[] sections = chunk.getSections();
            for (ChunkSection sec : sections) {
                if (sec != null && !sec.isAir()) {
                    dos.writeBoolean(true);
                    short[] rawBlocks = sec.getBlocks();
                    for (short pIdx : rawBlocks) {
                        dos.writeShort(pIdx);
                    }
                    dos.write(sec.getStates());
                    dos.write(sec.getLightMap());
                } else {
                    dos.writeBoolean(false);
                }
            }

            // Write Biomes (World Biome Palette short indices)
            Biome[] biomeMap = chunk.getBiomeMap();
            for (Biome b : biomeMap) {
                NamespacedKey bKey = b != null ? NamespacedKey.fromString(b.id) : BiomePalette.DEFAULT_BIOME;
                int bIdx = biomePalette.getOrAppend(bKey);
                dos.writeShort(bIdx);
            }

            // Write BlockEntities
            List<BlockEntity> chunkBlockEntities = new ArrayList<>();
            for (Map.Entry<Vector3i, BlockEntity> entry : world.getBlockEntities().entrySet()) {
                Vector3i pos = entry.getKey();
                int cx = pos.x >> 4;
                int cz = pos.z >> 4;
                if (cx == chunk.getWorldX() && cz == chunk.getWorldZ()) {
                    if (entry.getValue() != null) {
                        chunkBlockEntities.add(entry.getValue());
                    }
                }
            }
            dos.writeInt(chunkBlockEntities.size());
            for (BlockEntity be : chunkBlockEntities) {
                dos.writeUTF(be.getType().getKey().toString());
                be.write(dos);
            }

            // Write ItemEntities
            List<ItemEntity> chunkItemEntities = new ArrayList<>();
            for (de.delautrer.game.entity.Entity entity : world.getEntities()) {
                if (entity instanceof ItemEntity ie) {
                    Vector3d pos = ie.position;
                    int cx = (int) Math.floor(pos.x) >> 4;
                    int cz = (int) Math.floor(pos.z) >> 4;
                    if (cx == chunk.getWorldX() && cz == chunk.getWorldZ()) {
                        chunkItemEntities.add(ie);
                    }
                }
            }
            dos.writeInt(chunkItemEntities.size());
            for (ItemEntity ie : chunkItemEntities) {
                Vector3d pos = ie.position;
                dos.writeDouble(pos.x);
                dos.writeDouble(pos.y);
                dos.writeDouble(pos.z);
                ItemStack stack = ie.stack;
                dos.writeUTF(stack != null && stack.type != null ? Registries.ITEMS.getKey(stack.type).toString() : "veinstride:air");
                dos.writeInt(stack != null ? stack.amount : 0);
            }

            // Write Scheduled Ticks
            List<TickScheduler.ScheduledTick> ticks = world.getTickScheduler().getTicksForChunk(chunk.getWorldX(), chunk.getWorldZ());
            dos.writeInt(ticks.size());
            for (TickScheduler.ScheduledTick tick : ticks) {
                dos.writeInt(tick.pos.x);
                dos.writeInt(tick.pos.y);
                dos.writeInt(tick.pos.z);
                NamespacedKey blockKey = Registries.BLOCKS.getKey(tick.block);
                dos.writeUTF(blockKey != null ? blockKey.toString() : "veinstride:air");
                dos.writeLong(tick.triggerTick);
            }
        }
        return baos.toByteArray();
    }

    public static void deserialize(Chunk chunk, byte[] data, World world) throws IOException {
        if (world == null) {
            throw new IllegalArgumentException("World parameter cannot be null when deserializing chunk!");
        }

        WorldPalette worldPalette = world.getBlockPalette();
        BiomePalette biomePalette = world.getBiomePalette();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gzip)) {

            dis.mark(8);
            int firstInt = dis.readInt();

            if (firstInt == MAGIC) {
                // Version 1 format
                int version = dis.readInt();
                if (version != 1) {
                    throw new IOException("Unsupported chunk format version: " + version);
                }

                int savedX = dis.readInt();
                int savedZ = dis.readInt();
                if (savedX != chunk.getWorldX() || savedZ != chunk.getWorldZ()) {
                    throw new IOException("Chunk coordinates mismatch: expected " + chunk.getWorldX() + "," + chunk.getWorldZ() + " got " + savedX + "," + savedZ);
                }

                int minY = dis.readInt();
                int maxY = dis.readInt();
                int numSections = dis.readInt();

                // Read Sections
                ChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < Math.min(numSections, sections.length); i++) {
                    boolean hasData = dis.readBoolean();
                    if (hasData) {
                        sections[i] = new ChunkSection();
                        short[] rawBlocks = sections[i].getBlocks();
                        for (int bIdx = 0; bIdx < ChunkSection.VOLUME; bIdx++) {
                            rawBlocks[bIdx] = dis.readShort();
                        }
                        dis.readFully(sections[i].getStates());
                        dis.readFully(sections[i].getLightMap());
                        sections[i].recalculateAir();
                    } else {
                        sections[i] = null;
                    }
                }

                // Read Biomes
                Biome[] biomeMap = chunk.getBiomeMap();
                for (int i = 0; i < biomeMap.length; i++) {
                    int bIdx = dis.readUnsignedShort();
                    biomeMap[i] = biomePalette.getBiome(bIdx);
                }

                // Read BlockEntities
                try {
                    int beCount = dis.readInt();
                    for (int i = 0; i < beCount; i++) {
                        String typeKeyStr = dis.readUTF();
                        BlockEntityType<?> type = BlockEntityTypeRegistry.REGISTRY.get(NamespacedKey.fromString(typeKeyStr));
                        if (type != null) {
                            BlockEntity be = type.read(dis, world);
                            if (be != null) {
                                world.setBlockEntity(be.getPos(), be);
                            }
                        }
                    }
                } catch (EOFException ignored) {}

                // Read ItemEntities
                try {
                    int ieCount = dis.readInt();
                    for (int i = 0; i < ieCount; i++) {
                        double px = dis.readDouble();
                        double py = dis.readDouble();
                        double pz = dis.readDouble();
                        String itemKeyStr = dis.readUTF();
                        int count = dis.readInt();

                        de.delautrer.game.items.Item item = Registries.ITEMS.get(itemKeyStr);
                        if (item != null && count > 0) {
                            ItemEntity ie = new ItemEntity(new ItemStack(item, count), new Vector3d(px, py, pz), new org.joml.Vector3f(0, 0, 0));
                            world.spawnEntity(ie);
                        }
                    }
                } catch (EOFException ignored) {}

                // Read Scheduled Ticks
                try {
                    int tickCount = dis.readInt();
                    for (int i = 0; i < tickCount; i++) {
                        int tx = dis.readInt();
                        int ty = dis.readInt();
                        int tz = dis.readInt();
                        String bKey = dis.readUTF();
                        long triggerTick = dis.readLong();

                        Block b = Registries.BLOCKS.get(bKey);
                        if (b != null && world.getTickScheduler() != null) {
                            world.getTickScheduler().restoreTick(new BlockPos(tx, ty, tz), b, triggerTick);
                        }
                    }
                } catch (EOFException ignored) {}

            } else {
                // Version 0 Format (Legacy)
                int savedX = firstInt;
                int savedZ = dis.readInt();
                if (savedX != chunk.getWorldX() || savedZ != chunk.getWorldZ()) {
                    throw new IOException("Legacy Chunk-Coordinates mismatch!");
                }

                ChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    boolean hasData = dis.readBoolean();
                    if (hasData) {
                        sections[i] = new ChunkSection();
                        byte[] oldBlocks = new byte[ChunkSection.VOLUME];
                        dis.readFully(oldBlocks);

                        short[] rawBlocks = sections[i].getBlocks();
                        for (int bIdx = 0; bIdx < ChunkSection.VOLUME; bIdx++) {
                            byte oldId = oldBlocks[bIdx];
                            NamespacedKey key = LEGACY_V0_MAP.get(oldId);
                            if (key == null) key = WorldPalette.AIR;
                            int paletteIdx = worldPalette.getOrAppend(key);
                            rawBlocks[bIdx] = (short) paletteIdx;
                        }

                        dis.readFully(sections[i].getStates());
                        dis.readFully(sections[i].getLightMap());
                        sections[i].recalculateAir();
                    } else {
                        sections[i] = null;
                    }
                }

                try {
                    byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
                    dis.readFully(biomeBytes);
                } catch (EOFException ignored) {}
            }
        }
    }
}