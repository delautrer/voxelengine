package de.delautrer.engine.graphics;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import java.util.ArrayList;
import java.util.Map;

public class CullingUtils {
    public static void buildVisibleLists(ChunkManager chunkManager, Matrix4f mvp, RenderPacket packet) {
        FrustumIntersection frustum = new FrustumIntersection(mvp);
        packet.opaqueMeshes = new ArrayList<>();
        packet.waterMeshes = new ArrayList<>();

        // HIER WIRD getChunkMeshes() aufgerufen:
        for (Map.Entry<Vector2i, ChunkManager.ChunkMeshPair> entry : chunkManager.getChunkMeshes().entrySet()) {
            int cx = entry.getKey().x;
            int cz = entry.getKey().y;

            if (frustum.testAab(cx * Chunk.SIZE, 0, cz * Chunk.SIZE, (cx + 1) * Chunk.SIZE, Chunk.HEIGHT, (cz + 1) * Chunk.SIZE)) {
                ChunkManager.ChunkMeshPair pair = entry.getValue();
                if (pair.opaque != null && pair.opaque.getIndexCount() > 0) packet.opaqueMeshes.add(pair.opaque);
                if (pair.water != null && pair.water.getIndexCount() > 0) packet.waterMeshes.add(pair.water);
            }
        }
    }
}