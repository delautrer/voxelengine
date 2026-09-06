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

    private final ExecutorService chunkExecutor;

    private final ConcurrentLinkedQueue<MeshGenerationResult> meshUploadQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MeshToDelete> trashBin = new ConcurrentLinkedQueue<>();

    private final Set<Long> chunksLighting = ConcurrentHashMap.newKeySet();
    private final Set<Long> chunksMeshing = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingMeshUpdates = ConcurrentHashMap.newKeySet();
    private final Set<Long> chunksLoading = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> newlyLoadedQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> corruptChunks = ConcurrentHashMap.newKeySet();
    private volatile boolean hadImmediateRebuildThisFrame = false;

    public void rebuildChunkMeshImmediate(Chunk c) {
        if (c == null) return;
        long pos = packPos(c.getWorldX(), c.getWorldZ());
        if (!chunks.containsKey(pos)) return;

        c.incrementMeshEpoch();
        ChunkMesher.ChunkMeshResult result = c.generateMeshData(this);
        updateChunkMeshes(pos, result);
        c.clearMeshUpdate();
        pendingMeshUpdates.remove(pos);
        hadImmediateRebuildThisFrame = true;
    }

    public void requestMeshUpdate(Chunk c) {
        if (c != null) {
            c.requestMeshUpdate();
            long pos = packPos(c.getWorldX(), c.getWorldZ());
            if (chunks.containsKey(pos)) {
                pendingMeshUpdates.add(pos);
            }
        }
    }

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
    }

    // Core Logic
    public void update(double playerX, double playerZ) {
        int pX = (int) Math.floor(playerX) >> 4;
        int pZ = (int) Math.floor(playerZ) >> 4;

        boolean chunkChanged = (pX != lastPlayerChunkX || pZ != lastPlayerChunkZ) || !initialLoadComplete;

        if (chunkChanged) {
            lastPlayerChunkX = pX;
            lastPlayerChunkZ = pZ;

            // Safety-Net: Geladene Chunks in Mesh-Distanz ohne Mesh oder mit update-Bedarf queueen (gedrosselt)
            if (!hadImmediateRebuildThisFrame && pendingMeshUpdates.size() < 8) {
                int scanMeshDist = SettingsManager.get().renderDistance + 1;
                int addedCount = 0;
                for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
                    if (addedCount >= 32) break;
                    long pos = entry.getKey();
                    Chunk c = entry.getValue();
                    int cx = (int) (pos >> 32);
                    int cz = (int) pos;
                    if (Math.abs(cx - pX) <= scanMeshDist && Math.abs(cz - pZ) <= scanMeshDist) {
                        if (!meshes.containsKey(pos) || c.needsMeshUpdate()) {
                            if (!chunksLighting.contains(pos) && !chunksMeshing.contains(pos)) {
                                requestMeshUpdate(c);
                                addedCount++;
                            }
                        }
                    }
                }
            }

            // 1. CHUNKS ASYNCHRON LADEN UND GENERIEREN
            List<Long> chunksToLoad = new ArrayList<>();
            int dataDistance = SettingsManager.get().renderDistance + 2;
            for (int x = pX - dataDistance; x <= pX + dataDistance; x++) {
                for (int z = pZ - dataDistance; z <= pZ + dataDistance; z++) {
                    long pos = packPos(x, z);
                    if (!chunks.containsKey(pos) && !chunksLighting.contains(pos) && !chunksMeshing.contains(pos) && !chunksLoading.contains(pos)) {
                        chunksToLoad.add(pos);
                    }
                }
            }

            // Sortieren: Nächste Chunks zuerst
            chunksToLoad.sort(Comparator.comparingDouble(pos -> {
                int cx = (int) (pos.longValue() >> 32);
                int cz = pos.intValue();
                return Math.hypot(cx - pX, cz - pZ);
            }));

            for (long pos : chunksToLoad) {
                chunksLoading.add(pos);
                int finalX = (int) (pos >> 32);
                int finalZ = (int) pos;
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
                        requestMeshUpdate(c);
                }
            }

            // 5. ALTE CHUNK-DATEN ENTLADEN
            int unloadDataDistance = SettingsManager.get().renderDistance + 3;
            List<Long> chunksToRemove = new ArrayList<>();
            for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
                long pos = entry.getKey();
                if (chunksLighting.contains(pos) || chunksMeshing.contains(pos) || chunksLoading.contains(pos)) {
                    continue;
                }
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
                pendingMeshUpdates.remove(pos);
            }
        }

        // 1.5 FERTIGE CHUNKS EINFÜGEN (JETZT GEDROSSELT & ASYNC LICHT!)
        Chunk loadedChunk;
        int chunksIntegratedThisFrame = 0;
        int maxChunksToIntegrate = initialLoadComplete ? 1 : 10;

        while (chunksIntegratedThisFrame < maxChunksToIntegrate && (loadedChunk = newlyLoadedQueue.poll()) != null) {
            long pos = packPos(loadedChunk.getWorldX(), loadedChunk.getWorldZ());
            chunks.put(pos, loadedChunk);
            chunksLoading.remove(pos);
            chunksLighting.add(pos);

            final Chunk cToLight = loadedChunk;
            chunkExecutor.submit(() -> {
                try {
                    if (!chunks.containsKey(pos)) {
                        return;
                    }
                    if (cToLight.isDirty()) {
                        lightEngine.initSkyLightForChunk(cToLight);
                    }
                    lightEngine.stitchChunkBorders(cToLight);
                    Set<Chunk> dirtied = lightEngine.getAndClearDirtiedChunks();
                    for (Chunk dirtiedChunk : dirtied) {
                        requestMeshUpdate(dirtiedChunk);
                    }
                    requestMeshUpdate(cToLight);

                    int cx = cToLight.getWorldX();
                    int cz = cToLight.getWorldZ();
                    long[] neighborPositions = {
                        packPos(cx + 1, cz),
                        packPos(cx - 1, cz),
                        packPos(cx, cz + 1),
                        packPos(cx, cz - 1)
                    };
                    for (long nPos : neighborPositions) {
                        Chunk n = chunks.get(nPos);
                        if (n != null) {
                            requestMeshUpdate(n);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Fehler beim Chunk-Licht-Job: " + e.getMessage());
                } finally {
                    chunksLighting.remove(pos);
                    if (cToLight.needsMeshUpdate()) {
                        pendingMeshUpdates.add(pos);
                    }
                }
            });

            chunksIntegratedThisFrame++;
        }

        // 2. MESHES IM HINTERGRUND BERECHNEN (Mit Budget & Priorisierung!)
        if (!pendingMeshUpdates.isEmpty()) {
            List<Long> candidates = new ArrayList<>();
            int unloadMeshDist = SettingsManager.get().renderDistance + 1;

            for (Iterator<Long> it = pendingMeshUpdates.iterator(); it.hasNext(); ) {
                Long pos = it.next();
                Chunk c = chunks.get(pos);
                if (c == null || !c.needsMeshUpdate()) {
                    it.remove();
                    continue;
                }
                int cx = c.getWorldX();
                int cz = c.getWorldZ();
                if (Math.abs(cx - pX) <= unloadMeshDist && Math.abs(cz - pZ) <= unloadMeshDist) {
                    if (!chunksLighting.contains(pos) && !chunksMeshing.contains(pos)) {
                        candidates.add(pos);
                    }
                } else {
                    it.remove();
                }
            }

            if (!candidates.isEmpty()) {
                candidates.sort(Comparator.comparingDouble(pos -> {
                    int cx = (int) (pos >> 32);
                    int cz = pos.intValue();
                    return (cx - pX) * (cx - pX) + (cz - pZ) * (cz - pZ);
                }));

                int maxJobs = initialLoadComplete ? 2 : 8;
                int jobsSubmitted = 0;

                for (Long pos : candidates) {
                    if (jobsSubmitted >= maxJobs) break;
                    Chunk c = chunks.get(pos);
                    if (c == null || chunksLighting.contains(pos) || chunksMeshing.contains(pos)) continue;

                    pendingMeshUpdates.remove(pos);
                    c.clearMeshUpdate();
                    chunksMeshing.add(pos);
                    jobsSubmitted++;

                    int taskEpoch = c.getMeshEpoch();
                    chunkExecutor.submit(() -> {
                        try {
                            if (chunks.containsKey(pos)) {
                                ChunkMesher.ChunkMeshResult result = c.generateMeshData(this);
                                if (c.getMeshEpoch() == taskEpoch) {
                                    meshUploadQueue.add(new MeshGenerationResult(c, result, taskEpoch));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Fehler beim Chunk-Meshing: " + e.getMessage());
                            c.requestMeshUpdate();
                            pendingMeshUpdates.add(pos);
                        } finally {
                            chunksMeshing.remove(pos);
                            if (c.needsMeshUpdate()) {
                                pendingMeshUpdates.add(pos);
                            }
                        }
                    });
                }
            }
        }

        // 3. FERTIGE MESHES AN VULKAN SENDEN (Mit Byte-Cap & Chunk-Cap)
        int maxUploads = initialLoadComplete ? (hadImmediateRebuildThisFrame ? 1 : 2) : 20;
        long maxUploadBytes = initialLoadComplete ? 512 * 1024L : Long.MAX_VALUE;
        int uploadsThisFrame = 0;
        long bytesUploadedThisFrame = 0;

        while (uploadsThisFrame < maxUploads && bytesUploadedThisFrame < maxUploadBytes) {
            MeshGenerationResult result = meshUploadQueue.poll();
            if (result == null)
                break;

            long pos = packPos(result.chunk.getWorldX(), result.chunk.getWorldZ());
            chunksMeshing.remove(pos);

            if (chunks.containsKey(pos) && result.chunk.getMeshEpoch() == result.epoch) {
                updateChunkMeshes(pos, result.meshData);
                long uploadedBytes = (long) result.meshData.opaque().vertices().length * Float.BYTES
                        + (long) result.meshData.water().vertices().length * Float.BYTES;
                bytesUploadedThisFrame += uploadedBytes;
                if (result.meshData.opaque().indices().length > 0 || result.meshData.water().indices().length > 0) {
                    uploadsThisFrame++;
                }
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

        // Reset immediate rebuild flag at the very end of frame update
        hadImmediateRebuildThisFrame = false;
    }

    public void updateChunkMeshes(long pos, ChunkMesher.ChunkMeshResult result) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            int cx = (int) (pos >> 32);
            int cz = (int) pos;
            System.out.println("[ChunkManager] updateChunkMeshes for chunk (" + cx + ", " + cz + "). Opaque mesh size: " + result.opaque().vertices().length + " floats, Water mesh size: " + result.water().vertices().length + " floats");
        }
        ChunkMeshPair pair = meshes.computeIfAbsent(pos, k -> new ChunkMeshPair());
        int cx = (int) (pos >> 32);
        int cz = (int) pos;

        if (result.opaque().indices().length > 0) {
            if (pair.opaque == null) {
                pair.opaque = graphicsFactory.createMesh(result.opaque());
            } else {
                pair.opaque.updateMesh(result.opaque().vertices(), result.opaque().indices());
            }
            if (pair.opaque != null) {
                pair.opaque.setChunkOffset(cx * Chunk.SIZE, cz * Chunk.SIZE);
            }
        } else if (pair.opaque != null) {
            pair.opaque.updateMesh(result.opaque().vertices(), result.opaque().indices());
        }

        if (result.water().indices().length > 0) {
            if (pair.water == null) {
                pair.water = graphicsFactory.createMesh(result.water());
            } else {
                pair.water.updateMesh(result.water().vertices(), result.water().indices());
            }
            if (pair.water != null) {
                pair.water.setChunkOffset(cx * Chunk.SIZE, cz * Chunk.SIZE);
            }
        } else if (pair.water != null) {
            pair.water.updateMesh(result.water().vertices(), result.water().indices());
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

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(packPos(chunkX, chunkZ));
    }

    public void addChunk(Chunk chunk) {
        if (chunk != null) {
            long pos = packPos(chunk.getWorldX(), chunk.getWorldZ());
            chunks.put(pos, chunk);
        }
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
    }

    public World getWorld() {
        return world;
    }

    private static class MeshGenerationResult {
        public final Chunk chunk;
        public final ChunkMesher.ChunkMeshResult meshData;
        public final int epoch;

        public MeshGenerationResult(Chunk chunk, ChunkMesher.ChunkMeshResult meshData, int epoch) {
            this.chunk = chunk;
            this.meshData = meshData;
            this.epoch = epoch;
        }
    }

    public Map<Long, ChunkMeshPair> getChunkMeshes() {
        return meshes;
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
