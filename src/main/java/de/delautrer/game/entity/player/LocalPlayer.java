package de.delautrer.game.entity.player;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;

public class LocalPlayer extends Player {

    private final Camera camera;
    private PlayerInteraction interaction;
    private EventBus eventBus; // Neu: Brauchen wir für die Inventory-Events

    private float cameraVisualYOffset = 0.0f;

    private final float jumpForce = 9.0f;
    private final float speed = 5.0f;

    public LocalPlayer(Vector3f spawnPosition) {
        super(spawnPosition);
        this.camera = new Camera();
    }

    public void initInteraction(World world, VulkanContext context, EventBus eventBus) {
        this.eventBus = eventBus;
        this.interaction = new PlayerInteraction(world, this.camera, this, context, eventBus);
    }

    public void updateLocal(InputManager input, ChunkManager chunkManager, float deltaTime) {
        // --- 1. INVENTAR & HOTBAR INPUT ---
        if (input.isActionJustPressed("INVENTORY")) {
            inventory.toggle();
            eventBus.publish(new InventoryToggleEvent(inventory.isOpen()));
        }

        boolean isInventoryOpen = inventory.isOpen();

        if (!isInventoryOpen) {
            // Tasten 1-9 für Hotbar
            for (int i = 0; i < 9; i++) {
                if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                    inventory.setSelectedSlot(i);
                    eventBus.publish(new HotbarSlotChangeEvent(i));
                }
            }

            // Mausrad für Hotbar
            double scroll = input.consumeScroll();
            if (scroll != 0) {
                int newSlot = inventory.getSelectedSlot() - (int) Math.signum(scroll);
                if (newSlot < 0) newSlot = 8;
                else if (newSlot > 8) newSlot = 0;

                inventory.setSelectedSlot(newSlot);
                eventBus.publish(new HotbarSlotChangeEvent(newSlot));
            }
        }

        // --- 2. BEWEGUNG & SNEAKEN ---
        if (!isInventoryOpen) {
            isSneaking = input.isActionActive("SNEAK");
        } else {
            isSneaking = false;
        }

        float currentSpeed = isSneaking ? speed * 0.4f : speed;
        eyeHeight = isSneaking ? 1.5f : 1.8f;

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

        // --- 3. PHYSIK & INTERAKTION ---
        float prevY = position.y;

        super.update(deltaTime, chunkManager);

        float deltaY = position.y - prevY;
        if (deltaY > 0.0f && deltaY <= stepHeight && onGround) {
            cameraVisualYOffset -= deltaY;
        }

        cameraVisualYOffset += (0.0f - cameraVisualYOffset) * 15.0f * deltaTime;

        if (interaction != null) {
            interaction.update(input, deltaTime);
        }
    }

    public void updateCamera(long windowHandle, float deltaTime) {
        Vector3f smoothEyePos = new Vector3f(getEyePosition());
        smoothEyePos.y += cameraVisualYOffset;

        if (!inventory.isOpen()) {
            camera.update(windowHandle, deltaTime, smoothEyePos);
        } else {
            camera.setPosition(smoothEyePos);
        }
    }

    public Camera getCamera() { return camera; }
    public PlayerInteraction getInteraction() { return interaction; }
}