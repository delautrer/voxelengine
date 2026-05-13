package de.delautrer.game.world;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.buffer.VulkanMesh;
import de.delautrer.game.settings.SettingsManager;
import org.joml.Vector2i;
import java.util.*;
import java.util.concurrent.*;
import de.delautrer.engine.graphics.ChunkMesher;

public class ChunkManager {

    // Hilfsklasse, um beide Mesh-Typen pro Chunk zu verwalten
    public static class ChunkMeshPair {
        public IMesh opaque;
        public IMesh water;

        public void cleanup() {
            if (opaque != null)
                opaque.cleanup();
            if (water != null)
                water.cleanup();
        }
    }

    private static class MeshToDelete {
        final IMesh mesh;
        int framesToLive = 3;

        MeshToDelete(IMesh mesh) {
            this.mesh = mesh;
        }
    }

    private final World world;
    private final WorldGenerator worldGenerator;

    private final Map<Vector2i, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Vector2i, ChunkMeshPair> meshes = new ConcurrentHashMap<>();

    private final AsyncChunkBuilder asyncBuilder;
    private final ExecutorService chunkExecutor;

    private final ConcurrentLinkedQueue<MeshGenerationResult> meshUploadQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MeshToDelete> trashBin = new ConcurrentLinkedQueue<>();

    private final Set<Vector2i> chunksInPreparation = ConcurrentHashMap.newKeySet();
    private final Set<Vector2i> chunksLoading = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> newlyLoadedQueue = new ConcurrentLinkedQueue<>();
    private final LightEngine lightEngine;

    private final IGraphicsFactory graphicsFactory;

    private boolean initialLoadComplete = false;
    private final int requiredInitialRadius;
    private boolean isCleanedUp = false;

    @SuppressWarnings("this-escape")
    public ChunkManager(World world, IGraphicsFactory graphicsFactory) {
        this.requiredInitialRadius = SettingsManager.get().renderDistance;
        this.world = world;
        this.worldGenerator = new WorldGenerator(this.world.getSeed());
        this.graphicsFactory = graphicsFactory;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.chunkExecutor = Executors.newFixedThreadPool(threads);
        this.lightEngine = new LightEngine(this);
        this.asyncBuilder = new AsyncChunkBuilder();
    }

    // Core Logic
    public void update(double playerX, double playerZ) {
        int pX = Math.floorDiv((int) Math.floor(playerX), Chunk.SIZE);
        int pZ = Math.floorDiv((int) Math.floor(playerZ), Chunk.SIZE);

        // 1. CHUNKS ASYNCHRON LADEN UND GENERIEREN (Mit Priorisierung!)
        List<Vector2i> chunksToLoad = new ArrayList<>();
        int dataDistance = SettingsManager.get().renderDistance + 2;
        for (int x = pX - dataDistance; x <= pX + dataDistance; x++) {
            for (int z = pZ - dataDistance; z <= pZ + dataDistance; z++) {
                Vector2i pos = new Vector2i(x, z);
                if (!chunks.containsKey(pos) && !chunksLoading.contains(pos)) {
                    chunksToLoad.add(pos);
                }
            }
        }

        // Sortieren: Nächste Chunks zuerst
        chunksToLoad.sort(Comparator.comparingDouble(pos -> pos.distanceSquared(pX, pZ)));

        for (Vector2i pos : chunksToLoad) {
            chunksLoading.add(pos);
            int finalX = pos.x;
            int finalZ = pos.y;
            chunkExecutor.submit(() -> {
                try {
                    Chunk newChunk = new Chunk(finalX, finalZ);
                    boolean isLoaded = world.getStorageManager().loadChunkFromDisk(newChunk);
                    if (!isLoaded) {
                        worldGenerator.generate(newChunk);
                        newChunk.calculateSunlight();
                        newChunk.markDirty();
                    } else {
                        newChunk.clearDirty();
                    }
                    newlyLoadedQueue.add(newChunk);
                } catch (Exception e) {
                    System.err.println("Fehler beim Chunk-Laden: " + e.getMessage());
                    chunksLoading.remove(pos);
                }
            });
        }

        // 1.5 FERTIGE CHUNKS EINFÜGEN (JETZT GEDROSSELT!)
        Chunk loadedChunk;
        int chunksIntegratedThisFrame = 0;
        int maxChunksToIntegrate = initialLoadComplete ? 1 : 10;

        while (chunksIntegratedThisFrame < maxChunksToIntegrate && (loadedChunk = newlyLoadedQueue.poll()) != null) {
            Vector2i pos = new Vector2i(loadedChunk.getWorldX(), loadedChunk.getWorldZ());
            chunks.put(pos, loadedChunk);
            chunksLoading.remove(pos);

            if (loadedChunk.isDirty()) {
                lightEngine.initSkyLightForChunk(loadedChunk);
            }
            lightEngine.stitchChunkBorders(loadedChunk);
            lightEngine.getAndClearDirtiedChunks();

            Chunk nX1 = chunks.get(new Vector2i(pos.x + 1, pos.y));
            Chunk nX2 = chunks.get(new Vector2i(pos.x - 1, pos.y));
            Chunk nZ1 = chunks.get(new Vector2i(pos.x, pos.y + 1));
            Chunk nZ2 = chunks.get(new Vector2i(pos.x, pos.y - 1));

            if (nX1 != null)
                nX1.requestMeshUpdate();
            if (nX2 != null)
                nX2.requestMeshUpdate();
            if (nZ1 != null)
                nZ1.requestMeshUpdate();
            if (nZ2 != null)
                nZ2.requestMeshUpdate();

            loadedChunk.requestMeshUpdate();

            chunksIntegratedThisFrame++;
        }

        // 2. MESHES IM HINTERGRUND BERECHNEN (Mit Priorisierung!)
        List<Vector2i> chunksToUpdate = new ArrayList<>();
        int renderDistance = SettingsManager.get().renderDistance;
        
        for (int x = pX - renderDistance; x <= pX + renderDistance; x++) {
            for (int z = pZ - renderDistance; z <= pZ + renderDistance; z++) {
                Vector2i pos = new Vector2i(x, z);
                Chunk c = chunks.get(pos);
                if (c != null && !chunksInPreparation.contains(pos)) {
                    if (!meshes.containsKey(pos) || c.needsMeshUpdate()) {
                        chunksToUpdate.add(pos);
                    }
                }
            }
        }

        // Sortieren: Nächste Chunks zuerst
        chunksToUpdate.sort(Comparator.comparingDouble(pos -> pos.distanceSquared(pX, pZ)));

        for (Vector2i pos : chunksToUpdate) {
            Chunk c = chunks.get(pos);
            c.clearMeshUpdate();
            chunksInPreparation.add(pos);
            chunkExecutor.submit(() -> {
                try {
                    ChunkMesher.ChunkMeshResult result = c.generateMeshData(this);
                    meshUploadQueue.add(new MeshGenerationResult(c, result));
                } catch (Exception e) {
                    System.err.println("Fehler beim Chunk-Meshing: " + e.getMessage());
                    chunksInPreparation.remove(pos);
                }
            });
        }

        // 3. FERTIGE MESHES AN VULKAN SENDEN
        int maxUploads = initialLoadComplete ? 2 : 20;
        int uploadsThisFrame = 0;

        // NEU: Ein Schalter, damit wir pro Frame maximal einmal auf die GPU warten
        // müssen
        // boolean waitIdleCalled = false;

        while (uploadsThisFrame < maxUploads) {
            MeshGenerationResult result = meshUploadQueue.poll();
            if (result == null)
                break;

            Vector2i pos = new Vector2i(result.chunk.getWorldX(), result.chunk.getWorldZ());
            chunksInPreparation.remove(pos);

            if (chunks.containsKey(pos)) {
                /*
                 * if (meshes.containsKey(pos) && !waitIdleCalled) {
                 * VK10.vkDeviceWaitIdle(context.getDevice());
                 * waitIdleCalled = true;
                 * }
                 */
                updateChunkMeshes(pos, result.meshData);
                uploadsThisFrame++;
            }
        }

        // 4. ALTE MESHES ENTLADEN (Ohne GPU Stall!)
        int unloadMeshDistance = SettingsManager.get().renderDistance + 1;
        List<Vector2i> meshesToRemove = new ArrayList<>();
        for (Vector2i pos : meshes.keySet()) {
            if (Math.abs(pos.x - pX) > unloadMeshDistance || Math.abs(pos.y - pZ) > unloadMeshDistance) {
                meshesToRemove.add(pos);
            }
        }

        if (!meshesToRemove.isEmpty()) {
            for (Vector2i pos : meshesToRemove) {
                ChunkMeshPair pair = meshes.remove(pos);
                if (pair != null) {
                    if (pair.opaque != null)
                        trashBin.add(new MeshToDelete(pair.opaque));
                    if (pair.water != null)
                        trashBin.add(new MeshToDelete(pair.water));
                }

                Chunk c = chunks.get(pos);
                if (c != null)
                    c.clearMeshCache();
            }
        }

        // 5. ALTE CHUNK-DATEN ENTLADEN
        int unloadDataDistance = SettingsManager.get().renderDistance + 3;
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

        // 6. MÜLLEIMER LEEREN (Verzögerte Löschung)
        int itemsToProcess = trashBin.size();
        for (int i = 0; i < itemsToProcess; i++) {
            MeshToDelete item = trashBin.poll();
            if (item != null) {
                item.framesToLive--;
                if (item.framesToLive <= 0) {
                    item.mesh.cleanup(); // Jetzt ist es sicher! Die GPU nutzt es nicht mehr.
                } else {
                    trashBin.add(item); // Noch nicht tot genug, wieder hinten anstellen
                }
            }
        }
    }

    /**
     * Zentrale Methode, um die Meshes eines Chunks zu aktualisieren oder neu zu
     * erstellen.
     * Wird sowohl vom ChunkManager-Update als auch vom AsyncChunkBuilder genutzt.
     */
    public void updateChunkMeshes(Vector2i pos, ChunkMesher.ChunkMeshResult result) {
        ChunkMeshPair pair = meshes.computeIfAbsent(pos, k -> new ChunkMeshPair());

        if (pair.opaque != null) {
            trashBin.add(new MeshToDelete(pair.opaque));
        }
        pair.opaque = graphicsFactory.createMesh(result.opaque());
        if (pair.opaque instanceof VulkanMesh vm) {
            vm.chunkOffsetX = pos.x * Chunk.SIZE;
            vm.chunkOffsetZ = pos.y * Chunk.SIZE;
        }

        if (pair.water != null) {
            trashBin.add(new MeshToDelete(pair.water));
        }
        pair.water = graphicsFactory.createMesh(result.water());
        if (pair.water instanceof VulkanMesh vm) {
            vm.chunkOffsetX = pos.x * Chunk.SIZE;
            vm.chunkOffsetZ = pos.y * Chunk.SIZE;
        }
    }

    // Getter & Setter
    public Chunk getChunkAtBlock(int worldX, int worldY, int worldZ) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        Chunk c = chunks.get(new Vector2i(cx, cz));
        if (c != null)
            c.access();
        return c;
    }

    public AsyncChunkBuilder getAsyncBuilder() {
        return asyncBuilder;
    }

    public IGraphicsFactory getGraphicsFactory() {
        return graphicsFactory;
    }

    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    public LightEngine getLightEngine() {
        return lightEngine;
    }

    // Die Meshes Map gibt jetzt die Paare zurück
    public Map<Vector2i, ChunkMeshPair> getMeshes() {
        return meshes;
    }

    public float getLoadingProgress(double playerX, double playerZ) {
        if (initialLoadComplete)
            return 1.0f;
        int pX = Math.floorDiv((int) Math.floor(playerX), Chunk.SIZE);
        int pZ = Math.floorDiv((int) Math.floor(playerZ), Chunk.SIZE);
        int requiredChunks = (requiredInitialRadius * 2 + 1) * (requiredInitialRadius * 2 + 1);
        int totalTasks = requiredChunks * 2;
        int completedTasks = 0;
        for (int x = pX - requiredInitialRadius; x <= pX + requiredInitialRadius; x++) {
            for (int z = pZ - requiredInitialRadius; z <= pZ + requiredInitialRadius; z++) {
                Vector2i pos = new Vector2i(x, z);
                if (chunks.containsKey(pos))
                    completedTasks++;
                if (meshes.containsKey(pos))
                    completedTasks++;
            }
        }
        float progress = (float) completedTasks / totalTasks;
        if (progress >= 1.0f)
            initialLoadComplete = true;
        return progress;
    }

    public boolean isInitialLoadComplete() {
        return initialLoadComplete;
    }

    public void cleanup() {
        if (isCleanedUp)
            return;
        isCleanedUp = true;
        System.out.println("[Thread] Stopping background chunk threads...");
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[Thread] Clearing vulkan meshes. Give GPU space to breath");

        for (ChunkMeshPair pair : meshes.values()) {
            pair.cleanup();
        }

        if (asyncBuilder != null)
            asyncBuilder.cleanup();
    }

    public World getWorld() {
        return world;
    }

    // Kleine Hilfsklasse für die Upload-Queue
    private static class MeshGenerationResult {
        public final Chunk chunk;
        public final ChunkMesher.ChunkMeshResult meshData;

        public MeshGenerationResult(Chunk chunk, ChunkMesher.ChunkMeshResult meshData) {
            this.chunk = chunk;
            this.meshData = meshData;
        }
    }

    public Map<Vector2i, ChunkMeshPair> getChunkMeshes() {
        return meshes;
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
