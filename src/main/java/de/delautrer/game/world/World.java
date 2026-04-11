package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Player;
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

    public Vector3i raycast(Vector3f origin, Vector3f direction, float maxDist) {
        // (Lasse den kompletten Raycast-Code hier exakt so wie er vorher war)
        float x = origin.x;
        float y = origin.y;
        float z = origin.z;

        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);

        float dx = direction.x;
        float dy = direction.y;
        float dz = direction.z;

        int stepX = (dx > 0) ? 1 : -1;
        int stepY = (dy > 0) ? 1 : -1;
        int stepZ = (dz > 0) ? 1 : -1;

        float deltaX = Math.abs(1 / dx);
        float deltaY = Math.abs(1 / dy);
        float deltaZ = Math.abs(1 / dz);

        float maxX = (stepX > 0) ? (ix + 1 - x) * deltaX : (x - ix) * deltaX;
        float maxY = (stepY > 0) ? (iy + 1 - y) * deltaY : (y - iy) * deltaY;
        float maxZ = (stepZ > 0) ? (iz + 1 - z) * deltaZ : (z - iz) * deltaZ;

        float dist = 0;
        while (dist < maxDist) {
            if (maxX < maxY) {
                if (maxX < maxZ) {
                    ix += stepX;
                    dist = maxX;
                    maxX += deltaX;
                } else {
                    iz += stepZ;
                    dist = maxZ;
                    maxZ += deltaZ;
                }
            } else {
                if (maxY < maxZ) {
                    iy += stepY;
                    dist = maxY;
                    maxY += deltaY;
                } else {
                    iz += stepZ;
                    dist = maxZ;
                    maxZ += deltaZ;
                }
            }
            if (getBlockAt(ix, iy, iz) != 0 && getBlockAt(ix, iy, iz) != 4) {
                return new org.joml.Vector3i(ix, iy, iz);
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