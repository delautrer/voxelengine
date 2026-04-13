package de.delautrer.game.world;

import de.delautrer.engine.graphics.MeshData;
import org.joml.Vector2i;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncChunkBuilder {

    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private final ConcurrentLinkedQueue<ChunkBuildResult> readyMeshes = new ConcurrentLinkedQueue<>();

    public void queueRebuild(Chunk chunk, ChunkManager cm) {
        executor.submit(() -> {
            MeshData data = chunk.generateMeshData(cm);
            readyMeshes.add(new ChunkBuildResult(chunk, data));
        });
    }

    public void uploadReadyMeshes(ChunkManager cm) {
        while (!readyMeshes.isEmpty()) {
            ChunkBuildResult result = readyMeshes.poll();

            Vector2i pos = new Vector2i(result.chunk.getWorldX(), result.chunk.getWorldZ());
            de.delautrer.engine.graphics.VulkanMesh mesh = cm.getMeshes().get(pos);

            if (mesh != null) {
                mesh.updateMesh(result.data);
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