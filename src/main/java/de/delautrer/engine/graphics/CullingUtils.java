package de.delautrer.engine.graphics;

import de.delautrer.Constants;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Map;

public class CullingUtils {
    // NEU: Nimmt jetzt die playerPos entgegen!
    public static void buildVisibleLists(ChunkManager chunkManager, Matrix4f mvp, RenderPacket packet, boolean isIsoFrame, Vector3f playerPos) {
        FrustumIntersection frustum = new FrustumIntersection(mvp);
        packet.opaqueMeshes = new ArrayList<>();
        packet.waterMeshes = new ArrayList<>();

        // Berechne den Chunk, in dem der Spieler steht
        int playerChunkX = playerPos != null ? (int) Math.floor(playerPos.x / Chunk.SIZE) : 0;
        int playerChunkZ = playerPos != null ? (int) Math.floor(playerPos.z / Chunk.SIZE) : 0;

        int dioramaRadius = Constants.RENDERDISTANCE;

        for (Map.Entry<Vector2i, ChunkManager.ChunkMeshPair> entry : chunkManager.getChunkMeshes().entrySet()) {
            int cx = entry.getKey().x;
            int cz = entry.getKey().y;

            boolean isVisible = false;

            if (isIsoFrame) {
                // DIORAMA MODUS: Wir zwingen die Engine, ein perfektes Quadrat zu zeichnen!
                if (cx >= playerChunkX - dioramaRadius && cx <= playerChunkX + dioramaRadius &&
                        cz >= playerChunkZ - dioramaRadius && cz <= playerChunkZ + dioramaRadius) {
                    isVisible = true;
                }
            } else {
                // NORMALES CULLING
                if (frustum.testAab(cx * Chunk.SIZE, 0, cz * Chunk.SIZE, (cx + 1) * Chunk.SIZE, Chunk.HEIGHT, (cz + 1) * Chunk.SIZE)) {
                    isVisible = true;
                }
            }

            if (isVisible) {
                ChunkManager.ChunkMeshPair pair = entry.getValue();
                if (pair.opaque != null && pair.opaque.getIndexCount() > 0) packet.opaqueMeshes.add(pair.opaque);
                if (pair.water != null && pair.water.getIndexCount() > 0) packet.waterMeshes.add(pair.water);
            }
        }
    }
}