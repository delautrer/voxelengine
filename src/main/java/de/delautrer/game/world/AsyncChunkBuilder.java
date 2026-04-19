package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanMesh;
import org.joml.Vector2i;
import java.util.concurrent.*;
import java.util.Set;

public class AsyncChunkBuilder {
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private final ConcurrentLinkedQueue<ChunkBuildResult> readyMeshes = new ConcurrentLinkedQueue<>();
    private final Set<Chunk> currentlyBuilding = ConcurrentHashMap.newKeySet();

    public void queueRebuild(Chunk chunk, ChunkManager cm) {
        if (executor.isShutdown() || !currentlyBuilding.add(chunk)) return;

        executor.submit(() -> {
            try {
                // HIER DER FIX: Wir bekommen jetzt beide Meshes zurück
                Chunk.ChunkMeshResult result = chunk.generateMeshData(cm);
                readyMeshes.add(new ChunkBuildResult(chunk, result));
            } finally {
                currentlyBuilding.remove(chunk);
            }
        });
    }

    public void uploadReadyMeshes(ChunkManager cm) {
        if (readyMeshes.isEmpty()) return;
        org.lwjgl.vulkan.VK10.vkQueueWaitIdle(cm.getContext().getGraphicsQueue());

        while (!readyMeshes.isEmpty()) {
            ChunkBuildResult result = readyMeshes.poll();
            Vector2i pos = new Vector2i(result.chunk.getWorldX(), result.chunk.getWorldZ());

            // HIER DER FIX: Beide Meshes im Manager updaten
            cm.updateChunkMeshes(pos, result.data);
        }
    }

    public void cleanup() { executor.shutdown(); }

    private static class ChunkBuildResult {
        public final Chunk chunk;
        public final Chunk.ChunkMeshResult data;
        public ChunkBuildResult(Chunk chunk, Chunk.ChunkMeshResult data) {
            this.chunk = chunk;
            this.data = data;
        }
    }
}