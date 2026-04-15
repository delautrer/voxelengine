package de.delautrer.game.entity.player;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;

public class LocalPlayer extends Player {

    private final Camera camera;
    private PlayerInteraction interaction;

    private final float jumpForce = 9.0f;
    private final float speed = 5.0f;

    public LocalPlayer(Vector3f spawnPosition) {
        super(spawnPosition);
        this.camera = new Camera();
    }

    public void initInteraction(World world, VulkanContext context, EventBus eventBus) {
        this.interaction = new PlayerInteraction(world, this.camera, this, context, eventBus);
    }

    public void updateLocal(InputManager input, ChunkManager chunkManager, float deltaTime) {
        boolean isInventoryOpen = inventory.isOpen();

        // 1. Input & Sneak Status
        if (!isInventoryOpen) {
            isSneaking = input.isActionActive("SNEAK");
        } else {
            isSneaking = false;
        }

        float currentSpeed = isSneaking ? speed * 0.4f : speed;
        eyeHeight = isSneaking ? 1.5f : 1.8f;

        // 2. Bewegungsvektoren berechnen
        Vector3f moveDir = new Vector3f(0, 0, 0);
        Vector3f cameraFront = camera.getFront();
        Vector3f flatFront = new Vector3f(cameraFront.x, 0, cameraFront.z).normalize();
        Vector3f flatRight = new Vector3f(flatFront).cross(new Vector3f(0, 1, 0)).normalize();

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
        velocity.x = moveDir.x;
        velocity.z = moveDir.z;

        // 3. Entity-Physik ausführen (Gravitation & moveAndCollide)
        super.update(deltaTime, chunkManager);

        // 4. Interaktion updaten (Block Abbau/Platzieren)
        if (interaction != null) {
            interaction.update(input, deltaTime);
        }
    }

    // Kamera-Update aus der Game Loop
    public void updateCamera(long windowHandle, float deltaTime) {
        if (!inventory.isOpen()) {
            camera.update(windowHandle, deltaTime, getEyePosition());
        } else {
            camera.setPosition(getEyePosition());
        }
    }

    public Camera getCamera() { return camera; }
    public PlayerInteraction getInteraction() { return interaction; }
}