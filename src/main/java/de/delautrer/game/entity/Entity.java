package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

import java.util.List;

public abstract class Entity {
    public Vector3f position;
    public Vector3f velocity;

    protected float width = 0.3f;
    protected float height = 1.8f;

    protected boolean onGround = false;
    protected float gravity = -28.0f;

    // Wie hoch der Spieler automatisch steigen kann (0.6 reicht locker für 0.5f Slabs)
    protected float stepHeight = 0.6f;

    public Entity(Vector3f spawnPosition) {
        this.position = new Vector3f(spawnPosition);
        this.velocity = new Vector3f(0, 0, 0);
    }

    public abstract void update(float deltaTime, ChunkManager chunkManager);

    protected void moveAndCollide(ChunkManager chunkManager, float deltaTime, boolean avoidFall) {
        // --- X-ACHSE BEWEGUNG ---
        float originalX = position.x;
        position.x += velocity.x * deltaTime;

        if (isColliding(chunkManager)) {
            if (onGround) {
                float originalY = position.y;
                position.y += stepHeight; // Springe virtuell ganz nach oben

                if (isColliding(chunkManager)) {
                    // Hat nicht geklappt, Decke ist im Weg
                    position.y = originalY;
                    position.x = originalX;
                    velocity.x = 0;
                } else {
                    // FIX: Wir sind über der Stufe. Jetzt "snappen" wir exakt auf die Kante runter!
                    float step = stepHeight;
                    for (int i = 0; i < 10; i++) {
                        step /= 2;
                        position.y -= step;
                        if (isColliding(chunkManager)) position.y += step; // Zu weit runter? Wieder hoch!
                    }
                }
            } else {
                position.x = originalX;
                velocity.x = 0;
            }
        } else if (avoidFall && onGround) {
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                position.x = originalX;
                velocity.x = 0;
            }
        }

        // --- Z-ACHSE BEWEGUNG ---
        float originalZ = position.z;
        position.z += velocity.z * deltaTime;

        if (isColliding(chunkManager)) {
            if (onGround) {
                float originalY = position.y;
                position.y += stepHeight;

                if (isColliding(chunkManager)) {
                    position.y = originalY;
                    position.z = originalZ;
                    velocity.z = 0;
                } else {
                    // FIX: Auch hier auf die Kante snappen!
                    float step = stepHeight;
                    for (int i = 0; i < 10; i++) {
                        step /= 2;
                        position.y -= step;
                        if (isColliding(chunkManager)) position.y += step;
                    }
                }
            } else {
                position.z = originalZ;
                velocity.z = 0;
            }
        } else if (avoidFall && onGround) {
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                position.z = originalZ;
                velocity.z = 0;
            }
        }

        // --- Y-ACHSE BEWEGUNG (Gravitation) ---
        onGround = false;
        float originalY = position.y;
        position.y += velocity.y * deltaTime;

        if (isColliding(chunkManager)) {
            if (velocity.y < 0) {
                onGround = true; // Wir sind gelandet
            }
            // Die wichtigste Änderung für Slabs: Wir snappen exakt auf die Kante, nicht auf volle Blöcke!
            // Ein einfacher Hack, der extrem gut funktioniert: Langsam herantasten (binary search artig)
            position.y = originalY;
            float step = velocity.y * deltaTime;
            for (int i = 0; i < 10; i++) {
                step /= 2;
                position.y += step;
                if (isColliding(chunkManager)) position.y -= step;
            }
            velocity.y = 0;
        }
    }

    protected boolean wouldCollideIfMoved(ChunkManager chunkManager, float dx, float dy, float dz) {
        AABB testBB = new AABB(
                new Vector3f(position.x - width + dx, position.y + dy, position.z - width + dz),
                new Vector3f(position.x + width + dx, position.y + height + dy, position.z + width + dz)
        );
        return checkCollisionWithWorld(chunkManager, testBB);
    }

    protected boolean isColliding(ChunkManager chunkManager) {
        return checkCollisionWithWorld(chunkManager, getAABB());
    }

    private boolean checkCollisionWithWorld(ChunkManager chunkManager, AABB bb) {
        int minX = (int)Math.floor(bb.min.x); int maxX = (int)Math.floor(bb.max.x);
        int minY = (int)Math.floor(bb.min.y); int maxY = (int)Math.floor(bb.max.y);
        int minZ = (int)Math.floor(bb.min.z); int maxZ = (int)Math.floor(bb.max.z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);
                    if (c == null) return true;

                    byte blockId = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                    if (blockId != 0 && !BlockRegistry.get(blockId).isPassable) {

                        // HIER IST DIE MAGIE: Wir prüfen exakt gegen die Treppen/Slab-Hitboxen!
                        BlockState state = c.getBlockState(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                        List<AABB> boxes = BlockRegistry.get(blockId).getBoundingBoxes(state);

                        for (AABB box : boxes) {
                            AABB worldBox = new AABB(
                                    new Vector3f(box.min).add(x, y, z),
                                    new Vector3f(box.max).add(x, y, z)
                            );
                            if (AABB.isColliding(bb, worldBox)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public AABB getAABB() {
        return new AABB(
                new Vector3f(position.x - width, position.y, position.z - width),
                new Vector3f(position.x + width, position.y + height, position.z + width)
        );
    }
}