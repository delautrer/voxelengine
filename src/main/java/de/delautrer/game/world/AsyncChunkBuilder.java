package de.delautrer.game.world;

import de.delautrer.engine.graphics.MeshData;
import org.joml.Vector2i;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncChunkBuilder {

    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private final ConcurrentLinkedQueue<ChunkBuildResult> readyMeshes = new ConcurrentLinkedQueue<>();

    // NEU: Ein Set, das sich merkt, ob ein Chunk bereits in der Warteschlange ist
    private final Set<Chunk> currentlyBuilding = ConcurrentHashMap.newKeySet();

    public void queueRebuild(Chunk chunk, ChunkManager cm) {
        // Wenn er schon in der Schlange ist, ignorieren wir den doppelten Auftrag! (Spart massiv CPU)
        if (!currentlyBuilding.add(chunk)) {
            return;
        }

        executor.submit(() -> {
            try {
                MeshData data = chunk.generateMeshData(cm);
                readyMeshes.add(new ChunkBuildResult(chunk, data));
            } finally {
                currentlyBuilding.remove(chunk);
            }
        });
    }

    public void uploadReadyMeshes(ChunkManager cm) {
        if (!readyMeshes.isEmpty()) {
            org.lwjgl.vulkan.VK10.vkQueueWaitIdle(cm.getContext().getGraphicsQueue());

            while (!readyMeshes.isEmpty()) {
                ChunkBuildResult result = readyMeshes.poll();

                org.joml.Vector2i pos = new org.joml.Vector2i(result.chunk.getWorldX(), result.chunk.getWorldZ());
                de.delautrer.engine.graphics.VulkanMesh mesh = cm.getMeshes().get(pos);

                if (mesh != null) {
                    mesh.updateMesh(result.data);
                }
            }
        }
    }

    public void cleanup() {
        executor.shutdown();
    }

    private static class ChunkBuildResult {
        public final Chunk chunk;
        public final MeshData data;
        public ChunkBuildResult(Chunk chunk, MeshData data) {
            this.chunk = chunk;
            this.data = data;
        }
    }
}