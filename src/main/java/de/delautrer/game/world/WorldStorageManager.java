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

public class WorldStorageManager {
    private final Path worldDir;
    private final Path regionDir;
    private final Path playerDataDir;

    // Thread-sicherer Cache für geöffnete Region-Dateien
    private final Map<Vector2i, RegionFile> regionCache = new ConcurrentHashMap<>();

    // Warteschlange für den Hintergrund-Thread
    private final ConcurrentLinkedQueue<Chunk> saveQueue = new ConcurrentLinkedQueue<>();

    private Thread writerThread;
    private volatile boolean running = true;
    private final Gson gson;

    public WorldStorageManager(String worldName) {
        this.worldDir = Paths.get("saves", worldName);
        this.regionDir = worldDir.resolve("regions");
        this.playerDataDir = worldDir.resolve("playerdata");

        // Gson für schöne, lesbare JSON-Dateien initialisieren
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

    /**
     * Startet den I/O-Hintergrund-Thread.
     * Alles was mit der Festplatte zu tun hat, passiert HIER, nicht im Main-Thread!
     */
    private void startWriterThread() {
        writerThread = new Thread(() -> {
            while (running || !saveQueue.isEmpty()) {
                Chunk chunk = saveQueue.poll();

                if (chunk != null) {
                    saveChunkToDisk(chunk);
                } else {
                    try {
                        Thread.sleep(50); // CPU schonen, wenn die Queue leer ist
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        writerThread.setName("World-IO-Thread");
        writerThread.setDaemon(true); // Erlaubt das Beenden der JVM, falls der Thread hängt
        writerThread.start();
    }

    /**
     * Rechnet die Chunk-Koordinaten in Region-Koordinaten um (32x32 Chunks pro Region)
     * und holt die offene Datei aus dem Cache (oder öffnet sie neu).
     */
    private synchronized RegionFile getRegionFile(int cx, int cz) {
        // Bitshift um 5 ist exakt das Gleiche wie Division durch 32, nur schneller
        int rx = cx >> 5;
        int rz = cz >> 5;
        Vector2i rPos = new Vector2i(rx, rz);

        return regionCache.computeIfAbsent(rPos, pos -> {
            File file = regionDir.resolve("r." + pos.x + "." + pos.y + ".dat").toFile();
            return new RegionFile(file);
        });
    }

    /**
     * Wirft einen Chunk in die Warteschlange.
     * Der Aufruf kostet praktisch 0 Performance im Main-Thread.
     */
    public void queueChunkForSaving(Chunk chunk) {
        saveQueue.add(chunk);
    }

    /**
     * Die eigentliche Speicher-Logik (läuft im Hintergrund-Thread).
     */
    private void saveChunkToDisk(Chunk chunk) {
        try {
            // 1. Chunk in GZIP Byte-Array komprimieren
            byte[] data = chunk.serialize();

            // 2. Region File holen und Daten schreiben
            RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
            region.writeChunk(chunk.getWorldX(), chunk.getWorldZ(), data);

        } catch (IOException e) {
            System.err.println("Fehler beim Speichern von Chunk " + chunk.getWorldX() + "," + chunk.getWorldZ());
            e.printStackTrace();
        }
    }

    /**
     * Synchrones Laden. Muss sofort passieren, wenn der Chunk-Generator neue Chunks anfordert.
     * @return true wenn der Chunk existiert und geladen wurde, false wenn er neu generiert werden muss.
     */
    public boolean loadChunkFromDisk(Chunk chunk) {
        RegionFile region = getRegionFile(chunk.getWorldX(), chunk.getWorldZ());
        byte[] data = region.readChunk(chunk.getWorldX(), chunk.getWorldZ());

        if (data == null) {
            return false; // Chunk existiert in dieser Region nicht (noch nie generiert)
        }

        try {
            chunk.deserialize(data);
            return true;
        } catch (IOException e) {
            System.err.println("Beschädigter Chunk gefunden bei " + chunk.getWorldX() + "," + chunk.getWorldZ());
            e.printStackTrace();
            return false; // Bei Fehler neu generieren lassen
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
            System.err.println("Fehler beim Laden der level.json!");
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
            System.err.println("Fehler beim Laden der Spielerdaten für " + playerUUID);
            return null;
        }
    }

    // ==========================================
    // CLEANUP / SHUTDOWN
    // ==========================================

    /**
     * Wird beim Schließen des Spiels aufgerufen.
     * Stellt sicher, dass die Warteschlange komplett abgearbeitet wird,
     * bevor sich das Programm beendet!
     */
    public void shutdown() {
        System.out.println("Speichere Welt auf Festplatte... Bitte warten.");
        running = false;
        try {
            writerThread.join(); // Wartet geduldig, bis der Thread fertig ist
            System.out.println("Alle Chunks gespeichert.");

            // Schließe alle geöffneten Dateien (verhindert Memory-Leaks im Dateisystem)
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