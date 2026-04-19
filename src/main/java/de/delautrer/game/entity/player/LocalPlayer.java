package de.delautrer.game.entity.player;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;

public class LocalPlayer extends Player {

    private final Camera camera;
    private PlayerInteraction interaction;
    private EventBus eventBus;

    private GameMode gameMode = GameMode.SURVIVAL;
    private boolean isChatOpen = false;
    private float lastSpacePressTime = 0.0f;
    private float cameraVisualYOffset = 0.0f;
    private final float jumpForce = 9.0f;
    private final float speed = 5.0f;

    private Block headBlock = BlockRegistry.AIR;

    public LocalPlayer(Vector3f spawnPosition) {
        super(spawnPosition);
        this.camera = new Camera();
    }

    public void initInteraction(World world, VulkanContext context, EventBus eventBus) {
        this.eventBus = eventBus;
        this.interaction = new PlayerInteraction(world, this.camera, this, context, eventBus);
    }

    public void updateLocal(InputManager input, ChunkManager chunkManager, float deltaTime) {
        // --- GAMEMODE LOGIK ---
        if (gameMode == GameMode.SPECTATOR) {
            isFlying = true; // Spectator fliegt immer
        }

        // --- 1. INVENTAR, HOTBAR & FLIEGEN TOGGLE (NUR WENN CHAT ZU IST) ---
        if (!isChatOpen) {

            if (input.isActionJustPressed("JUMP")) {
                float currentTime = (float) org.lwjgl.glfw.GLFW.glfwGetTime();
                if (currentTime - lastSpacePressTime < 0.3f) {
                    if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
                        isFlying = !isFlying;
                    }
                }
                lastSpacePressTime = currentTime;
            }

            // Inventar öffnen / schließen
            if (input.isActionJustPressed("INVENTORY") && gameMode != GameMode.SPECTATOR) {
                inventory.toggle();
                eventBus.publish(new InventoryToggleEvent(inventory.isOpen()));
            }

            if (!inventory.isOpen()) {
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
        }

        // --- BEWEGUNG ---
        boolean isInventoryOpen = inventory.isOpen() || isChatOpen;

        if (!isInventoryOpen) {
            isSneaking = input.isActionActive("SNEAK");
            isSprinting = input.isActionActive("SPRINT") && !isSneaking;
        } else {
            isSneaking = false;
            isSprinting = false;
        }

        float currentSpeed = speed;
        if (isSneaking) currentSpeed *= 0.4f;
        else if (isSprinting) currentSpeed *= 1.5f;

        if (isFlying) currentSpeed *= isSneaking ? 2.0f : 1.5f;


        Vector3f moveDir = new Vector3f(0, 0, 0);
        Vector3f cameraFront = camera.getFront();
        Vector3f flatFront = new Vector3f(cameraFront.x, 0, cameraFront.z).normalize();
        Vector3f flatRight = new Vector3f(flatFront).cross(new Vector3f(0, 1, 0)).normalize();

        if (!isInventoryOpen) {
            if (input.isActionActive("MOVE_FORWARD")) moveDir.add(flatFront);
            if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(flatFront);
            if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
            if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

            if (isFlying) {
                velocity.y = 0;
                if (input.isActionActive("JUMP")) velocity.y = currentSpeed;
                if (input.isActionActive("SNEAK")) velocity.y = -currentSpeed*2.5f;
            } else if (onGround && input.isActionActive("JUMP")) {
                velocity.y = jumpForce;
                onGround = false;
            }
        }

        if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);
        velocity.x = moveDir.x;
        velocity.z = moveDir.z;

        if (gameMode != GameMode.SPECTATOR) {
            pushOutOfBlocks(chunkManager, input, deltaTime);
        }

        // --- 3. PHYSIK & INTERAKTION ---
        float prevY = position.y;

        if (gameMode == GameMode.SPECTATOR) {
            position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
        } else {
            super.update(deltaTime, chunkManager);
        }

        float deltaY = position.y - prevY;
        if (deltaY > 0.0f && deltaY <= stepHeight && onGround) {
            cameraVisualYOffset -= deltaY;
        }

        cameraVisualYOffset += (0.0f - cameraVisualYOffset) * 15.0f * deltaTime;

        Vector3f eyePos = getEyePosition();
        byte blockIdAtHead = chunkManager.getWorld().getBlockAt(
                (int) Math.floor(eyePos.x),
                (int) Math.floor(eyePos.y),
                (int) Math.floor(eyePos.z)
        );
        Block b = BlockRegistry.get(blockIdAtHead);

        if (b.isSolid && !b.isTransparent) {
            headBlock = b;
        } else {
            headBlock = BlockRegistry.AIR;
        }

        if (interaction != null) {
            interaction.update(input, deltaTime);
        }
    }

    protected void pushOutOfBlocks(ChunkManager cm, de.delautrer.engine.input.InputManager input, float deltaTime) {
        int byFeet = (int) Math.floor(position.y + 0.1f);
        int byHead = (int) Math.floor(position.y + height * 0.8f);
        boolean isStuck = false;
        float checkW = width - 0.01f;

        int minX = (int) Math.floor(position.x - checkW);
        int maxX = (int) Math.floor(position.x + checkW);
        int minZ = (int) Math.floor(position.z - checkW);
        int maxZ = (int) Math.floor(position.z + checkW);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (BlockRegistry.get(cm.getWorld().getBlockAt(x, byFeet, z)).isSolid ||
                        BlockRegistry.get(cm.getWorld().getBlockAt(x, byHead, z)).isSolid) {
                    isStuck = true;
                    break;
                }
            }
        }

        if (!isStuck) return;

        int bx = (int) Math.floor(position.x);
        int bz = (int) Math.floor(position.z);

        if (input.isActionActive("JUMP")) {
            Block blockAboveHead = BlockRegistry.get(cm.getWorld().getBlockAt(bx, byHead + 1, bz));
            if (!blockAboveHead.isSolid) {
                position.y += 4.5f * deltaTime;
                return;
            }
        }
        boolean leftFree = !BlockRegistry.get(cm.getWorld().getBlockAt(bx - 1, byFeet, bz)).isSolid &&
                !BlockRegistry.get(cm.getWorld().getBlockAt(bx - 1, byHead, bz)).isSolid;
        boolean rightFree = !BlockRegistry.get(cm.getWorld().getBlockAt(bx + 1, byFeet, bz)).isSolid &&
                !BlockRegistry.get(cm.getWorld().getBlockAt(bx + 1, byHead, bz)).isSolid;
        boolean backFree = !BlockRegistry.get(cm.getWorld().getBlockAt(bx, byFeet, bz - 1)).isSolid &&
                !BlockRegistry.get(cm.getWorld().getBlockAt(bx, byHead, bz - 1)).isSolid;
        boolean frontFree = !BlockRegistry.get(cm.getWorld().getBlockAt(bx, byFeet, bz + 1)).isSolid &&
                !BlockRegistry.get(cm.getWorld().getBlockAt(bx, byHead, bz + 1)).isSolid;

        float distLeft  = position.x - bx;
        float distRight = (bx + 1.0f) - position.x;
        float distBack  = position.z - bz;
        float distFront = (bz + 1.0f) - position.z;

        float intentBonus = 10.0f;
        if (velocity.x < -0.1f) distLeft -= intentBonus;
        if (velocity.x > 0.1f) distRight -= intentBonus;
        if (velocity.z < -0.1f) distBack -= intentBonus;
        if (velocity.z > 0.1f) distFront -= intentBonus;

        float minScore = 999.0f;
        int escapeDir = -1;

        if (leftFree && distLeft < minScore) { minScore = distLeft; escapeDir = 0; }
        if (rightFree && distRight < minScore) { minScore = distRight; escapeDir = 1; }
        if (backFree && distBack < minScore) { minScore = distBack; escapeDir = 2; }
        if (frontFree && distFront < minScore) { minScore = distFront; escapeDir = 3; }

        if (escapeDir != -1) {
            float pushSpeed = 4.5f * deltaTime;

            if (escapeDir == 0) position.x -= pushSpeed;
            if (escapeDir == 1) position.x += pushSpeed;
            if (escapeDir == 2) position.z -= pushSpeed;
            if (escapeDir == 3) position.z += pushSpeed;
        }
    }

    public void updateCamera(long windowHandle, float deltaTime) {
        Vector3f smoothEyePos = new Vector3f(getEyePosition());
        smoothEyePos.y += cameraVisualYOffset;

        if (!inventory.isOpen() && !isChatOpen) { // Auch hier verhindern wir Kamera-Drehung beim Tippen!
            camera.update(windowHandle, deltaTime, smoothEyePos);
        } else {
            camera.setPosition(smoothEyePos);
        }
    }
    public void setChatOpen(boolean chatOpen) {
        this.isChatOpen = chatOpen;
    }
    public boolean isChatOpen() { return isChatOpen; }
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        if (mode == GameMode.SURVIVAL) this.isFlying = false;
    }
    public GameMode getGameMode() { return gameMode; }
    public Camera getCamera() { return camera; }
    public PlayerInteraction getInteraction() { return interaction; }
    public Block getHeadBlock() { return headBlock; }
}