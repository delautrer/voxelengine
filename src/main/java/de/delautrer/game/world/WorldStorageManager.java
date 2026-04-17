package de.delautrer.game.world;

import org.joml.Vector2i;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.delautrer.game.world.persistence.RegionFile;
import de.delautrer.game.world.persistence.WorldData;
import de.delautrer.game.world.persistence.PlayerData;

import java.io.*;
import java.nio.file.*;
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
    // STATISCHE HELPER FÜR DAS MENÜ (NEU!)
    // ==========================================

    /**
     * Erstellt einen garantierten, fehlerfreien und einmaligen Ordnernamen für eine neue Welt.
     */
    public static String getUniqueValidFolderName(String rawName) {
        // 1. Verbotene Zeichen durch Unterstriche ersetzen
        String safeName = ILLEGAL_FILENAME_CHARS.matcher(rawName).replaceAll("_").trim();
        if (safeName.isEmpty() || safeName.equals("_")) {
            safeName = "World";
        }

        Path savesDir = Paths.get("saves");
        if (!Files.exists(savesDir)) {
            try { Files.createDirectories(savesDir); } catch (IOException ignored) {}
        }

        // 2. Prüfen ob der Ordner existiert. Wenn ja: Nummern anhängen (-1, -2, etc.)
        Path target = savesDir.resolve(safeName);
        int counter = 1;
        while (Files.exists(target)) {
            target = savesDir.resolve(safeName + "-" + counter);
            counter++;
        }

        return target.getFileName().toString();
    }

    /**
     * Liest die WorldData extrem schnell aus, OHNE den ganzen Manager zu starten.
     * Perfekt für die Welt-Auswahl im Hauptmenü!
     */
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