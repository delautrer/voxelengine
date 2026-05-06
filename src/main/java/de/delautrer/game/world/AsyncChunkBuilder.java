package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanMesh;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK10;

import java.util.concurrent.*;
import java.util.Set;

import de.delautrer.engine.graphics.ChunkMesher;
public class AsyncChunkBuilder {
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private final ConcurrentLinkedQueue<ChunkBuildResult> readyMeshes = new ConcurrentLinkedQueue<>();
    private final Set<Chunk> currentlyBuilding = ConcurrentHashMap.newKeySet();

    public void queueRebuild(Chunk chunk, ChunkManager cm) {
        if (executor.isShutdown() || !currentlyBuilding.add(chunk)) return;

        executor.submit(() -> {
            try {
                // HIER DER FIX: Wir bekommen jetzt beide Meshes zurück
                ChunkMesher.ChunkMeshResult result = chunk.generateMeshData(cm);
                readyMeshes.add(new ChunkBuildResult(chunk, result));
            } finally {
                currentlyBuilding.remove(chunk);
            }
        });
    }

    public void uploadReadyMeshes(ChunkManager cm) {
        if (readyMeshes.isEmpty()) return;
        VK10.vkQueueWaitIdle(cm.getContext().getGraphicsQueue());

        while (!readyMeshes.isEmpty()) {
            ChunkBuildResult result = readyMeshes.poll();
            Vector2i pos = new Vector2i(result.chunk.getWorldX(), result.chunk.getWorldZ());
            cm.updateChunkMeshes(pos, result.data);
        }
    }

    public void cleanup() { executor.shutdown(); }

    private static class ChunkBuildResult {
        public final Chunk chunk;
        public final ChunkMesher.ChunkMeshResult data;
        public ChunkBuildResult(Chunk chunk, ChunkMesher.ChunkMeshResult data) {
            this.chunk = chunk;
            this.data = data;
        }
    }
}
