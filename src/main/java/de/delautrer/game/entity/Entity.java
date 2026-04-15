package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public abstract class Entity {
    public Vector3f position;
    public Vector3f velocity;

    protected float width = 0.3f;
    protected float height = 1.9f;

    protected boolean onGround = false;
    protected float gravity = -28.0f;

    public Entity(Vector3f spawnPosition) {
        this.position = new Vector3f(spawnPosition);
        this.velocity = new Vector3f(0, 0, 0);
    }

    // Jede Entity muss definieren, wie sie sich updatet
    public abstract void update(float deltaTime, ChunkManager chunkManager);

    // Ausgelagerte, allgemeine Physik- & Kollisionslogik
    protected void moveAndCollide(ChunkManager chunkManager, float deltaTime, boolean avoidFall) {
        // --- X-ACHSE BEWEGUNG ---
        float originalX = position.x;
        position.x += velocity.x * deltaTime;

        if (isColliding(chunkManager)) {
            position.x = originalX;
            velocity.x = 0;
        } else if (avoidFall && onGround) { // Z.B. beim Sneaken
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                position.x = originalX;
                velocity.x = 0;
            }
        }

        // --- Z-ACHSE BEWEGUNG ---
        float originalZ = position.z;
        position.z += velocity.z * deltaTime;

        if (isColliding(chunkManager)) {
            position.z = originalZ;
            velocity.z = 0;
        } else if (avoidFall && onGround) {
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                position.z = originalZ;
                velocity.z = 0;
            }
        }

        // --- Y-ACHSE BEWEGUNG ---
        onGround = false;
        float originalY = position.y;
        position.y += velocity.y * deltaTime;

        if (isColliding(chunkManager)) {
            if (velocity.y < 0) {
                onGround = true; // Wir sind gelandet
            }
            position.y = originalY;
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
        for (int x = (int)Math.floor(bb.min.x); x <= (int)Math.floor(bb.max.x); x++) {
            for (int y = (int)Math.floor(bb.min.y); y <= (int)Math.floor(bb.max.y); y++) {
                for (int z = (int)Math.floor(bb.min.z); z <= (int)Math.floor(bb.max.z); z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);

                    if (c == null) return true; // Ungeladene Welt verhält sich wie eine Wand

                    byte block = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                    if(block != 0 && !BlockRegistry.get(block).isPassable) return true;
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