package de.delautrer.engine.graphics;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CullingUtils {

    public static List<VulkanMesh> getVisibleMeshes(ChunkManager chunkManager, Matrix4f mvp) {
        FrustumIntersection frustum = new FrustumIntersection(mvp);
        List<VulkanMesh> visibleMeshes = new ArrayList<>();

        for (Map.Entry<Vector2i, VulkanMesh> entry : chunkManager.getMeshes().entrySet()) {
            int cx = entry.getKey().x;
            int cz = entry.getKey().y;

            float minX = cx * Chunk.SIZE;
            float minY = 0.0f;
            float minZ = cz * Chunk.SIZE;
            float maxX = minX + Chunk.SIZE;
            float maxY = Chunk.HEIGHT;
            float maxZ = minZ + Chunk.SIZE;

            if (frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
                visibleMeshes.add(entry.getValue());
            }
        }
        return visibleMeshes;
    }
}