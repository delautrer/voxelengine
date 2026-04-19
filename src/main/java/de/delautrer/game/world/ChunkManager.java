package de.delautrer.game.world;

import de.delautrer.Constants;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanMesh;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK10;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {

    private final World world;
    private final WorldGenerator worldGenerator;

    private final Map<Vector2i, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Vector2i, VulkanMesh> meshes = new ConcurrentHashMap<>();

    private final AsyncChunkBuilder asyncBuilder;
    private final ExecutorService chunkExecutor;
    private final ConcurrentLinkedQueue<Chunk> meshUploadQueue = new ConcurrentLinkedQueue<>();
    private final Set<Vector2i> chunksInPreparation = ConcurrentHashMap.newKeySet();
    private final Set<Vector2i> chunksLoading = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> newlyLoadedQueue = new ConcurrentLinkedQueue<>();
    private final LightEngine lightEngine;

    private final VulkanContext context;

    private boolean initialLoadComplete = false;
    private final int requiredInitialRadius = 4;
    private boolean isCleanedUp = false;

    public ChunkManager(World world, VulkanContext context) {
        this.world = world;
        this.worldGenerator = new WorldGenerator(this.world.getSeed());
        this.context = context;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.chunkExecutor = Executors.newFixedThreadPool(threads);
        this.lightEngine = new LightEngine(this);
        this.asyncBuilder = new AsyncChunkBuilder();
    }

    // Core Logic
    public void update(float playerX, float playerZ) {
        int pX = Math.floorDiv((int) Math.floor(playerX), Chunk.SIZE);
        int pZ = Math.floorDiv((int) Math.floor(playerZ), Chunk.SIZE);

        // 1. CHUNKS ASYNCHRON LADEN UND GENERIEREN
        int dataDistance = Constants.RENDERDISTANCE + 2;

        for (int x = pX - dataDistance; x <= pX + dataDistance; x++) {
            for (int z = pZ - dataDistance; z <= pZ + dataDistance; z++) {
                Vector2i pos = new Vector2i(x, z);

                // Wenn der Chunk noch nicht existiert UND auch noch nicht geladen wird...
                if (!chunks.containsKey(pos) && !chunksLoading.contains(pos)) {
                    chunksLoading.add(pos); // Sofort markieren!

                    // AB IN DEN HINTERGRUND-THREAD! (Befreit den Main-Thread)
                    int finalX = x;
                    int finalZ = z;
                    chunkExecutor.submit(() -> {
                        try {
                            Chunk newChunk = new Chunk(finalX, finalZ);

                            // Festplatte lesen (Dauert lange -> Perfekt für den Hintergrund!)
                            boolean isLoaded = world.getStorageManager().loadChunkFromDisk(newChunk);

                            if (!isLoaded) {
                                // Noise berechnen (CPU intensiv -> Perfekt für den Hintergrund!)
                                worldGenerator.generate(newChunk);
                                newChunk.calculateSunlight();
                                newChunk.markDirty(); // Markieren, damit er später auf die SSD kommt
                            } else {
                                newChunk.clearDirty(); // Kam von SSD, ist sicher.
                            }

                            // Fertig! Ab in die Postbox für den Main-Thread
                            newlyLoadedQueue.add(newChunk);

                        } catch (Exception e) {
                            System.err.println("Fehler beim Chunk-Laden: " + e.getMessage());
                            e.printStackTrace();
                            chunksLoading.remove(pos);
                        }
                    });
                }
            }
        }

        // 1.5 FERTIGE CHUNKS AUS DER POSTBOX IN DIE WELT EINFÜGEN
        // (Das passiert wieder auf dem Main-Thread, dauert aber nur 0.001 Millisekunden!)
        Chunk loadedChunk;
        while ((loadedChunk = newlyLoadedQueue.poll()) != null) {
            Vector2i pos = new Vector2i(loadedChunk.getWorldX(), loadedChunk.getWorldZ());

            chunks.put(pos, loadedChunk);
            chunksLoading.remove(pos);

            if (loadedChunk.isDirty()) {
                lightEngine.initSkyLightForChunk(loadedChunk);
            }
        }

        // 2. MESHES IM HINTERGRUND BERECHNEN
        for (int x = pX - Constants.RENDERDISTANCE; x <= pX + Constants.RENDERDISTANCE; x++) {
            for (int z = pZ - Constants.RENDERDISTANCE; z <= pZ + Constants.RENDERDISTANCE; z++) {
                Vector2i pos = new Vector2i(x, z);

                if (!meshes.containsKey(pos) && !chunksInPreparation.contains(pos)) {
                    Chunk c = chunks.get(pos);

                    if (c != null) {
                        chunksInPreparation.add(pos);
                        chunkExecutor.submit(() -> {
                            try {
                                c.generateMeshData(this);
                                meshUploadQueue.add(c);
                            } catch (Exception e) {
                                System.err.println("Fehler beim Chunk-Meshing: " + e.getMessage());
                                chunksInPreparation.remove(pos);
                            }
                        });
                    }
                }
            }
        }

        // 3. FERTIGE MESHES AN VULKAN SENDEN
        int maxUploads = initialLoadComplete ? 2 : 20;
        int uploadsThisFrame = 0;

        while (uploadsThisFrame < maxUploads) {
            Chunk finishedChunk = meshUploadQueue.poll();
            if (finishedChunk == null) break;

            Vector2i pos = new Vector2i(finishedChunk.getWorldX(), finishedChunk.getWorldZ());
            chunksInPreparation.remove(pos);

            if (chunks.containsKey(pos)) {
                if (meshes.containsKey(pos)) {
                    meshes.get(pos).updateMesh(finishedChunk);
                } else {
                    meshes.put(pos, new VulkanMesh(context, finishedChunk));
                }
                uploadsThisFrame++;
            }
        }

        // 4. ALTE MESHES ENTLADEN (Vulkan-Cleanup)
        int unloadMeshDistance = Constants.RENDERDISTANCE + 1;
        List<Vector2i> meshesToRemove = new ArrayList<>();

        for (Vector2i pos : meshes.keySet()) {
            if (Math.abs(pos.x - pX) > unloadMeshDistance || Math.abs(pos.y - pZ) > unloadMeshDistance) {
                meshesToRemove.add(pos);
            }
        }

        if (!meshesToRemove.isEmpty()) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            for (Vector2i pos : meshesToRemove) {
                VulkanMesh oldMesh = meshes.remove(pos);
                if (oldMesh != null) oldMesh.cleanup();

                Chunk c = chunks.get(pos);
                if (c != null) c.clearMeshCache();
            }
        }

        // 5. ALTE CHUNK-DATEN ENTLADEN (RAM-Cleanup)
        int unloadDataDistance = Constants.RENDERDISTANCE + 3;
        List<Vector2i> chunksToRemove = new ArrayList<>();

        for (Map.Entry<Vector2i, Chunk> entry : chunks.entrySet()) {
            Vector2i pos = entry.getKey();
            if (Math.abs(pos.x - pX) > unloadDataDistance || Math.abs(pos.y - pZ) > unloadDataDistance) {
                Chunk c = entry.getValue();
                world.getStorageManager().queueChunkForSaving(c);
                chunksToRemove.add(pos);
            }
        }

        for (Vector2i pos : chunksToRemove) {
            chunks.remove(pos);
        }
    }

    // Getter & Setter
    public Chunk getChunkAtBlock(int worldX, int worldY, int worldZ) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        Chunk c = chunks.get(new Vector2i(cx, cz));
        if (c != null) {
            c.access();
        }
        return c;
    }

    public AsyncChunkBuilder getAsyncBuilder() { return asyncBuilder; }
    public VulkanContext getContext() {
        return context;
    }
    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }
    public LightEngine getLightEngine() { return lightEngine; }
    public Map<Vector2i, VulkanMesh> getMeshes() { return meshes; }

    // Gibt einen Wert zwischen 0.0f (0%) und 1.0f (100%) zurück
    public float getLoadingProgress(float playerX, float playerZ) {
        if (initialLoadComplete) return 1.0f;

        int pX = Math.floorDiv((int) Math.floor(playerX), Chunk.SIZE);
        int pZ = Math.floorDiv((int) Math.floor(playerZ), Chunk.SIZE);

        int requiredChunks = (requiredInitialRadius * 2 + 1) * (requiredInitialRadius * 2 + 1);

        // JEDER Chunk hat 2 Phasen: 1. Daten generieren, 2. 3D-Mesh bauen
        int totalTasks = requiredChunks * 2;
        int completedTasks = 0;

        for (int x = pX - requiredInitialRadius; x <= pX + requiredInitialRadius; x++) {
            for (int z = pZ - requiredInitialRadius; z <= pZ + requiredInitialRadius; z++) {
                Vector2i pos = new Vector2i(x, z);

                // 1 Punkt für fertige Blockdaten (aus Noise oder von SSD)
                if (chunks.containsKey(pos)) completedTasks++;

                // 1 Punkt für das fertige 3D-Mesh auf der Grafikkarte
                if (meshes.containsKey(pos)) completedTasks++;
            }
        }

        float progress = (float) completedTasks / totalTasks;

        if (progress >= 1.0f) {
            initialLoadComplete = true; // Fertig!
        }
        return progress;
    }

    public boolean isInitialLoadComplete() {
        return initialLoadComplete;
    }

    // Cleanup
    public void cleanup() {
        if (isCleanedUp) return;
        isCleanedUp = true;

        System.out.println("Stoppe Chunk-Hintergrund-Threads...");
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Räume Vulkan-Meshes auf...");
        VK10.vkDeviceWaitIdle(context.getDevice());

        for (VulkanMesh mesh : meshes.values()) {
            if (mesh != null) mesh.cleanup();
        }

        if (asyncBuilder != null) {
            asyncBuilder.cleanup();
        }
    }

    public World getWorld() {
        return world;
    }
}