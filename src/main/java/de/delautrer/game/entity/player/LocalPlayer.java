package de.delautrer.game.entity.player;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.events.PlayerItemDropEvent;
import de.delautrer.game.items.ItemStack;
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

    // NEU: Damit wir wissen, ob wir den GLFW-Cursor-Modus ändern müssen
    private boolean wasUIOpen = false;

    public LocalPlayer(Vector3f spawnPosition) {
        super(spawnPosition);
        this.camera = new Camera();
    }

    public void initInteraction(World world, VulkanContext context, EventBus eventBus) {
        this.eventBus = eventBus;
        this.interaction = new PlayerInteraction(world, this.camera, this, context, eventBus);
    }

    public void updateLocal(InputManager input, ChunkManager chunkManager, float deltaTime) {
        if (gameMode == GameMode.SPECTATOR) {
            isFlying = true;
        }

        // ==========================================
        // 1. WASSER-ERKENNUNG VOR DER BEWEGUNG
        // ==========================================
        int bx = (int) Math.floor(position.x);
        int byFeet = (int) Math.floor(position.y + 0.1f);
        int byWaist = (int) Math.floor(position.y + 0.6f);
        int byBody = (int) Math.floor(position.y + (height * 0.5f));
        int byHead = (int) Math.floor(getEyePosition().y);
        int bz = (int) Math.floor(position.z);

        byte blockFeet = chunkManager.getWorld().getBlockAt(bx, byFeet, bz);
        byte blockWaist = chunkManager.getWorld().getBlockAt(bx, byWaist, bz);
        byte blockBody = chunkManager.getWorld().getBlockAt(bx, byBody, bz);
        byte blockHead = chunkManager.getWorld().getBlockAt(bx, byHead, bz);
        byte waterId = BlockRegistry.WATER.getId();

        this.isInWater = (blockFeet == waterId || blockBody == waterId);
        this.isHeadInWater = (blockHead == waterId);

        // ==========================================
        // UI / Inventar Logik
        // ==========================================
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

            if (input.isActionJustPressed("INVENTORY") && gameMode != GameMode.SPECTATOR) {
                inventory.toggle();

                // 1. Das alte Event, das du behalten wolltest
                eventBus.publish(new InventoryToggleEvent(inventory.isOpen()));

                // 2. Die neuen dedizierten Events
                if (inventory.isOpen()) {
                    eventBus.publish(new de.delautrer.game.events.InventoryOpenedEvent(this, inventory));
                } else {
                    eventBus.publish(new de.delautrer.game.events.InventoryClosedEvent(this, inventory));
                    // Falls eine Kiste offen war, diese ebenfalls logisch schließen
                    if (getOpenedInventory() != null) {
                        eventBus.publish(new de.delautrer.game.events.InventoryClosedEvent(this, getOpenedInventory()));
                        closeInventory();
                    }
                }
            }

            // Hotbar-Auswahl (Darf nur möglich sein, wenn kein UI offen ist)
            if (!inventory.isOpen() && getOpenedInventory() == null) {
                boolean slotChanged = false;

                for (int i = 0; i < 9; i++) {
                    if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                        inventory.setSelectedSlot(i);
                        eventBus.publish(new HotbarSlotChangeEvent(i));
                        slotChanged = true;
                    }
                }
                double scroll = input.consumeScroll();
                if (scroll != 0) {
                    int newSlot = inventory.getSelectedSlot() - (int) Math.signum(scroll);
                    if (newSlot < 0) newSlot = 8;
                    else if (newSlot > 8) newSlot = 0;
                    inventory.setSelectedSlot(newSlot);
                    eventBus.publish(new HotbarSlotChangeEvent(newSlot));
                    slotChanged = true;
                }

                // 3. BlockSelectedEvent feuern
                if (slotChanged) {
                    ItemStack selectedStack = inventory.getStack(inventory.getSelectedSlot());
                    if (selectedStack != null && selectedStack.type instanceof de.delautrer.game.items.BlockItem) {
                        de.delautrer.game.items.BlockItem blockItem = (de.delautrer.game.items.BlockItem) selectedStack.type;
                        eventBus.publish(new de.delautrer.game.events.BlockSelectedEvent(blockItem.getBlock().getId()));
                    } else {
                        // Senden wir 0 (Air), wenn kein Block in der Hand ist
                        eventBus.publish(new de.delautrer.game.events.BlockSelectedEvent((byte) 0));
                    }
                }
            }
        }

        boolean isUIOpen = inventory.isOpen() || isChatOpen || getOpenedInventory() != null;

        if (input.isActionJustPressed("DROP_ITEM") && !isUIOpen) {
            ItemStack currentStack = inventory.getStack(inventory.getSelectedSlot());

            if (currentStack != null) {
                ItemStack dropStack = new ItemStack(currentStack.type, 1);
                currentStack.amount -= 1;
                if (currentStack.amount <= 0) {
                    inventory.setStack(inventory.getSelectedSlot(), null);
                }

                eventBus.publish(new de.delautrer.game.events.InventoryChangeEvent());

                Vector3f spawnPos = new Vector3f(this.position).add(0, 1.5f, 0);

                float yawRad = (float) Math.toRadians(this.getCamera().getYaw());
                float pitchRad = (float) Math.toRadians(this.getCamera().getPitch());
                float adjustedYaw = yawRad;

                float vx = (float) (Math.cos(adjustedYaw) * Math.cos(pitchRad));
                float vy = (float) (-Math.sin(pitchRad));
                float vz = (float) (Math.sin(adjustedYaw) * Math.cos(pitchRad));

                Vector3f lookDir = new Vector3f(vx, vy, vz).normalize();
                Vector3f throwVelocity = new Vector3f(lookDir).mul(5.0f).add(0, 1.5f, 0);

                ItemEntity itemEntity = new ItemEntity(dropStack, spawnPos, throwVelocity);
                chunkManager.getWorld().spawnEntity(itemEntity);

                eventBus.publish(new PlayerItemDropEvent(this, dropStack));
            }
        }

        // ==========================================
        // 2. STATUS-UPDATES (Sprinten & Schwimmen & Hitbox)
        // ==========================================
        if (!isUIOpen) {
            isSneaking = input.isActionActive("SNEAK");
            isSprinting = input.isActionActive("SPRINT") && !isSneaking;

            if (!isSprinting) {
                swimLock = false;
            }

            if (isInWater) {
                if (isSprinting && input.isActionActive("MOVE_FORWARD") && !swimLock && blockWaist == waterId) {
                    isSwimming = true;
                }
                if (!input.isActionActive("MOVE_FORWARD")) {
                    isSwimming = false;
                }

                if (isSwimming && blockWaist != waterId) {
                    isSwimming = false;
                    swimLock = true;
                }
            } else {
                isSwimming = false;
            }
        } else {
            isSneaking = false;
            isSprinting = false;
            isSwimming = false;
        }

        // --- HITBOX & 1x1 TUNNEL SCHUTZ ---
        if (!isSwimming && swimProgress > 0.1f) {
            int headBlockWhenStanding = chunkManager.getWorld().getBlockAt(
                    (int) Math.floor(position.x),
                    (int) Math.floor(position.y + 1.8f),
                    (int) Math.floor(position.z)
            );

            if (BlockRegistry.get((byte) headBlockWhenStanding).isSolid) {
                isSwimming = true;
            }
        }

        // --- ANIMATION BERECHNEN ---
        if (isSwimming) {
            swimProgress += deltaTime * 5.0f;
            if (swimProgress > 1.0f) swimProgress = 1.0f;
        } else {
            swimProgress -= deltaTime * 5.0f;
            if (swimProgress < 0.0f) swimProgress = 0.0f;
        }

        this.height = 1.8f + (0.6f - 1.8f) * swimProgress;

        // ==========================================
        // 3. GESCHWINDIGKEIT BERECHNEN
        // ==========================================
        float currentSpeed = speed;
        if (isFlying) {
            currentSpeed *= isSneaking ? 2.0f : 1.5f;
        } else if (isSwimming) {
            currentSpeed *= 1.3f;
        } else if (isInWater) {
            currentSpeed *= 0.4f;
        } else {
            if (isSneaking) currentSpeed *= 0.4f;
            else if (isSprinting) currentSpeed *= 1.5f;
        }

        Vector3f moveDir = new Vector3f(0, 0, 0);
        Vector3f cameraFront = camera.getFront();
        Vector3f flatFront = new Vector3f(cameraFront.x, 0, cameraFront.z).normalize();
        Vector3f flatRight = new Vector3f(flatFront).cross(new Vector3f(0, 1, 0)).normalize();

        // ==========================================
        // 4. BEWEGUNGS-LOGIK
        // ==========================================
        if (!isUIOpen) {
            if (isSwimming) {
                if (input.isActionActive("MOVE_FORWARD")) moveDir.add(cameraFront);
                if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(cameraFront);
                if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);

                velocity.x = moveDir.x;
                velocity.z = moveDir.z;
                velocity.y = moveDir.y;

                int blockAtTop = chunkManager.getWorld().getBlockAt(bx, (int) Math.floor(position.y + height + 0.1f), bz);
                if (blockAtTop != waterId && velocity.y > 0 && !input.isActionActive("JUMP")) {
                    velocity.y *= 0.1f;
                }

                if (input.isActionActive("JUMP")) velocity.y += 3.5f;
                if (input.isActionActive("SNEAK")) velocity.y -= 2.0f;

            } else if (isFlying) {
                if (input.isActionActive("MOVE_FORWARD")) moveDir.add(flatFront);
                if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(flatFront);
                if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);
                velocity.x = moveDir.x;
                velocity.z = moveDir.z;

                velocity.y = 0;
                if (input.isActionActive("JUMP")) velocity.y = currentSpeed;
                if (input.isActionActive("SNEAK")) velocity.y = -currentSpeed * 1.2f;

            } else {
                if (input.isActionActive("MOVE_FORWARD")) moveDir.add(flatFront);
                if (input.isActionActive("MOVE_BACKWARD")) moveDir.sub(flatFront);
                if (input.isActionActive("MOVE_LEFT")) moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT")) moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0) moveDir.normalize().mul(currentSpeed);
                velocity.x = moveDir.x;
                velocity.z = moveDir.z;

                if (isInWater) {
                    if (blockWaist == waterId) {
                        if (input.isActionActive("JUMP")) {
                            velocity.y += 15.0f * deltaTime;
                            if (velocity.y > 4.0f) velocity.y = 4.0f;
                        } else if (input.isActionActive("SNEAK")) {
                            velocity.y -= 15.0f * deltaTime;
                            if (velocity.y < -4.0f) velocity.y = -4.0f;
                        } else {
                            velocity.y -= 2.0f * deltaTime;
                            if (velocity.y < -2.0f) velocity.y = -2.0f;
                        }
                    } else {
                        if (input.isActionActive("JUMP")) {
                            if (onGround) {
                                velocity.y = jumpForce;
                                onGround = false;
                            } else {
                                velocity.y += 15.0f * deltaTime;
                                if (velocity.y > 3.0f) velocity.y = 3.0f;
                            }
                        } else {
                            velocity.y += gravity * deltaTime;
                        }
                    }
                } else if (onGround && input.isActionActive("JUMP")) {
                    velocity.y = jumpForce;
                    onGround = false;
                }
            }
        } else {
            velocity.x = 0;
            velocity.z = 0;
            if (isInWater && !isFlying) velocity.y *= 0.9f;
        }

        // ==========================================
        // Kollisions-Updates
        // ==========================================
        if (gameMode != GameMode.SPECTATOR) {
            pushOutOfBlocks(chunkManager, input, deltaTime);
        }

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

        Block b = BlockRegistry.get(blockHead);
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
        // ... (Dein bisheriger Kollisionscode, der bleibt absolut identisch)
        int byFeet = (int) Math.floor(position.y + 0.1f);
        int byHead = (int) Math.floor(position.y + height * 0.8f);
        boolean isStuck = false;

        float checkW = 0.3f - 0.01f;

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

        // Prüfen, ob IRGENDEIN UI Element offen ist (Spieler-Inv, Kisten-Inv, Chat)
        boolean isUIOpen = inventory.isOpen() || isChatOpen || getOpenedInventory() != null;

        if (isUIOpen != wasUIOpen) {
            if (isUIOpen) {
                // Cursor sichtbar machen, Kamera stoppen
                org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
            } else {
                org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED);
                camera.resetMouseTracking();
            }
            wasUIOpen = isUIOpen;
        }

        if (!isUIOpen) {
            camera.update(windowHandle, deltaTime, smoothEyePos);
        } else {
            camera.setPosition(smoothEyePos);
        }
    }

    public void setChatOpen(boolean chatOpen) { this.isChatOpen = chatOpen; }
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