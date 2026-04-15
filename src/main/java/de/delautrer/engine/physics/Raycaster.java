package de.delautrer.engine.physics;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Raycaster {

    public static class RaycastResult {
        public final Vector3i hitPos;
        public final Vector3i adjacentPos;

        public RaycastResult(Vector3i hitPos, Vector3i adjacentPos) {
            this.hitPos = hitPos;
            this.adjacentPos = adjacentPos;
        }
    }

    public static RaycastResult raycast(World world, Vector3f start, Vector3f dir, float maxDistance) {
        int x = (int) Math.floor(start.x);
        int y = (int) Math.floor(start.y);
        int z = (int) Math.floor(start.z);

        int stepX = Float.compare(dir.x, 0.0f);
        int stepY = Float.compare(dir.y, 0.0f);
        int stepZ = Float.compare(dir.z, 0.0f);

        float tDeltaX = stepX != 0 ? Math.abs(1.0f / dir.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1.0f / dir.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1.0f / dir.z) : Float.MAX_VALUE;

        float tMaxX = stepX > 0 ? (x + 1.0f - start.x) * tDeltaX : (start.x - x) * tDeltaX;
        float tMaxY = stepY > 0 ? (y + 1.0f - start.y) * tDeltaY : (start.y - y) * tDeltaY;
        float tMaxZ = stepZ > 0 ? (z + 1.0f - start.z) * tDeltaZ : (start.z - z) * tDeltaZ;

        Vector3i lastPos = new Vector3i(x, y, z);
        float dist = 0.0f;

        byte startBlockId = world.getBlockAt(x, y, z);
        Block startBlock = BlockRegistry.get(startBlockId);
        if (startBlock.isRaycastable) {
            return new RaycastResult(new Vector3i(x, y, z), new Vector3i(x, y, z));
        }

        while (dist <= maxDistance) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    lastPos.set(x, y, z);
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    lastPos.set(x, y, z);
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    lastPos.set(x, y, z);
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    lastPos.set(x, y, z);
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }

            if (dist > maxDistance) break;

            byte blockId = world.getBlockAt(x, y, z);
            Block block = BlockRegistry.get(blockId);
            if (block.isRaycastable) {
                return new RaycastResult(new Vector3i(x, y, z), new Vector3i(lastPos));
            }
        }
        return null;
    }
}