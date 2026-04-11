package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Player;
import de.delautrer.game.blocks.BlockRegistry;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class World {
    private final ChunkManager chunkManager;
    private final FluidSimulator fluidSimulator; // NEU!
    private final Player player;

    private float waterTimer = 0.0f;

    public World(VulkanContext context, Player player) {
        this.chunkManager = new ChunkManager(context);
        this.fluidSimulator = new FluidSimulator(chunkManager, context); // NEU!
        this.player = player;

        chunkManager.update(player.position.x, player.position.z);

        Vector3f safeSpawn = findSafeSpawn(8, 8);
        player.position.set(safeSpawn);
    }

    public Vector3f findSafeSpawn(int x, int z) {
        for (int y = Chunk.HEIGHT - 3; y > 0; y--) {
            if (getBlockAt(x, y, z) != 0) {
                if (getBlockAt(x, y + 1, z) == 0 && getBlockAt(x, y + 2, z) == 0) {
                    return new Vector3f(x + 0.5f, y + 1.0f, z + 0.5f);
                }
            }
        }
        return new Vector3f(x, 30, z);
    }

    public byte getBlockAt(int x, int y, int z) {
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }

    public void update(InputManager input, Vector3f cameraFront, float deltaTime) {
        player.update(input, chunkManager, cameraFront, deltaTime);
        chunkManager.update(player.position.x, player.position.z);

        // Wasser Physik-Tick über den Simulator!
        waterTimer += deltaTime;
        if (waterTimer >= 0.3f) {
            fluidSimulator.tick();
            waterTimer = 0.0f;
        }

        if (player.position.y < -50) {
            player.position.set(findSafeSpawn((int)player.position.x, (int)player.position.z));
            player.velocity.set(0);
        }
    }

    // --- NEU: Die Hilfsklasse für das Raycast-Ergebnis ---
    public static class RaycastResult {
        public final Vector3i hitPos;
        public final Vector3i adjacentPos;

        public RaycastResult(Vector3i hitPos, Vector3i adjacentPos) {
            this.hitPos = hitPos;
            this.adjacentPos = adjacentPos;
        }
    }

    // --- NEU: Der 3D DDA Algorithmus ---
    public RaycastResult raycast(Vector3f start, Vector3f dir, float maxDistance) {
        int x = (int) Math.floor(start.x);
        int y = (int) Math.floor(start.y);
        int z = (int) Math.floor(start.z);

        // Die Richtung, in die wir springen (+1, -1 oder 0)
        int stepX = Float.compare(dir.x, 0.0f);
        int stepY = Float.compare(dir.y, 0.0f);
        int stepZ = Float.compare(dir.z, 0.0f);

        // Wie weit muss der Strahl wandern, um genau 1 Block in X/Y/Z-Richtung voranzukommen?
        float tDeltaX = stepX != 0 ? Math.abs(1.0f / dir.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1.0f / dir.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1.0f / dir.z) : Float.MAX_VALUE;

        // Wie weit ist es bis zur ALLERERSTEN Blockgrenze?
        float tMaxX = stepX > 0 ? (x + 1.0f - start.x) * tDeltaX : (start.x - x) * tDeltaX;
        float tMaxY = stepY > 0 ? (y + 1.0f - start.y) * tDeltaY : (start.y - y) * tDeltaY;
        float tMaxZ = stepZ > 0 ? (z + 1.0f - start.z) * tDeltaZ : (start.z - z) * tDeltaZ;

        Vector3i lastPos = new Vector3i(x, y, z);
        float dist = 0.0f;

        byte air = BlockRegistry.AIR.getId();
        byte water = BlockRegistry.WATER.getId();

        // Prüfen, ob der Spieler mit dem Kopf bereits in einem Block steckt
        byte startBlock = getBlockAt(x, y, z);
        if (startBlock != air && startBlock != water) {
            return new RaycastResult(new Vector3i(x, y, z), new Vector3i(x, y, z));
        }

        // Die magische Voxel-Traversal-Schleife
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

            byte blockId = getBlockAt(x, y, z);
            if (blockId != air && blockId != water) {
                // Treffer! Wir geben den getroffenen Block UND die vorherige Position (lastPos) zurück!
                return new RaycastResult(new Vector3i(x, y, z), new Vector3i(lastPos));
            }
        }
        return null;
    }

    public java.util.List<de.delautrer.engine.graphics.VulkanMesh> getVisibleMeshes(org.joml.Matrix4f mvp) {
        org.joml.FrustumIntersection frustum = new org.joml.FrustumIntersection(mvp);
        java.util.List<de.delautrer.engine.graphics.VulkanMesh> visibleMeshes = new java.util.ArrayList<>();

        for (java.util.Map.Entry<org.joml.Vector2i, de.delautrer.engine.graphics.VulkanMesh> entry : chunkManager.getMeshes().entrySet()) {
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

    public ChunkManager getChunkManager() { return chunkManager; }
    public Player getPlayer() { return player; }
}