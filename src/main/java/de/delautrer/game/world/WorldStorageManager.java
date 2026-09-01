package de.delautrer.game.world;

import de.delautrer.engine.utils.GamePaths;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3f;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.game.world.persistence.RegionFile;
import de.delautrer.game.world.persistence.WorldData;
import de.delautrer.game.world.persistence.PlayerData;
import de.delautrer.game.world.persistence.ChunkSerializer;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.blocks.entities.FurnaceBlockEntity;
import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

// NEUE IMPORTS FÜR KISTEN UND ITEMS

public class WorldStorageManager {

    private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final Path worldDir;
    private final Path regionDir;
    private final Path playerDataDir;

    private final Map<Vector2i, RegionFile> regionCache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Chunk> saveQueue = new ConcurrentLinkedQueue<>();

    private Thread writerThread;
    private volatile boolean running = true;
    private final Gson gson;
    private World world;

    // Der Parameter "folderName" ist hier jetzt der sichere Ordnername (z.B. "Neue_Welt-1")
    public WorldStorageManager(String folderName) {
        this(folderName, null);
    }

    public WorldStorageManager(String folderName, World world) {
        this.world = world;
        this.worldDir = GamePaths.SAVES_DIR.resolve(folderName);
        this.regionDir = worldDir.resolve("regions");
        this.playerDataDir = worldDir.resolve("playerdata");

        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(regionDir);
            Files.createDirectories(playerDataDir);
        } catch (IOException e) {
            System.err.println("[WorldStorageManager] Critical error: Could not create save directories: " + e.getMessage());
        }

        startWriterThread();
    }

    public void setWorld(World world) {
        this.world = world;
    }

    // ==========================================
    // STATISCHE HELPER FÜR DAS MENÜ
    // ==========================================

    public static String getUniqueValidFolderName(String rawName) {
        String safeName = ILLEGAL_FILENAME_CHARS.matcher(rawName).replaceAll("_").trim();
        if (safeName.isEmpty() || safeName.equals("_")) {
            safeName = "World";
        }

        Path savesDir = GamePaths.SAVES_DIR;
        if (!Files.exists(savesDir)) {
            try { Files.createDirectories(savesDir); } catch (IOException ignored) {}
        }

        Path target = savesDir.resolve(safeName);
        int counter = 1;
        while (Files.exists(target)) {
            target = savesDir.resolve(safeName + "-" + counter);
            counter++;
        }

        return target.getFileName().toString();
    }

    public static WorldData readMetadataForUI(File saveFolder) {
        Path levelFile = saveFolder.toPath().resolve("level.json");
        if (!Files.exists(levelFile)) return null;

        try {
            Gson tempGson = new Gson();
            String json = Files.readString(levelFile);
            return tempGson.fromJson(json, WorldData.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================================
    // THREADING & CHUNK IO
    // ==========================================

    private void startWriterThread() {
        writerThread = new Thread(() -> {
            while (running || !saveQueue.isEmpty()) {
                Chunk chunk = saveQueue.poll();

                if (chunk != null) {
                    saveChunkToDisk(chunk);
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        writerThread.setName("World-IO-Thread");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    private RegionFile getRegionFile(int cx, int cz) {
        int rx = cx >> 5;
        int rz = cz >> 5;
        Vector2i rPos = new Vector2i(rx, rz);
        return regionCache.computeIfAbsent(rPos, pos -> {
            File file = regionDir.resolve("r." + pos.x + "." + pos.y + ".dat").toFile();
            try {
                return new RegionFile(file);
            } catch (IOException e) {
                System.err.println("[WorldStorageManager] Could not open region file: " + file);
                return null;
            }
        });
    }

    public void queueChunkForSaving(Chunk chunk) {
        saveQueue.add(chunk);
    }

    private void handleCorruptChunk(Chunk chunk, byte[] data) {
        System.err.println("[WorldStorageManager] Corrupt chunk at (" + chunk.getWorldX() + "," + chunk.getWorldZ() + "). Quarantining chunk file!");
        Path bakPath = regionDir.resolve("r." + (chunk.getWorldX() >> 5) + "." + (chunk.getWorldZ() >> 5) + ".dat.corrupt-chunk-c" + chunk.getWorldX() + "-c" + chunk.getWorldZ() + ".bak");
        try {
            if (data != null) {
                Files.write(bakPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(bakPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[WorldStorageManager] Could not write corrupt chunk backup: " + e.getMessage());
        }
        if (world.getChunkManager() != null) {
            world.getChunkManager().markCorruptChunk(chunk.getWorldX(), chunk.getWorldZ());
        }
    }

    private void saveChunkToDisk(Chunk chunk) {
        if (world == null) {
            chunk.markDirty();
            return;
        }
        if (world.getChunkManager() != null && world.getChunkManager().isChunkCorrupt(chunk.getWorldX(), chunk.getWorldZ())) {
            return;
        }
        try {
            byte[] data = ChunkSerializer.serialize(chunk, world);
            RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
            if (region != null) {
                region.writeChunk(chunk.getWorldX(), chunk.getWorldZ(), data);
                chunk.clearDirty();
            } else {
                chunk.markDirty();
            }
        } catch (IOException e) {
            System.err.println("[WorldStorageManager] Error saving chunk (" + chunk.getWorldX() + "," + chunk.getWorldZ() + "): " + e.getMessage());
            e.printStackTrace();
            chunk.markDirty();
        }
    }

    public boolean loadChunkFromDisk(Chunk chunk) {
        if (world == null) {
            throw new IllegalStateException("World instance must be set before loading chunks!");
        }
        RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
        if (region == null) return false;
        RegionFile.ReadResult result = region.readChunk(chunk.getWorldX(), chunk.getWorldZ());

        if (result.type == RegionFile.ReadResultType.MISSING) {
            return false;
        }

        if (result.type == RegionFile.ReadResultType.CORRUPT) {
            handleCorruptChunk(chunk, result.data);
            return false;
        }

        try {
            ChunkSerializer.deserialize(chunk, result.data, world);
            chunk.clearDirty();
            return true;
        } catch (Exception e) {
            System.err.println("[WorldStorageManager] Failed to deserialize chunk at (" + chunk.getWorldX() + "," + chunk.getWorldZ() + "): " + e.getMessage());
            handleCorruptChunk(chunk, result.data);
            return false;
        }
    }

    // ==========================================
    // ENTITIES & BLOCK ENTITIES (NEU)
    // ==========================================

    public void saveBlockEntities(Map<Vector3i, BlockEntity> entities) {
        // Legacy file writing disabled for v1 chunk payloads.
    }

    public void loadBlockEntities(World world) {
        Path file = worldDir.resolve("block_entities.dat");
        Path migratedFile = worldDir.resolve("block_entities.dat.migrated");
        if (Files.exists(migratedFile) || !Files.exists(file)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                Vector3i pos = new Vector3i(in.readInt(), in.readInt(), in.readInt());
                String type = in.readUTF();
                if (type.equals("CHEST")) {
                    ChestBlockEntity chest = new ChestBlockEntity(world, pos);
                    loadInventory(in, chest.getInventory());
                    world.setBlockEntity(pos, chest);
                } else if (type.equals("FURNACE")) {
                    FurnaceBlockEntity furnace = new FurnaceBlockEntity(world, pos);
                    furnace.setBurnTime(in.readInt());
                    furnace.setMaxBurnTime(in.readInt());
                    furnace.setCookTime(in.readInt());
                    loadInventory(in, furnace.getInventory());
                    world.setBlockEntity(pos, furnace);
                }
            }
            Files.move(file, migratedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[WorldStorageManager] Error loading block entities: " + e.getMessage());
        }
    }

    public void saveEntities(List<Entity> entities) {
        // Legacy file writing disabled for v1 chunk payloads.
    }

    public void loadEntities(World world) {
        Path file = worldDir.resolve("entities.dat");
        Path migratedFile = worldDir.resolve("entities.dat.migrated");
        if (Files.exists(migratedFile) || !Files.exists(file)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                Vector3d pos = new Vector3d(in.readFloat(), in.readFloat(), in.readFloat());
                Vector3f vel = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
                ItemStack stack = loadItemStack(in);
                if (stack != null) {
                    world.spawnEntity(new ItemEntity(stack, pos, vel));
                }
            }
            Files.move(file, migratedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[WorldStorageManager] Error loading entities: " + e.getMessage());
        }
    }

    // Hilfsmethoden für ItemStacks
    private void saveInventory(DataOutputStream out, IInventory inv) throws IOException {
        out.writeInt(inv.getSize());
        for (int i = 0; i < inv.getSize(); i++) {
            saveItemStack(out, inv.getStack(i));
        }
    }

    private void loadInventory(DataInputStream in, IInventory inv) throws IOException {
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            inv.setStack(i, loadItemStack(in));
        }
    }

    private void saveItemStack(DataOutputStream out, ItemStack stack) throws IOException {
        if (stack == null || stack.type == null) {
            out.writeUTF("null");
        } else {
            String id = ItemRegistry.getId(stack.type);
            out.writeUTF(id != null ? id : "null");
            out.writeInt(stack.amount);
        }
    }

    private ItemStack loadItemStack(DataInputStream in) throws IOException {
        String id = in.readUTF();
        if (id.equals("null")) return null;
        int amount = in.readInt();
        Item item = ItemRegistry.get(id);
        return item != null ? new ItemStack(item, amount) : null;
    }

    // ==========================================
    // METADATEN (Welt & Spieler)
    // ==========================================

    public void saveLevelMetadata(WorldData data) {
        Path levelFile = worldDir.resolve("level.json");
        try {
            String json = gson.toJson(data);
            Files.writeString(levelFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der level.json!");
        }
    }

    public WorldData loadLevelMetadata() {
        Path levelFile = worldDir.resolve("level.json");
        if (!Files.exists(levelFile)) return null;

        try {
            String json = Files.readString(levelFile);
            return gson.fromJson(json, WorldData.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void savePlayerData(String playerUUID, PlayerData data) {
        Path playerFile = playerDataDir.resolve(playerUUID + ".json");
        try {
            String json = gson.toJson(data);
            Files.writeString(playerFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der Spielerdaten für " + playerUUID);
        }
    }

    public PlayerData loadPlayerData(String playerUUID) {
        Path playerFile = playerDataDir.resolve(playerUUID + ".json");
        if (!Files.exists(playerFile)) return null;

        try {
            String json = Files.readString(playerFile);
            return gson.fromJson(json, PlayerData.class);
        } catch (IOException e) {
            return null;
        }
    }

    // ==========================================
    // CLEANUP / SHUTDOWN
    // ==========================================

    public void shutdown() {
        System.out.println("Saving world data...");
        running = false;
        try {
            if (writerThread != null) {
                writerThread.join(5000);
            }
            System.out.println("All chunks saved.");

            for (RegionFile region : regionCache.values()) {
                region.close();
            }
            regionCache.clear();

            System.out.println("World was successfully saved.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[WorldStorageManager] Error during shutdown: " + e.getMessage());
        }
    }
}
