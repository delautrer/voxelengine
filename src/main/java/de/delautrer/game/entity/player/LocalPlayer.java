package de.delautrer.game.entity.player;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.events.PlayerDamageEvent;
import de.delautrer.game.events.PlayerItemDropEvent;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import de.delautrer.Constants;
import de.delautrer.game.events.BlockSelectedEvent;
import de.delautrer.game.events.InventoryChangeEvent;
import de.delautrer.game.events.InventoryClosedEvent;
import de.delautrer.game.events.InventoryOpenedEvent;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.engine.physics.AABB;

import java.util.List;

public class LocalPlayer extends Player {

    private final Camera camera;
    private PlayerInteraction interaction;
    private EventBus eventBus;

    private boolean isChatOpen = false;
    private float lastSpacePressTime = 0.0f;
    private double cameraVisualYOffset = 0.0;
    private final float jumpForce = 9.0f;
    private final float speed = 5.0f;

    private float distanceWalked = 0.0f;

    private Block headBlock = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air");
    private double fallDistance = 0.0;
    private boolean wasOnGround = true;

    private boolean wasUIOpen = false;

    // --- NEU: Double-Tap Sprint Logik Variablen ---
    private float lastForwardPressTime = 0.0f;
    private boolean isForwardPressedLastFrame = false;

    public LocalPlayer(Vector3d spawnPosition) {
        super(spawnPosition);
        this.camera = new Camera();
    }

    public void initInteraction(World world, EventBus eventBus) {
        this.eventBus = eventBus;
        this.interaction = new PlayerInteraction(world, this.camera, this, eventBus);
    }

    public void updateLocal(InputManager input, ChunkManager chunkManager, float deltaTime) {
        if (isDead) {
            velocity.x = 0;
            velocity.z = 0;
            if (!onGround) {
                velocity.y += gravity * deltaTime;
            }
            pushOutOfBlocks(chunkManager, input, deltaTime);
            super.update(deltaTime, chunkManager);
            return;
        }

        if (gameMode == GameMode.SPECTATOR) {
            isFlying = true;
        }

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
        byte waterId = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water").getId();

        this.isInWater = (blockFeet == waterId || blockBody == waterId);
        this.isHeadInWater = (blockHead == waterId);

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
                if (getOpenedInventory() != null) {
                    eventBus.publish(new InventoryClosedEvent(this, getOpenedInventory()));
                    closeInventory();
                } else {
                    inventory.toggle();
                    eventBus.publish(new InventoryToggleEvent(inventory.isOpen()));

                    if (inventory.isOpen()) {
                        eventBus.publish(new InventoryOpenedEvent(this, inventory));
                    } else {
                        eventBus.publish(new InventoryClosedEvent(this, inventory));
                    }
                }
            }

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
                    if (newSlot < 0)
                        newSlot = 8;
                    else if (newSlot > 8)
                        newSlot = 0;
                    inventory.setSelectedSlot(newSlot);
                    eventBus.publish(new HotbarSlotChangeEvent(newSlot));
                    slotChanged = true;
                }

                if (slotChanged) {
                    ItemStack selectedStack = inventory.getStack(inventory.getSelectedSlot());
                    if (selectedStack != null && selectedStack.type instanceof BlockItem) {
                        BlockItem blockItem = (BlockItem) selectedStack.type;
                        eventBus.publish(new BlockSelectedEvent(blockItem.getBlock().getId()));
                    } else {
                        eventBus.publish(new BlockSelectedEvent((byte) 0));
                    }
                }
            }
        }

        boolean isUIOpen = inventory.isOpen() || isChatOpen || getOpenedInventory() != null || isDead;

        if (input.isActionJustPressed("DROP_ITEM") && !isUIOpen) {
            interaction.dropFromSlot(inventory.getSelectedSlot(), input.isControlDown());
        }

        // ==========================================
        // 2. STATUS-UPDATES (Sprinten & Schwimmen & Hitbox)
        // ==========================================
        if (!isUIOpen) {
            isSneaking = input.isActionActive("SNEAK");

            // --- NEU: Double-Tap Sprint Logik ---
            boolean isForwardPressedNow = input.isActionActive("MOVE_FORWARD");
            float currentTime = (float) org.lwjgl.glfw.GLFW.glfwGetTime();

            if (isForwardPressedNow && !isForwardPressedLastFrame) {
                // Key was just pressed
                if (currentTime - lastForwardPressTime < 0.3f && !isSneaking) {
                    isSprinting = true;
                }
                lastForwardPressTime = currentTime;
            } else if (!isForwardPressedNow) {
                // Stop sprinting if we stop moving forward
                isSprinting = false;
            }

            // Allow explicit sprint key to override
            if (input.isActionActive("SPRINT") && !isSneaking && isForwardPressedNow) {
                isSprinting = true;
            }

            isForwardPressedLastFrame = isForwardPressedNow;
            // -------------------------------------

            if (!isSprinting) {
                swimLock = false;
            }

            if (isInWater && gameMode != GameMode.SPECTATOR) {
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
            isForwardPressedLastFrame = false;
        }

        if (!isSwimming && swimProgress > 0.1f) {
            int headBlockWhenStanding = chunkManager.getWorld().getBlockAt(
                    (int) Math.floor(position.x),
                    (int) Math.floor(position.y + 1.8f),
                    (int) Math.floor(position.z));

            if (BlockRegistry.get((byte) headBlockWhenStanding).isSolid) {
                isSwimming = true;
            }
        }

        if (isSwimming) {
            swimProgress += deltaTime * 5.0f;
            if (swimProgress > 1.0f)
                swimProgress = 1.0f;
        } else {
            swimProgress -= deltaTime * 5.0f;
            if (swimProgress < 0.0f)
                swimProgress = 0.0f;
        }

        float targetBaseHeight = isSneaking ? 1.5f : 1.8f;
        this.height = targetBaseHeight + (0.6f - targetBaseHeight) * swimProgress;

        // ==========================================
        // 3. GESCHWINDIGKEIT BERECHNEN
        // ==========================================
        float currentSpeed = speed;
        if (isFlying) {
            // --- NEU: Sprinten modifiziert auch die Fluggeschwindigkeit ---
            if (gameMode == GameMode.SPECTATOR && isSprinting)
                currentSpeed *= 6.0f; // Extra schnell im Spectator
            else if (isSprinting)
                currentSpeed *= 3.0f; // Schneller fliegen beim Sprinten
            else
                currentSpeed *= 1.5f; // Default fliegen ist 1.5x
        } else if (isSwimming) {
            currentSpeed *= 1.3f;
        } else if (isInWater) {
            currentSpeed *= 0.4f;
        } else {
            if (isSneaking)
                currentSpeed *= 0.4f;
            else if (isSprinting)
                currentSpeed *= 1.5f;
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
                if (input.isActionActive("MOVE_FORWARD"))
                    moveDir.add(cameraFront);
                if (input.isActionActive("MOVE_BACKWARD"))
                    moveDir.sub(cameraFront);
                if (input.isActionActive("MOVE_LEFT"))
                    moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT"))
                    moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0)
                    moveDir.normalize().mul(currentSpeed);

                velocity.x = moveDir.x;
                velocity.z = moveDir.z;
                velocity.y = moveDir.y;

                int blockAtTop = chunkManager.getWorld().getBlockAt(bx, (int) Math.floor(position.y + height + 0.1f),
                        bz);
                if (blockAtTop != waterId && velocity.y > 0 && !input.isActionActive("JUMP")) {
                    velocity.y *= 0.1f;
                }

                if (input.isActionActive("JUMP"))
                    velocity.y += 3.5f;
                if (input.isActionActive("SNEAK"))
                    velocity.y -= 2.0f;

            } else if (isFlying) {
                if (input.isActionActive("MOVE_FORWARD"))
                    moveDir.add(flatFront);
                if (input.isActionActive("MOVE_BACKWARD"))
                    moveDir.sub(flatFront);
                if (input.isActionActive("MOVE_LEFT"))
                    moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT"))
                    moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0)
                    moveDir.normalize().mul(currentSpeed);
                velocity.x = moveDir.x;
                velocity.z = moveDir.z;

                velocity.y = 0;
                boolean jump = input.isActionActive("JUMP");
                boolean sneak = input.isActionActive("SNEAK");
                if (jump && !sneak) velocity.y = currentSpeed;
                else if (sneak && !jump) velocity.y = -currentSpeed;

            } else {
                if (input.isActionActive("MOVE_FORWARD"))
                    moveDir.add(flatFront);
                if (input.isActionActive("MOVE_BACKWARD"))
                    moveDir.sub(flatFront);
                if (input.isActionActive("MOVE_LEFT"))
                    moveDir.sub(flatRight);
                if (input.isActionActive("MOVE_RIGHT"))
                    moveDir.add(flatRight);

                if (moveDir.lengthSquared() > 0)
                    moveDir.normalize().mul(currentSpeed);
                velocity.x = moveDir.x;
                velocity.z = moveDir.z;

                if (isInWater) {
                    if (blockWaist == waterId) {
                        if (input.isActionActive("JUMP")) {
                            velocity.y += 15.0f * deltaTime;
                            if (velocity.y > 4.0f)
                                velocity.y = 4.0f;
                        } else if (input.isActionActive("SNEAK")) {
                            velocity.y -= 15.0f * deltaTime;
                            if (velocity.y < -4.0f)
                                velocity.y = -4.0f;
                        } else {
                            velocity.y -= 2.0f * deltaTime;
                            if (velocity.y < -2.0f)
                                velocity.y = -2.0f;
                        }
                    } else {
                        if (input.isActionActive("JUMP")) {
                            if (onGround) {
                                velocity.y = jumpForce;
                                onGround = false;
                            } else {
                                velocity.y += 15.0f * deltaTime;
                                if (velocity.y > 3.0f)
                                    velocity.y = 3.0f;
                            }
                        } else {
                            velocity.y += gravity * deltaTime;
                        }
                    }
                } else if (onGround && input.isActionActive("JUMP")) {
                    velocity.y = jumpForce;
                    onGround = false;
                    playMovementSound(chunkManager, "jump_start", 0.4f, 0.8f, 1.2f, "Player");
                }
            }
        } else {
            velocity.x = 0;
            velocity.z = 0;
            if (isInWater && !isFlying)
                velocity.y *= 0.9f;
        }

        // --- WASSERSTRÖMUNG ---
        if (isInWater && !isFlying && gameMode != GameMode.SPECTATOR) {
            int wx = (int) Math.floor(position.x);
            int wy = (int) Math.floor(position.y + 0.1f);
            int wz = (int) Math.floor(position.z);
            BlockState ws = chunkManager.getWorld().getBlockState(wx, wy, wz);
            if (ws.getBlock() instanceof de.delautrer.game.blocks.WaterBlock wb) {
                Vector3f flow = wb.getFlowDirection(chunkManager.getWorld(), wx, wy, wz);
                float strength = 2.0f; // Strömungsstärke
                velocity.x += flow.x * strength * deltaTime;
                velocity.z += flow.z * strength * deltaTime;
                velocity.y += flow.y * strength * deltaTime;
            }
        }

        if (gameMode != GameMode.SPECTATOR) {
            pushOutOfBlocks(chunkManager, input, deltaTime);
        }

        double prevY = position.y;

        if (gameMode == GameMode.SPECTATOR) {
            position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
        } else {
            super.update(deltaTime, chunkManager);
        }

        double deltaY = position.y - prevY;

        if (!wasInWater && isInWater && !onGround) {
            playMovementSound(chunkManager, "jump_land", 0.3f, 0.8f, 1.2f, "Player");
            fallDistance = 0.0f;
        }

        if (!wasOnGround && onGround && !isInWater) {
            playMovementSound(chunkManager, "jump_land", 0.3f, 0.8f, 1.2f, "Player");

            if (fallDistance > 3.0f && gameMode == GameMode.SURVIVAL) {
                float dmg = (float) Math.floor(fallDistance - 3.0f);
                if (dmg > 0) {
                    this.damage(dmg);
                    if (eventBus != null) {
                        eventBus.publish(new PlayerDamageEvent(this, dmg));
                    }
                }
            }
            fallDistance = 0.0f;
        } else if (!onGround && deltaY < 0 && !isFlying && !isInWater) {
            fallDistance += Math.abs(deltaY);
        } else if (isFlying || isInWater) {
            fallDistance = 0.0f;
        }

        wasOnGround = onGround;
        wasInWater = isInWater;

        if (!isFlying && (Math.abs(velocity.x) > 0.1f || Math.abs(velocity.z) > 0.1f
                || (isInWater && Math.abs(velocity.y) > 0.1f))) {

            float moveSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (isInWater)
                moveSpeed += Math.abs(velocity.y);

            distanceWalked += moveSpeed * deltaTime;

            float stepThreshold = isSprinting ? 1.4f : 1.2f;
            if (isInWater)
                if (isSwimming)
                    stepThreshold = 5.4f;
                else
                    stepThreshold = 1.5f;

            if (distanceWalked > stepThreshold) {
                distanceWalked = 0.0f;

                String action = isSprinting ? "run" : "walk";

                playMovementSound(chunkManager, action, 0.3f, 0.8f, 1.2f, "Player");
            }
        } else {
            distanceWalked = 0.0f;
        }

        if (deltaY > 0.0f && deltaY <= stepHeight && onGround) {
            cameraVisualYOffset -= deltaY;
        }

        cameraVisualYOffset += (0.0f - cameraVisualYOffset) * 15.0f * deltaTime;

        Block b = BlockRegistry.get(blockHead);
        if (b.isSolid && !b.isTransparent) {
            headBlock = b;
        } else {
            headBlock = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air");
        }

        if (interaction != null) {
            interaction.update(input, deltaTime);
        }
    }

    protected void pushOutOfBlocks(ChunkManager cm, InputManager input, float deltaTime) {
        AABB playerBB = getAABB();
        // Minimale Toleranz, um Jitter zu vermeiden, aber sensibel genug für Türen
        AABB checkBB = new AABB(
            new Vector3f(playerBB.min).add(0.001f, 0.001f, 0.001f),
            new Vector3f(playerBB.max).sub(0.001f, 0.001f, 0.001f)
        );

        List<AABB> potentialCollisions = getNearbyBoxes(cm, checkBB);
        AABB stuckBox = null;
        for (AABB box : potentialCollisions) {
            if (AABB.isColliding(checkBB, box)) {
                stuckBox = box;
                break;
            }
        }
        
        if (stuckBox == null) return;
        
        // Block-Koordinate bestimmen, zu der diese Box gehört
        int cx = (int) Math.floor(stuckBox.min.x + (stuckBox.max.x - stuckBox.min.x) * 0.5f);
        int cy = (int) Math.floor(stuckBox.min.y + (stuckBox.max.y - stuckBox.min.y) * 0.5f);
        int cz = (int) Math.floor(stuckBox.min.z + (stuckBox.max.z - stuckBox.min.z) * 0.5f);

        // Wir stecken fest! Jetzt den kürzesten Weg aus dem Block (cx, cy, cz) finden.
        double distLeft = position.x - cx;
        double distRight = (cx + 1.0) - position.x;
        double distBack = position.z - cz;
        double distFront = (cz + 1.0) - position.z;

        // Wir prüfen die Nachbarn. Ein Nachbar ist "frei", wenn er nicht voll-solid ist.
        boolean leftFree = isFreeForPush(cm, cx - 1, cy, cz);
        boolean rightFree = isFreeForPush(cm, cx + 1, cy, cz);
        boolean backFree = isFreeForPush(cm, cx, cy, cz - 1);
        boolean frontFree = isFreeForPush(cm, cx, cy, cz + 1);

        double minScore = 999.0;
        int escapeDir = -1;

        if (leftFree && distLeft < minScore) {
            minScore = distLeft;
            escapeDir = 0;
        }
        if (rightFree && distRight < minScore) {
            minScore = distRight;
            escapeDir = 1;
        }
        if (backFree && distBack < minScore) {
            minScore = distBack;
            escapeDir = 2;
        }
        if (frontFree && distFront < minScore) {
            minScore = distFront;
            escapeDir = 3;
        }

        // Fallback: Wenn kein Nachbar "frei" ist, trotzdem in die kürzeste Richtung schieben
        if (escapeDir == -1) {
            minScore = distLeft; escapeDir = 0;
            if (distRight < minScore) { minScore = distRight; escapeDir = 1; }
            if (distBack < minScore) { minScore = distBack; escapeDir = 2; }
            if (distFront < minScore) { minScore = distFront; escapeDir = 3; }
        }

        float pushSpeed = 4.0f * deltaTime;
        if (escapeDir == 0) position.x -= pushSpeed;
        if (escapeDir == 1) position.x += pushSpeed;
        if (escapeDir == 2) position.z -= pushSpeed;
        if (escapeDir == 3) position.z += pushSpeed;
    }

    private boolean isFreeForPush(ChunkManager cm, int x, int y, int z) {
        Block b = BlockRegistry.get(cm.getWorld().getBlockAt(x, y, z));
        // Frei für Push-Out sind alle Blöcke, die keine vollen undurchsichtigen Blöcke sind.
        return !b.isSolid || b.isTransparent || b.isPassable;
    }

    public void updateCamera(long windowHandle, float deltaTime) {
        Vector3d smoothEyePos = new Vector3d(getEyePosition());
        smoothEyePos.y += cameraVisualYOffset;

        boolean isUIOpen = inventory.isOpen() || isChatOpen || getOpenedInventory() != null || isDead;

        if (isUIOpen != wasUIOpen) {
            if (isUIOpen) {
                org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                        org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
            } else {
                org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                        org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED);
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

    public void setChatOpen(boolean chatOpen) {
        this.isChatOpen = chatOpen;
    }

    public boolean isChatOpen() {
        return isChatOpen;
    }

    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        if (mode == GameMode.SURVIVAL)
            this.isFlying = false;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public Camera getCamera() {
        return camera;
    }

    public PlayerInteraction getInteraction() {
        return interaction;
    }

    public Block getHeadBlock() {
        return headBlock;
    }

    public void respawn(Vector3d spawnPos) {
        this.position.set(spawnPos);
        this.velocity.set(0, 0, 0);
        this.currentHealth = this.maxHealth;
        this.isDead = false;
        this.fallDistance = 0.0f;

        if (interaction != null) {
            interaction.resetCooldown();
        }
    }

    @Override
    public Vector3d getEyePosition() {
        return new Vector3d(position.x, position.y + (this.height * 0.9f), position.z);
    }

    private void playMovementSound(ChunkManager chunkManager, String action, float volume, float minPitch, float maxPitch, String source) {
        int bx = (int) Math.floor(position.x);
        int byFeet = (int) Math.floor(position.y + 0.1f);
        int bz = (int) Math.floor(position.z);

        byte blockInsideId = chunkManager.getWorld().getBlockAt(bx, byFeet, bz);
        Block blockInside = BlockRegistry.get(blockInsideId);

        if (blockInsideId != 0 && (!blockInside.isSolid
                || blockInside == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water"))) {
            SoundManager.playEvent(blockInside.getSoundMaterialName(), action, volume, minPitch, maxPitch, source);
            return;
        }

        int footY = (int) Math.floor(position.y - 0.2f);
        byte groundBlockId = chunkManager.getWorld().getBlockAt(bx, footY, bz);

        if (groundBlockId != 0) {
            Block groundBlock = BlockRegistry.get(groundBlockId);
            SoundManager.playEvent(groundBlock.getSoundMaterialName(), action, volume, minPitch, maxPitch, source);
        }
    }
}
