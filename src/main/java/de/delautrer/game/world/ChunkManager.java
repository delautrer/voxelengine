package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanMesh;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ChunkManager {

    private final WorldGenerator worldGenerator = new WorldGenerator();
    private final Map<Vector2i, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Vector2i, VulkanMesh> meshes = new ConcurrentHashMap<>();

    private final AsyncChunkBuilder asyncBuilder;
    private final ExecutorService chunkExecutor;
    private final ConcurrentLinkedQueue<Chunk> meshUploadQueue = new ConcurrentLinkedQueue<>();
    private final java.util.Set<Vector2i> chunksInPreparation = ConcurrentHashMap.newKeySet();
    private final LightEngine lightEngine;

    private final VulkanContext context;
    private final int renderDistance = 3;

    public ChunkManager(VulkanContext context) {
        this.context = context;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.chunkExecutor = Executors.newFixedThreadPool(threads);
        this.lightEngine = new LightEngine(this);
        this.asyncBuilder = new AsyncChunkBuilder();
    }

    public void update(float playerX, float playerZ) {
        int pX = (int) Math.floor(playerX / Chunk.SIZE);
        int pZ = (int) Math.floor(playerZ / Chunk.SIZE);

        // 1. CHUNKS INITIALISIEREN (Daten anlegen oder aus Speicher laden)
        int dataDistance = renderDistance + 1;

        java.util.List<Chunk> newlyGeneratedChunks = new java.util.ArrayList<>();

        for (int x = pX - dataDistance; x <= pX + dataDistance; x++) {
            for (int z = pZ - dataDistance; z <= pZ + dataDistance; z++) {
                Vector2i pos = new Vector2i(x, z);
                if (!chunks.containsKey(pos)) {
                    Chunk newChunk = new Chunk(x, z);
                    chunks.put(pos, newChunk);
                    worldGenerator.generate(newChunk);
                    newChunk.calculateSunlight();
                    newlyGeneratedChunks.add(newChunk);
                }
            }
        }

        for (Chunk c : newlyGeneratedChunks) {
            lightEngine.initSkyLightForChunk(c);
        }

        // 2. MESHES IM HINTERGRUND BERECHNEN (Multi-Threading)
        for (int x = pX - renderDistance; x <= pX + renderDistance; x++) {
            for (int z = pZ - renderDistance; z <= pZ + renderDistance; z++) {
                Vector2i pos = new Vector2i(x, z);

                if (!meshes.containsKey(pos) && !chunksInPreparation.contains(pos)) {
                    chunksInPreparation.add(pos);
                    Chunk c = chunks.get(pos);

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

        // 3. FERTIGE MESHES AN VULKAN SENDEN (Haupt-Thread)
        int uploadsThisFrame = 0;
        while (uploadsThisFrame < 2) {
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

        // 4. ALTE CHUNKS ENTLADEN (Mesh-Cleanup)
        int unloadDistance = renderDistance + 1;
        List<Vector2i> meshesToRemove = new ArrayList<>();

        for (Vector2i pos : meshes.keySet()) {
            if (Math.abs(pos.x - pX) > unloadDistance || Math.abs(pos.y - pZ) > unloadDistance) {
                meshesToRemove.add(pos);
            }
        }

        if (!meshesToRemove.isEmpty()) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            for (Vector2i pos : meshesToRemove) {
                VulkanMesh oldMesh = meshes.remove(pos);
                if (oldMesh != null) {
                    oldMesh.cleanup();
                }

                Chunk c = chunks.get(pos);
                if (c != null) {
                    c.clearMeshCache();
                }
            }
        }
    }

    public java.util.Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    public Map<Vector2i, VulkanMesh> getMeshes() { return meshes; }

    public LightEngine getLightEngine() { return lightEngine; }

    public Chunk getChunkAtBlock(int worldX, int worldY, int worldZ) {
        int cx = (int) Math.floor((float)worldX / Chunk.SIZE);
        int cz = (int) Math.floor((float)worldZ / Chunk.SIZE);
        return chunks.get(new Vector2i(cx, cz));
    }

    public AsyncChunkBuilder getAsyncBuilder() { return asyncBuilder; }
    public VulkanContext getContext() {
        return context;
    }

    public void cleanup() {
        chunkExecutor.shutdownNow();
        VK10.vkDeviceWaitIdle(context.getDevice());
        for (VulkanMesh mesh : meshes.values()) mesh.cleanup();
        meshes.clear();
        chunks.clear();
        if (asyncBuilder != null) asyncBuilder.cleanup();
    }
}