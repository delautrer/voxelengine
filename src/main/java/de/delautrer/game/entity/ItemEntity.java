package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public class ItemEntity extends Entity {

    public ItemStack stack;
    public float pickupDelay = 1.0f;
    private boolean isDead = false;
    public AABB boundingBox; // Fallback, falls nicht in Entity deklariert

    public ItemEntity(ItemStack stack, Vector3f spawnPos, Vector3f initialVelocity) {
        super(spawnPos); // FIX: Ruft den Konstruktor deiner Entity-Basisklasse auf
        this.stack = stack;
        this.velocity = new Vector3f(initialVelocity);
        this.boundingBox = new AABB(new Vector3f(-0.125f, 0, -0.125f), new Vector3f(0.125f, 0.25f, 0.125f));
    }

    @Override
    public void update(float deltaTime, ChunkManager cm) { // FIX: Korrekte Signatur
        if (isDead) return;

        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }

        velocity.y -= 20.0f * deltaTime; // Schwerkraft

        float nextX = position.x + velocity.x * deltaTime;
        float nextY = position.y + velocity.y * deltaTime;
        float nextZ = position.z + velocity.z * deltaTime;

        // Block unter dem Item prüfen (über den ChunkManager)
        Chunk c = cm.getChunkAtBlock((int) Math.floor(nextX), (int) Math.floor(nextY), (int) Math.floor(nextZ));
        if (c != null) {
            byte blockBelow = c.getBlock(Math.floorMod((int) Math.floor(nextX), Chunk.SIZE), (int) Math.floor(nextY), Math.floorMod((int) Math.floor(nextZ), Chunk.SIZE));
            if (BlockRegistry.get(blockBelow).isSolid) {
                nextY = (float) Math.floor(nextY) + 1.001f;
                velocity.y = 0;
                velocity.x *= 0.5f;
                velocity.z *= 0.5f;
            }
        }

        position.set(nextX, nextY, nextZ);
    }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { this.isDead = dead; }
}