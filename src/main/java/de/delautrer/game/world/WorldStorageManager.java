package de.delautrer.game.world;

import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector3f;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.game.world.persistence.RegionFile;
import de.delautrer.game.world.persistence.WorldData;
import de.delautrer.game.world.persistence.PlayerData;

// NEUE IMPORTS FÜR KISTEN UND ITEMS
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
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

    // Der Parameter "folderName" ist hier jetzt der sichere Ordnername (z.B. "Neue_Welt-1")
    public WorldStorageManager(String folderName) {
        this.worldDir = Paths.get("saves", folderName);
        this.regionDir = worldDir.resolve("regions");
        this.playerDataDir = worldDir.resolve("playerdata");

        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(regionDir);
            Files.createDirectories(playerDataDir);
        } catch (IOException e) {
            System.err.println("Kritischer Fehler: Konnte Save-Ordner nicht erstellen!");
            e.printStackTrace();
        }

        startWriterThread();
    }

    // ==========================================
    // STATISCHE HELPER FÜR DAS MENÜ
    // ==========================================

    public static String getUniqueValidFolderName(String rawName) {
        String safeName = ILLEGAL_FILENAME_CHARS.matcher(rawName).replaceAll("_").trim();
        if (safeName.isEmpty() || safeName.equals("_")) {
            safeName = "World";
        }

        Path savesDir = Paths.get("saves");
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

    private synchronized RegionFile getRegionFile(int cx, int cz) {
        int rx = cx >> 5;
        int rz = cz >> 5;
        Vector2i rPos = new Vector2i(rx, rz);

        return regionCache.computeIfAbsent(rPos, pos -> {
            File file = regionDir.resolve("r." + pos.x + "." + pos.y + ".dat").toFile();
            return new RegionFile(file);
        });
    }

    public void queueChunkForSaving(Chunk chunk) {
        saveQueue.add(chunk);
    }

    private void saveChunkToDisk(Chunk chunk) {
        try {
            byte[] data = chunk.serialize();
            RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
            region.writeChunk(chunk.getWorldX(), chunk.getWorldZ(), data);

            // WICHTIGER FIX: Ohne das speichert der Chunk jeden Frame aufs Neue!
            chunk.clearDirty();
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern von Chunk " + chunk.getWorldX() + "," + chunk.getWorldZ());
            e.printStackTrace();
        }
    }

    public boolean loadChunkFromDisk(Chunk chunk) {
        RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
        byte[] data = region.readChunk(chunk.getWorldX(), chunk.getWorldZ());

        if (data == null) return false;

        try {
            chunk.deserialize(data);
            return true;
        } catch (IOException e) {
            System.err.println("Beschädigter Chunk gefunden bei " + chunk.getWorldX() + "," + chunk.getWorldZ());
            return false;
        }
    }

    // ==========================================
    // ENTITIES & BLOCK ENTITIES (NEU)
    // ==========================================

    public void saveBlockEntities(Map<Vector3i, BlockEntity> entities) {
        Path file = worldDir.resolve("block_entities.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.writeInt(entities.size());
            for (Map.Entry<Vector3i, BlockEntity> entry : entities.entrySet()) {
                Vector3i pos = entry.getKey();
                out.writeInt(pos.x); out.writeInt(pos.y); out.writeInt(pos.z);

                if (entry.getValue() instanceof ChestBlockEntity chest) {
                    out.writeUTF("CHEST");
                    saveInventory(out, chest.getInventory());
                } else {
                    out.writeUTF("UNKNOWN");
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadBlockEntities(World world) {
        Path file = worldDir.resolve("block_entities.dat");
        if (!Files.exists(file)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                Vector3i pos = new Vector3i(in.readInt(), in.readInt(), in.readInt());
                String type = in.readUTF();
                if (type.equals("CHEST")) {
                    ChestBlockEntity chest = new ChestBlockEntity(world, pos);
                    loadInventory(in, chest.getInventory());
                    world.setBlockEntity(pos, chest);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void saveEntities(List<Entity> entities) {
        Path file = worldDir.resolve("entities.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            // Wir filtern nur ItemEntities (Drops)
            List<ItemEntity> items = entities.stream()
                    .filter(e -> e instanceof ItemEntity)
                    .map(e -> (ItemEntity) e)
                    .toList();

            out.writeInt(items.size());
            for (ItemEntity item : items) {
                out.writeFloat(item.position.x); out.writeFloat(item.position.y); out.writeFloat(item.position.z);
                out.writeFloat(item.velocity.x); out.writeFloat(item.velocity.y); out.writeFloat(item.velocity.z);
                saveItemStack(out, item.stack);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadEntities(World world) {
        Path file = worldDir.resolve("entities.dat");
        if (!Files.exists(file)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                Vector3f pos = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
                Vector3f vel = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
                ItemStack stack = loadItemStack(in);
                if (stack != null) {
                    world.spawnEntity(new ItemEntity(stack, pos, vel));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // Hilfsmethoden für ItemStacks
    private void saveInventory(DataOutputStream out, de.delautrer.game.inventory.IInventory inv) throws IOException {
        out.writeInt(inv.getSize());
        for (int i = 0; i < inv.getSize(); i++) {
            saveItemStack(out, inv.getStack(i));
        }
    }

    private void loadInventory(DataInputStream in, de.delautrer.game.inventory.IInventory inv) throws IOException {
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            inv.setStack(i, loadItemStack(in));
        }
    }

    private void saveItemStack(DataOutputStream out, ItemStack stack) throws IOException {
        if (stack == null || stack.type == null) {
            out.writeUTF("null");
        } else {
            String id = de.delautrer.game.items.ItemRegistry.getId(stack.type);
            out.writeUTF(id != null ? id : "null");
            out.writeInt(stack.amount);
        }
    }

    private ItemStack loadItemStack(DataInputStream in) throws IOException {
        String id = in.readUTF();
        if (id.equals("null")) return null;
        int amount = in.readInt();
        de.delautrer.game.items.Item item = de.delautrer.game.items.ItemRegistry.get(id);
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
        System.out.println("Speichere Welt auf Festplatte... Bitte warten.");
        running = false;
        try {
            writerThread.join();
            System.out.println("Alle Chunks gespeichert.");

            for (RegionFile region : regionCache.values()) {
                region.close();
            }
            regionCache.clear();

            System.out.println("Welt erfolgreich gesichert!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}