package de.delautrer.engine.physics;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class Raycaster {

    public static class RaycastResult {
        public final Vector3i hitPos;
        public final Vector3i adjacentPos;
        public final Vector3i hitFace;
        public final Vector3f exactHit;

        public RaycastResult(Vector3i hitPos, Vector3i adjacentPos, Vector3f exactHit) {
            this.hitPos = hitPos;
            this.adjacentPos = adjacentPos;
            this.hitFace = new Vector3i(adjacentPos.x - hitPos.x, adjacentPos.y - hitPos.y, adjacentPos.z - hitPos.z);
            this.exactHit = exactHit;
        }
    }

    public static RaycastResult raycast(World world, Vector3f start, Vector3f dir, float maxDistance) {
        return raycast(world, start, dir, maxDistance, false);
    }

    public static RaycastResult raycast(World world, Vector3f start, Vector3f dir, float maxDistance, boolean holdingVoid) {
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

        while (dist <= maxDistance) {
            Block block = world.getBlock(x, y, z);

            boolean isVoid = block instanceof de.delautrer.game.blocks.StructureVoidBlock;
            if (block != null && block.isRaycastable && (!isVoid || holdingVoid)) {
                BlockState state = world.getBlockState(x, y, z);
                List<AABB> boxes = block.getBoundingBoxes(state);

                // Wir testen, ob der Ray WIRKLICH eine der Boxen trifft (oder durch die Lücke geht)
                float closestHitDist = Float.MAX_VALUE;
                Vector3i bestNormal = null;

                for (AABB box : boxes) {
                    AABB worldBox = new AABB(new Vector3f(box.min).add(x, y, z), new Vector3f(box.max).add(x, y, z));
                    Vector3i normal = new Vector3i();
                    float hitDist = intersectRayAABB(start, dir, worldBox, normal);

                    if (hitDist >= 0 && hitDist < closestHitDist && hitDist <= maxDistance) {
                        closestHitDist = hitDist;
                        bestNormal = normal;
                    }
                }

                if (closestHitDist != Float.MAX_VALUE) {
                    Vector3f exactHit = new Vector3f(dir).mul(closestHitDist).add(start);
                    Vector3i adjPos = new Vector3i(x + bestNormal.x, y + bestNormal.y, z + bestNormal.z);
                    Vector3i hitPos = new Vector3i(x, y, z);

                    // Spezial-Logik für Türen: Immer die untere Position zurückgeben, um Highlight-Blipping zu vermeiden
                    if (block.getClass().getSimpleName().equals("DoorBlock")) {
                        if (state.getValue(de.delautrer.game.blocks.DoorBlock.HALF) == de.delautrer.game.blocks.state.BlockProperties.Half.TOP) {
                            hitPos.y -= 1;
                        }
                    }

                    return new RaycastResult(hitPos, adjPos, exactHit);
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    lastPos.set(x, y, z); x += stepX; dist = tMaxX; tMaxX += tDeltaX;
                } else {
                    lastPos.set(x, y, z); z += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    lastPos.set(x, y, z); y += stepY; dist = tMaxY; tMaxY += tDeltaY;
                } else {
                    lastPos.set(x, y, z); z += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    // Mathe-Magie: Berechnet exakt, ob und wo ein Ray eine Box trifft
    private static float intersectRayAABB(Vector3f origin, Vector3f dir, AABB box, Vector3i outNormal) {
        float tmin = (box.min.x - origin.x) / dir.x;
        float tmax = (box.max.x - origin.x) / dir.x;
        if (tmin > tmax) { float temp = tmin; tmin = tmax; tmax = temp; }

        float tymin = (box.min.y - origin.y) / dir.y;
        float tymax = (box.max.y - origin.y) / dir.y;
        if (tymin > tymax) { float temp = tymin; tymin = tymax; tymax = temp; }

        if ((tmin > tymax) || (tymin > tmax)) return -1;
        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        float tzmin = (box.min.z - origin.z) / dir.z;
        float tzmax = (box.max.z - origin.z) / dir.z;
        if (tzmin > tzmax) { float temp = tzmin; tzmin = tzmax; tzmax = temp; }

        if ((tmin > tzmax) || (tzmin > tmax)) return -1;
        if (tzmin > tmin) tmin = tzmin;

        if (tmin < 0) return -1;

        // Normale (Hit-Face) bestimmen
        Vector3f hitPoint = new Vector3f(dir).mul(tmin).add(origin);
        float epsilon = 0.001f;
        outNormal.set(0, 0, 0);
        if (Math.abs(hitPoint.x - box.min.x) < epsilon) outNormal.x = -1;
        else if (Math.abs(hitPoint.x - box.max.x) < epsilon) outNormal.x = 1;
        else if (Math.abs(hitPoint.y - box.min.y) < epsilon) outNormal.y = -1;
        else if (Math.abs(hitPoint.y - box.max.y) < epsilon) outNormal.y = 1;
        else if (Math.abs(hitPoint.z - box.min.z) < epsilon) outNormal.z = -1;
        else if (Math.abs(hitPoint.z - box.max.z) < epsilon) outNormal.z = 1;

        return tmin;
    }
}