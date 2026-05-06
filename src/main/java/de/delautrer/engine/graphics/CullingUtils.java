package de.delautrer.engine.graphics;
import de.delautrer.engine.graphics.*;
import de.delautrer.game.settings.SettingsManager;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.Map;

public class CullingUtils {
    public static void buildVisibleLists(ChunkManager chunkManager, Matrix4f mvpCameraRelative, RenderPacket packet, boolean isIsoFrame, Vector3d cameraPos) {
        FrustumIntersection frustum = new FrustumIntersection(mvpCameraRelative);
        packet.opaqueMeshes = new ArrayList<>();
        packet.waterMeshes = new ArrayList<>();

        // Wir nutzen double, damit bei der Subtraktion weit draußen kein Bit verloren geht!
        double camX = cameraPos != null ? cameraPos.x : 0.0;
        double camY = cameraPos != null ? cameraPos.y : 0.0;
        double camZ = cameraPos != null ? cameraPos.z : 0.0;

        int playerChunkX = (int) Math.floor(camX / Chunk.SIZE);
        int playerChunkZ = (int) Math.floor(camZ / Chunk.SIZE);

        int dioramaRadius = SettingsManager.get().renderDistance;

        for (Map.Entry<Vector2i, ChunkManager.ChunkMeshPair> entry : chunkManager.getChunkMeshes().entrySet()) {
            int cx = entry.getKey().x;
            int cz = entry.getKey().y;

            boolean isVisible = false;

            if (isIsoFrame) {
                if (cx >= playerChunkX - dioramaRadius && cx <= playerChunkX + dioramaRadius &&
                        cz >= playerChunkZ - dioramaRadius && cz <= playerChunkZ + dioramaRadius) {
                    isVisible = true;
                }
            } else {
                // Präzise Berechnung in Double, das Ergebnis der Subtraktion ist dann eine kleine Zahl!
                double chunkWorldX = cx * (double) Chunk.SIZE;
                double chunkWorldZ = cz * (double) Chunk.SIZE;

                float startX = (float) (chunkWorldX - camX);
                float startY = (float) (0.0 - camY);
                float startZ = (float) (chunkWorldZ - camZ);

                float endX = (float) ((chunkWorldX + Chunk.SIZE) - camX);
                float endY = (float) (Chunk.HEIGHT - camY);
                float endZ = (float) ((chunkWorldZ + Chunk.SIZE) - camZ);

                if (frustum.testAab(startX, startY, startZ, endX, endY, endZ)) {
                    isVisible = true;
                }
            }

            if (isVisible) {
                ChunkManager.ChunkMeshPair pair = entry.getValue();
                if (pair.opaque != null && pair.opaque.getIndexCount() > 0) packet.opaqueMeshes.add(pair.opaque);
                if (pair.water != null && pair.water.getIndexCount() > 0) packet.waterMeshes.add(pair.water);
            }
        }

        // --- 2. ENTITY CULLING ---
        packet.entities = new ArrayList<>();
        float cullRadius = (SettingsManager.get().renderDistance + 1) * 16.0f;
        float entityCullDistSq = cullRadius * cullRadius;
        
        for (de.delautrer.game.entity.Entity entity : chunkManager.getWorld().getEntities()) {
            double ex = entity.position.x;
            double ey = entity.position.y;
            double ez = entity.position.z;

            // 1. Distance Culling
            double dx = ex - camX;
            double dy = ey - camY;
            double dz = ez - camZ;
            if (dx*dx + dy*dy + dz*dz > entityCullDistSq) continue;

            // 2. Frustum Culling (Jetzt mit kleiner AABB für mehr Robustheit)
            float r = 0.5f;
            float h = 2.0f;
            if (frustum.testAab((float)(dx - r), (float)(dy), (float)(dz - r),
                                (float)(dx + r), (float)(dy + h), (float)(dz + r))) {
                packet.entities.add(entity);
            }
        }
    }
}
