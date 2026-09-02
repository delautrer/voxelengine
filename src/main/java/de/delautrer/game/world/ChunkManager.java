package de.delautrer.game.world;

import de.delautrer.Constants;
import de.delautrer.engine.graphics.*;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.settings.SettingsManager;
import java.util.*;
import java.util.concurrent.*;
import de.delautrer.engine.graphics.ChunkMesher;

public final class ChunkManager {

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

    public static long packPos(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    private final World world;
    private final WorldGenerator worldGenerator;

    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Long, ChunkMeshPair> meshes = new ConcurrentHashMap<>();

    private final AsyncChunkBuilder asyncBuilder;
    private final ExecutorService chunkExecutor;

    private final ConcurrentLinkedQueue<MeshGenerationResult> meshUploadQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MeshToDelete> trashBin = new ConcurrentLinkedQueue<>();

    private final Set<Long> chunksInPreparation = ConcurrentHashMap.newKeySet();
    private final Set<Long> chunksLoading = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> newlyLoadedQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> corruptChunks = ConcurrentHashMap.newKeySet();

    public void markCorruptChunk(int cx, int cz) {
        corruptChunks.add(packPos(cx, cz));
    }

    public boolean isChunkCorrupt(int cx, int cz) {
        return corruptChunks.contains(packPos(cx, cz));
    }
    private final LightEngine lightEngine;

    private final IGraphicsFactory graphicsFactory;

    private boolean initialLoadComplete = false;
    private final int requiredInitialRadius;
    private boolean isCleanedUp = false;

    private int lastPlayerChunkX = Integer.MAX_VALUE;
    private int lastPlayerChunkZ = Integer.MAX_VALUE;

    public ChunkManager(World world, IGraphicsFactory graphicsFactory) {
        this.requiredInitialRadius = SettingsManager.get().renderDistance;
        this.world = world;
        this.worldGenerator = new WorldGenerator(this.world.getSeed(), this.world.getGeneratorType(), this.world.getGeneratorOptions());
        this.worldGenerator.setPalettes(this.world.getBlockPalette(), this.world.getBiomePalette());
        this.graphicsFactory = graphicsFactory;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.chunkExecutor = Executors.newFixedThreadPool(threads);
        this.lightEngine = new LightEngine(this);
        this.asyncBuilder = new AsyncChunkBuilder();
    }

    // Core Logic
    public void update(double playerX, double playerZ) {
        int pX = (int) Math.floor(playerX) >> 4;
        int pZ = (int) Math.floor(playerZ) >> 4;

        boolean chunkChanged = (pX != lastPlayerChunkX || pZ != lastPlayerChunkZ) || !initialLoadComplete;

        if (chunkChanged) {
            lastPlayerChunkX = pX;
            lastPlayerChunkZ = pZ;

            // 1. CHUNKS ASYNCHRON LADEN UND GENERIEREN
            List<Long> chunksToLoad = new ArrayList<>();
            int dataDistance = SettingsManager.get().renderDistance + 2;
            for (int x = pX - dataDistance; x <= pX + dataDistance; x++) {
                for (int z = pZ - dataDistance; z <= pZ + dataDistance; z++) {
                    long pos = packPos(x, z);
                    if (!chunks.containsKey(pos) && !chunksInPreparation.contains(pos) && !chunksLoading.contains(pos)) {
                        chunksToLoad.add(pos);
                    }
                }
            }

            // Sortieren: Nächste Chunks zuerst
            chunksToLoad.sort(Comparator.comparingDouble(pos -> {
                int cx = (int) (pos >> 32);
                int cz = (int) (long) pos;
                return Math.hypot(cx - pX, cz - pZ);
            }));

            for (long pos : chunksToLoad) {
                chunksLoading.add(pos);
                int finalX = (int) (pos >> 32);
                int finalZ = (int) (long) pos;
                chunkExecutor.submit(() -> {
                    try {
                        Chunk newChunk = new Chunk(finalX, finalZ);
                        newChunk.setPalette(world.getBlockPalette());
                        boolean isLoaded = world.getStorageManager().loadChunkFromDisk(newChunk);
                        if (!isLoaded) {
                            if (!isChunkCorrupt(finalX, finalZ)) {
                                worldGenerator.generate(newChunk);
                                newChunk.calculateSunlight();
                                newChunk.markDirty();
                            } else {
                                System.err.println("[ChunkManager] Skipped terrain generation for corrupt chunk at (" + finalX + "," + finalZ + ")");
                            }
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

            // 4. ALTE MESHES ENTLADEN
            int unloadMeshDistance = SettingsManager.get().renderDistance + 1;
            List<Long> meshesToRemove = new ArrayList<>();
            for (Long pos : meshes.keySet()) {
                int cx = (int) (pos >> 32);
                int cz = (int) (long) pos;
                if (Math.abs(cx - pX) > unloadMeshDistance || Math.abs(cz - pZ) > unloadMeshDistance) {
                    meshesToRemove.add(pos);
                }
            }

            if (!meshesToRemove.isEmpty()) {
                for (Long pos : meshesToRemove) {
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
            List<Long> chunksToRemove = new ArrayList<>();
            for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
                long pos = entry.getKey();
                int cx = (int) (pos >> 32);
                int cz = (int) pos;
                if (Math.abs(cx - pX) > unloadDataDistance || Math.abs(cz - pZ) > unloadDataDistance) {
                    Chunk c = entry.getValue();
                    world.getStorageManager().queueChunkForSaving(c);
                    chunksToRemove.add(pos);
                }
            }
            for (Long pos : chunksToRemove) {
                chunks.remove(pos);
            }
        }

        // 1.5 FERTIGE CHUNKS EINFÜGEN (JETZT GEDROSSELT!)
        Chunk loadedChunk;
        int chunksIntegratedThisFrame = 0;
        int maxChunksToIntegrate = initialLoadComplete ? 1 : 10;

        while (chunksIntegratedThisFrame < maxChunksToIntegrate && (loadedChunk = newlyLoadedQueue.poll()) != null) {
            long pos = packPos(loadedChunk.getWorldX(), loadedChunk.getWorldZ());
            chunks.put(pos, loadedChunk);
            chunksLoading.remove(pos);

            if (loadedChunk.isDirty()) {
                lightEngine.initSkyLightForChunk(loadedChunk);
            }
            lightEngine.stitchChunkBorders(loadedChunk);
            lightEngine.getAndClearDirtiedChunks();

            Chunk nX1 = chunks.get(packPos(loadedChunk.getWorldX() + 1, loadedChunk.getWorldZ()));
            Chunk nX2 = chunks.get(packPos(loadedChunk.getWorldX() - 1, loadedChunk.getWorldZ()));
            Chunk nZ1 = chunks.get(packPos(loadedChunk.getWorldX(), loadedChunk.getWorldZ() + 1));
            Chunk nZ2 = chunks.get(packPos(loadedChunk.getWorldX(), loadedChunk.getWorldZ() - 1));

            Chunk nX1Z1 = chunks.get(packPos(loadedChunk.getWorldX() + 1, loadedChunk.getWorldZ() + 1));
            Chunk nX2Z1 = chunks.get(packPos(loadedChunk.getWorldX() - 1, loadedChunk.getWorldZ() + 1));
            Chunk nX1Z2 = chunks.get(packPos(loadedChunk.getWorldX() + 1, loadedChunk.getWorldZ() - 1));
            Chunk nX2Z2 = chunks.get(packPos(loadedChunk.getWorldX() - 1, loadedChunk.getWorldZ() - 1));

            if (nX1 != null) nX1.requestMeshUpdate();
            if (nX2 != null) nX2.requestMeshUpdate();
            if (nZ1 != null) nZ1.requestMeshUpdate();
            if (nZ2 != null) nZ2.requestMeshUpdate();

            if (nX1Z1 != null) nX1Z1.requestMeshUpdate();
            if (nX2Z1 != null) nX2Z1.requestMeshUpdate();
            if (nX1Z2 != null) nX1Z2.requestMeshUpdate();
            if (nX2Z2 != null) nX2Z2.requestMeshUpdate();

            loadedChunk.requestMeshUpdate();

            chunksIntegratedThisFrame++;
        }

        // 2. MESHES IM HINTERGRUND BERECHNEN (Mit Priorisierung!)
        List<Chunk> chunksToUpdate = new ArrayList<>();
        
        int unloadMeshDist = SettingsManager.get().renderDistance + 1;
        for (Chunk c : chunks.values()) {
            if (c.needsMeshUpdate()) {
                int cx = c.getWorldX();
                int cz = c.getWorldZ();
                if (Math.abs(cx - pX) <= unloadMeshDist && Math.abs(cz - pZ) <= unloadMeshDist) {
                    long pos = packPos(cx, cz);
                    if (!chunksInPreparation.contains(pos)) {
                        chunksToUpdate.add(c);
                    }
                }
            }
        }

        if (!chunksToUpdate.isEmpty()) {
            chunksToUpdate.sort(Comparator.comparingDouble(c -> {
                int cx = c.getWorldX();
                int cz = c.getWorldZ();
                return (cx - pX) * (cx - pX) + (cz - pZ) * (cz - pZ);
            }));

            for (Chunk c : chunksToUpdate) {
                long pos = packPos(c.getWorldX(), c.getWorldZ());
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
        }

        // 3. FERTIGE MESHES AN VULKAN SENDEN
        int maxUploads = initialLoadComplete ? 2 : 20;
        int uploadsThisFrame = 0;

        while (uploadsThisFrame < maxUploads) {
            MeshGenerationResult result = meshUploadQueue.poll();
            if (result == null)
                break;

            long pos = packPos(result.chunk.getWorldX(), result.chunk.getWorldZ());
            chunksInPreparation.remove(pos);

            if (chunks.containsKey(pos)) {
                updateChunkMeshes(pos, result.meshData);
                uploadsThisFrame++;
            }
        }

        // 6. MÜLLEIMER LEEREN (Verzögerte Löschung)
        int itemsToProcess = trashBin.size();
        for (int i = 0; i < itemsToProcess; i++) {
            MeshToDelete item = trashBin.poll();
            if (item != null) {
                item.framesToLive--;
                if (de.delautrer.Constants.VULKAN_DEBUG) {
                    System.out.println("[ChunkManager] Trash bin item 0x" + Integer.toHexString(item.mesh.hashCode()) + " framesToLive: " + item.framesToLive);
                }
                if (item.framesToLive <= 0) {
                    if (de.delautrer.Constants.VULKAN_DEBUG) {
                        System.out.println("[ChunkManager] Trash bin item 0x" + Integer.toHexString(item.mesh.hashCode()) + " expired. Cleaning up!");
                    }
                    item.mesh.cleanup();
                } else {
                    trashBin.add(item); 
                }
            }
        }
    }

    public void updateChunkMeshes(long pos, ChunkMesher.ChunkMeshResult result) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            int cx = (int) (pos >> 32);
            int cz = (int) pos;
            System.out.println("[ChunkManager] updateChunkMeshes for chunk (" + cx + ", " + cz + "). Opaque mesh size: " + result.opaque().vertices().length + " floats, Water mesh size: " + result.water().vertices().length + " floats");
        }
        ChunkMeshPair pair = meshes.computeIfAbsent(pos, k -> new ChunkMeshPair());

        if (pair.opaque != null) {
            trashBin.add(new MeshToDelete(pair.opaque));
        }
        pair.opaque = graphicsFactory.createMesh(result.opaque());
        if (pair.opaque != null) {
            int cx = (int) (pos >> 32);
            int cz = (int) pos;
            pair.opaque.setChunkOffset(cx * Chunk.SIZE, cz * Chunk.SIZE);
        }

        if (pair.water != null) {
            trashBin.add(new MeshToDelete(pair.water));
        }
        pair.water = graphicsFactory.createMesh(result.water());
        if (pair.water != null) {
            int cx = (int) (pos >> 32);
            int cz = (int) pos;
            pair.water.setChunkOffset(cx * Chunk.SIZE, cz * Chunk.SIZE);
        }
    }

    // Getter & Setter
    public Chunk getChunkAtBlock(int worldX, int worldY, int worldZ) {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        Chunk c = chunks.get(packPos(cx, cz));
        if (c != null)
            c.access();
        return c;
    }

    public void addChunk(Chunk chunk) {
        if (chunk != null) {
            long pos = packPos(chunk.getWorldX(), chunk.getWorldZ());
            chunks.put(pos, chunk);
        }
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

    public Map<Long, ChunkMeshPair> getMeshes() {
        return meshes;
    }

    public float getLoadingProgress(double playerX, double playerZ) {
        if (initialLoadComplete)
            return 1.0f;
        int pX = (int) Math.floor(playerX) >> 4;
        int pZ = (int) Math.floor(playerZ) >> 4;
        int requiredChunks = (requiredInitialRadius * 2 + 1) * (requiredInitialRadius * 2 + 1);
        int totalTasks = requiredChunks * 2;
        int completedTasks = 0;
        for (int x = pX - requiredInitialRadius; x <= pX + requiredInitialRadius; x++) {
            for (int z = pZ - requiredInitialRadius; z <= pZ + requiredInitialRadius; z++) {
                long pos = packPos(x, z);
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
        System.out.println("Stopping background chunk threads...");
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Clearing vulkan meshes. Give GPU space to breath");

        for (ChunkMeshPair pair : meshes.values()) {
            pair.cleanup();
        }

        if (asyncBuilder != null)
            asyncBuilder.cleanup();
    }

    public World getWorld() {
        return world;
    }

    private static class MeshGenerationResult {
        public final Chunk chunk;
        public final ChunkMesher.ChunkMeshResult meshData;

        public MeshGenerationResult(Chunk chunk, ChunkMesher.ChunkMeshResult meshData) {
            this.chunk = chunk;
            this.meshData = meshData;
        }
    }

    public Map<Long, ChunkMeshPair> getChunkMeshes() {
        return meshes;
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
