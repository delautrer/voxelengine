package de.delautrer.engine.player;

import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

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
    private final float speed = 5.0f; // Standard 5.0f

    public void update(InputManager input, ChunkManager chunkManager, Vector3f cameraFront, boolean isInventoryOpen, float deltaTime) {
        if (position.y <= -100.0f) respawn();

        // Sneaken (Wird nur geupdatet, wenn wir nicht im Inventar sind!)
        if (!isInventoryOpen) {
            isSneaking = input.isActionActive("SNEAK");
        } else {
            isSneaking = false; // Im Inventar aufhören zu sneaken
        }

        float currentSpeed = isSneaking ? speed * 0.4f : speed;
        eyeHeight = isSneaking ? 1.5f : 1.8f;

        Vector3f moveDir = new Vector3f(0, 0, 0);
        Vector3f flatFront = new Vector3f(cameraFront.x, 0, cameraFront.z).normalize();
        Vector3f flatRight = new Vector3f(flatFront).cross(new Vector3f(0, 1, 0)).normalize();

        // BEWEGUNG NUR ZULASSEN, WENN INVENTAR ZU IST!
        if (!isInventoryOpen) {
            if (input.isActionActive("MOVE_FORWARD")) moveDir.add(flatFront);
            if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(flatFront);
            if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
            if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

            if (onGround && input.isActionActive("JUMP")) {
                velocity.y = jumpForce;
                onGround = false;
            }
        }

        if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);

        // Wir überschreiben die X/Z Velocity NICHT hart, sondern setzen sie weich,
        // damit externe Kräfte (später z.B. Explosionen oder Wasserströmung) addiert werden können.
        // Für den Moment setzen wir sie aber auf unsere Eingabe.
        velocity.x = moveDir.x;
        velocity.z = moveDir.z;
        velocity.y += gravity * deltaTime;

        moveAndCollide(chunkManager, deltaTime);
    }

    public void respawn() {
        this.position.set(spawnPoint);
        this.velocity.set(0, 0, 0);
    }

    private void moveAndCollide(ChunkManager chunkManager, float deltaTime) {
        // --- X-ACHSE BEWEGUNG ---
        float originalX = position.x;
        position.x += velocity.x * deltaTime;

        // Normale Kollision (Wand getroffen?)
        if (isColliding(chunkManager)) {
            position.x = originalX;
            velocity.x = 0;
        }
        // SNEAK-ABGRUND-PRÜFUNG
        else if (isSneaking && onGround) {
            // Wenn wir einen Block nach unten gucken, berühren wir noch etwas?
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                // Nein! Das ist ein Abgrund. Bewegung rückgängig machen.
                position.x = originalX;
                velocity.x = 0;
            }
        }

        // --- Z-ACHSE BEWEGUNG ---
        float originalZ = position.z;
        position.z += velocity.z * deltaTime;

        // Normale Kollision (Wand getroffen?)
        if (isColliding(chunkManager)) {
            position.z = originalZ;
            velocity.z = 0;
        }
        // SNEAK-ABGRUND-PRÜFUNG
        else if (isSneaking && onGround) {
            // Wenn wir einen Block nach unten gucken, berühren wir noch etwas?
            if (!wouldCollideIfMoved(chunkManager, 0, -0.05f, 0)) {
                // Nein! Das ist ein Abgrund. Bewegung rückgängig machen.
                position.z = originalZ;
                velocity.z = 0;
            }
        }

        // --- Y-ACHSE BEWEGUNG ---
        onGround = false;
        float originalY = position.y;
        position.y += velocity.y * deltaTime;

        if (isColliding(chunkManager)) {
            // Wir sind auf den Boden (oder unter eine Decke) gekracht
            if (velocity.y < 0) {
                onGround = true;
            }
            position.y = originalY;
            velocity.y = 0;
        }
    }

    // Hilfsmethode für das Sneaken (Simuliert eine Bewegung, ohne den Spieler zu verändern)
    private boolean wouldCollideIfMoved(ChunkManager chunkManager, float dx, float dy, float dz) {
        AABB testBB = new AABB(
                new Vector3f(position.x - playerWidth + dx, position.y + dy, position.z - playerWidth + dz),
                new Vector3f(position.x + playerWidth + dx, position.y + playerHeight + dy, position.z + playerWidth + dz)
        );

        for (int x = (int)Math.floor(testBB.min.x); x <= (int)Math.floor(testBB.max.x); x++) {
            for (int y = (int)Math.floor(testBB.min.y); y <= (int)Math.floor(testBB.max.y); y++) {
                for (int z = (int)Math.floor(testBB.min.z); z <= (int)Math.floor(testBB.max.z); z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);
                    if (c != null) {
                        byte block = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                        // 0 = Luft, 4 = Wasser (Beides fällt man durch!)
                        //if (block != 0 && block != 4) return true;
                        if(block != 0 && !BlockRegistry.get(block).isPassable) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isColliding(ChunkManager chunkManager) {
        AABB playerBB = getAABB();
        for (int x = (int)Math.floor(playerBB.min.x); x <= (int)Math.floor(playerBB.max.x); x++) {
            for (int y = (int)Math.floor(playerBB.min.y); y <= (int)Math.floor(playerBB.max.y); y++) {
                for (int z = (int)Math.floor(playerBB.min.z); z <= (int)Math.floor(playerBB.max.z); z++) {
                    Chunk c = chunkManager.getChunkAtBlock(x, y, z);
                    if (c != null) {
                        byte block = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
                        //if (block != 0 && block != 4) return true;
                        if(block != 0 && !BlockRegistry.get(block).isPassable) return true;
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