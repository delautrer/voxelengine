package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.List;

public abstract class Entity {
    public Vector3d position;
    public Vector3f velocity;

    /** Smoothed sky-light brightness at this entity's position (0.0 - 1.0) */
    public float skyLightBrightness = 1.0f;
    /** Smoothed block-light brightness at this entity's position (0.0 - 1.0) */
    public float blockLightBrightness = 0.0f;

    protected float width = 0.3f;
    protected float height = 1.8f;

    protected boolean isDead = false;
    protected boolean onGround = false;
    protected float gravity = -28.0f;
    protected float stepHeight = 0.6f;

    protected boolean inWater = false;

    public Entity(Vector3d spawnPosition) {
        this.position = new Vector3d(spawnPosition);
        this.velocity = new Vector3f(0, 0, 0);
    }
    
    public float getGravity() {
        return gravity;
    }
    
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public abstract void update(float deltaTime, ChunkManager chunkManager);

    /**
     * Wird 20 mal pro Sekunde vom Ticksystem aufgerufen.
     */
    public void onTick(de.delautrer.game.world.World world) {}

    protected void moveAndCollide(ChunkManager chunkManager, float deltaTime, boolean avoidFall) {
        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;

        AABB currentBB = getAABB();
        AABB searchBounds = new AABB(
                new Vector3f(Math.min(currentBB.min.x, currentBB.min.x + dx),
                        Math.min(currentBB.min.y, currentBB.min.y + dy) - stepHeight,
                        Math.min(currentBB.min.z, currentBB.min.z + dz)),
                new Vector3f(Math.max(currentBB.max.x, currentBB.max.x + dx),
                        Math.max(currentBB.max.y, currentBB.max.y + dy) + stepHeight,
                        Math.max(currentBB.max.z, currentBB.max.z + dz))
        );

        List<AABB> nearbyBoxes = getNearbyBoxes(chunkManager, searchBounds);

        // --- X-ACHSE BEWEGUNG ---
        double originalX = position.x;
        position.x += dx;
        if (isCollidingWithList(getAABB(), nearbyBoxes)) {
            if (onGround) {
                double originalY = position.y;
                position.y += stepHeight;
                if (isCollidingWithList(getAABB(), nearbyBoxes)) {
                    position.y = originalY;
                    position.x = originalX;
                    velocity.x = 0;
                } else {
                    float step = stepHeight;
                    for (int i = 0; i < 10; i++) {
                        step /= 2;
                        position.y -= step;
                        if (isCollidingWithList(getAABB(), nearbyBoxes)) position.y += step;
                    }
                }
            } else {
                position.x = originalX;
                velocity.x = 0;
            }
        } else if (avoidFall && onGround) {
            AABB fallCheckBB = new AABB(
                    new Vector3f((float)position.x - width, (float)position.y - 0.05f, (float)position.z - width),
                    new Vector3f((float)position.x + width, (float)position.y + height, (float)position.z + width)
            );
            if (!isCollidingWithList(fallCheckBB, nearbyBoxes)) {
                position.x = originalX;
                velocity.x = 0;
            }
        }

        // --- Z-ACHSE BEWEGUNG ---
        double originalZ = position.z;
        position.z += dz;
        if (isCollidingWithList(getAABB(), nearbyBoxes)) {
            if (onGround) {
                double originalY = position.y;
                position.y += stepHeight;
                if (isCollidingWithList(getAABB(), nearbyBoxes)) {
                    position.y = originalY;
                    position.z = originalZ;
                    velocity.z = 0;
                } else {
                    float step = stepHeight;
                    for (int i = 0; i < 10; i++) {
                        step /= 2;
                        position.y -= step;
                        if (isCollidingWithList(getAABB(), nearbyBoxes)) position.y += step;
                    }
                }
            } else {
                position.z = originalZ;
                velocity.z = 0;
            }
        } else if (avoidFall && onGround) {
            AABB fallCheckBB = new AABB(
                    new Vector3f((float)position.x - width, (float)position.y - 0.05f, (float)position.z - width),
                    new Vector3f((float)position.x + width, (float)position.y + height, (float)position.z + width)
            );
            if (!isCollidingWithList(fallCheckBB, nearbyBoxes)) {
                position.z = originalZ;
                velocity.z = 0;
            }
        }

        // --- Y-ACHSE BEWEGUNG (Gravitation) ---
        onGround = false;
        double originalY = position.y;

        // Wasser-Physik
        inWater = checkInWater(chunkManager);
        if (inWater) {
            velocity.y *= 0.8f;
            applyWaterCurrent(chunkManager, deltaTime);
        }

        position.y += dy;
        if (isCollidingWithList(getAABB(), nearbyBoxes)) {
            if (velocity.y < 0) {
                onGround = true;
            }
            position.y = originalY;
            float step = dy;
            for (int i = 0; i < 10; i++) {
                step /= 2;
                position.y += step;
                if (isCollidingWithList(getAABB(), nearbyBoxes)) position.y -= step;
            }
            velocity.y = 0;
        }
    }

    private boolean checkInWater(ChunkManager cm) {
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y + height * 0.5f);
        int z = (int) Math.floor(position.z);
        return cm.getWorld().getBlock(x, y, z) instanceof WaterBlock;
    }

    private void applyWaterCurrent(ChunkManager cm, float dt) {
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y + height * 0.5f);
        int z = (int) Math.floor(position.z);
        Block b = cm.getWorld().getBlock(x, y, z);
        if (b instanceof WaterBlock wb) {
            // Flow direction is based on levels
        }
    }

    protected List<AABB> getNearbyBoxes(ChunkManager chunkManager, AABB bounds) {
        List<AABB> boxes = new java.util.ArrayList<>();
        int minX = (int)Math.floor(bounds.min.x - 1.0f); int maxX = (int)Math.floor(bounds.max.x + 1.0f);
        int minY = (int)Math.floor(bounds.min.y - 1.0f); int maxY = (int)Math.floor(bounds.max.y + 1.0f);
        int minZ = (int)Math.floor(bounds.min.z - 1.0f); int maxZ = (int)Math.floor(bounds.max.z + 1.0f);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);
                    if (c == null) {
                        boxes.add(new AABB(new Vector3f(x, y, z), new Vector3f(x + 1, y + 1, z + 1)));
                        continue;
                    }
                    de.delautrer.game.blocks.Block block = chunkManager.getWorld().getBlock(x, y, z);
                    if (block != null && !block.isPassable) {
                        BlockState state = chunkManager.getWorld().getBlockState(x, y, z);
                        for (AABB box : block.getCollisionBoxes(state)) {
                            boxes.add(new AABB(
                                    new Vector3f(box.min).add(x, y, z),
                                    new Vector3f(box.max).add(x, y, z)
                            ));
                        }
                    }
                }
            }
        }
        return boxes;
    }

    private boolean isCollidingWithList(AABB bb, List<AABB> boxes) {
        for (AABB box : boxes) {
            if (AABB.isColliding(bb, box)) return true;
        }
        return false;
    }

    public AABB getAABB() {
        return new AABB(
                new Vector3f((float)position.x - width, (float)position.y, (float)position.z - width),
                new Vector3f((float)position.x + width, (float)position.y + height, (float)position.z + width)
        );
    }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { this.isDead = dead; }
}