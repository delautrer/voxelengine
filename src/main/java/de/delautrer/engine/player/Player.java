package de.delautrer.engine.player;

import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Player {
    private final Vector3f spawnPoint = new Vector3f(8.0f, 20.0f, 8.0f);
    public Vector3f position = new Vector3f(spawnPoint);
    public Vector3f velocity = new Vector3f(0, 0, 0);

    private final float playerWidth = 0.3f;
    private final float playerHeight = 1.9f;
    private float eyeHeight = 1.8f;

    private boolean isSneaking = false;
    private boolean onGround = false;
    private final float gravity = -28.0f;
    private final float jumpForce = 9.0f;
    private final float speed = 5.0f;

    public void update(InputManager input, ChunkManager chunkManager, Vector3f cameraFront, float deltaTime) {
        if (position.y <= -100.0f) respawn();

        // Sneaken (Jetzt viel lesbarer!)
        isSneaking = input.isActionActive("SNEAK");
        float currentSpeed = isSneaking ? speed * 0.4f : speed;
        eyeHeight = isSneaking ? 1.5f : 1.8f;

        Vector3f moveDir = new Vector3f(0, 0, 0);
        Vector3f flatFront = new Vector3f(cameraFront.x, 0, cameraFront.z).normalize();
        Vector3f flatRight = new Vector3f(flatFront).cross(new Vector3f(0, 1, 0)).normalize();

        // Bewegung über logische Aktionen
        if (input.isActionActive("MOVE_FORWARD")) moveDir.add(flatFront);
        if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(flatFront);
        if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
        if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

        if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);

        velocity.x = moveDir.x;
        velocity.z = moveDir.z;
        velocity.y += gravity * deltaTime;

        if (onGround && input.isActionActive("JUMP")) {
            velocity.y = jumpForce;
            onGround = false;
        }

        moveAndCollide(chunkManager, deltaTime);
    }

    public void respawn() {
        this.position.set(spawnPoint);
        this.velocity.set(0, 0, 0);
    }

    private void moveAndCollide(ChunkManager chunkManager, float deltaTime) {
        position.x += velocity.x * deltaTime;
        if (isColliding(chunkManager)) position.x -= velocity.x * deltaTime;

        position.z += velocity.z * deltaTime;
        if (isColliding(chunkManager)) position.z -= velocity.z * deltaTime;

        onGround = false;
        position.y += velocity.y * deltaTime;
        if (isColliding(chunkManager)) {
            if (velocity.y < 0) onGround = true;
            position.y -= velocity.y * deltaTime;
            velocity.y = 0;
        }
    }

    private boolean isColliding(ChunkManager chunkManager) {
        AABB playerBB = getAABB();
        for (int x = (int)Math.floor(playerBB.min.x); x <= (int)Math.floor(playerBB.max.x); x++) {
            for (int y = (int)Math.floor(playerBB.min.y); y <= (int)Math.floor(playerBB.max.y); y++) {
                for (int z = (int)Math.floor(playerBB.min.z); z <= (int)Math.floor(playerBB.max.z); z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);
                    if (c != null) {
                        byte block = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                        if (block != 0 && block != 4) return true;
                    }
                }
            }
        }
        return false;
    }

    public AABB getAABB() {
        return new AABB(
                new Vector3f(position.x - playerWidth, position.y, position.z - playerWidth),
                new Vector3f(position.x + playerWidth, position.y + playerHeight, position.z + playerWidth)
        );
    }

    public Vector3f getEyePosition() {
        return new Vector3f(position.x, position.y + eyeHeight, position.z);
    }
}