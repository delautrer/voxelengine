package de.delautrer.game.entity.player;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.IInteractable;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.events.BlockBreakEvent;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.ItemType;
import de.delautrer.game.world.World;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3i;

public class PlayerInteraction {

    private final World world;
    private final Camera camera;
    private final LocalPlayer player;
    private final VulkanContext vulkanContext;
    private final EventBus eventBus;

    private Vector3i selectedBlockPos = null;
    private Vector3i adjacentBlockPos = null;

    private float interactTimer = 0.0f;
    private final float INTERACT_COOLDOWN = 0.2f;
    private float clickCooldown = 0.0f;

    private Vector3i currentlyMiningPos = null;
    private float miningProgress = 0.0f;

    public PlayerInteraction(World world, Camera camera, LocalPlayer player, VulkanContext vulkanContext, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.vulkanContext = vulkanContext;
        this.eventBus = eventBus;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public void update(InputManager input, float deltaTime) {
        if (player.isDead() || player.getInventory().isOpen() || player.isChatOpen() || player.getOpenedInventory() != null) {
            selectedBlockPos = null;
            adjacentBlockPos = null;
            return;
        }

        if (clickCooldown > 0) {
            clickCooldown -= deltaTime;
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR) return;

        Block headBlock = player.getHeadBlock();

        // 1. Raycast (Kopf steckt fest oder normal)
        if (headBlock != BlockRegistry.AIR) {
            org.joml.Vector3f eyePos = player.getEyePosition();

            selectedBlockPos = new org.joml.Vector3i(
                    (int) Math.floor(eyePos.x),
                    (int) Math.floor(eyePos.y),
                    (int) Math.floor(eyePos.z)
            );

            // hitFace = de.delautrer.game.blocks.state.BlockProperties.BlockFace.UP;
        } else {
            // 1.2 Sonst normaler raycast
            Raycaster.RaycastResult result = Raycaster.raycast(world, camera.getPosition(), camera.getFront(), 6.0f);
            if (result != null) {
                selectedBlockPos = result.hitPos;
                adjacentBlockPos = result.adjacentPos;
            } else {
                selectedBlockPos = null;
                adjacentBlockPos = null;
            }
        }

        if (!camera.isCursorCaptured()) return;

        // 2. Pick Block (Mittlere Maustaste)
        if (input.isActionJustPressed("PICK_BLOCK") && selectedBlockPos != null) {
            byte targetId = world.getBlockAt(selectedBlockPos);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack != null && stack.type instanceof BlockItem) {
                    if (((BlockItem)stack.type).block.getId() == targetId) {
                        player.getInventory().setSelectedSlot(i);
                        eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(i));
                        break;
                    }
                }
            }
        }

        // 3. Interaktion (Abbauen / Bauen)
        if (interactTimer > 0) interactTimer -= deltaTime;

        if (input.isActionActive("INTERACT_BREAK")) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                // Im Creative Mode bleibt es Instant-Break
                miningProgress = 0.0f;
                currentlyMiningPos = null;
                if (interactTimer <= 0) {
                    handleMouseClick(true);
                    interactTimer = INTERACT_COOLDOWN;
                }
            } else if (player.getGameMode() == GameMode.SURVIVAL) {
                // Im Survival Mode: Härte und Zeit berechnen
                if (selectedBlockPos != null) {
                    // Prüfen, ob wir immer noch denselben Block anschauen
                    if (currentlyMiningPos == null || !currentlyMiningPos.equals(selectedBlockPos)) {
                        currentlyMiningPos = new Vector3i(selectedBlockPos);
                        miningProgress = 0.0f; // Reset, wenn man wegschaut
                    }

                    byte blockId = world.getBlockAt(selectedBlockPos);
                    Block targetBlock = BlockRegistry.get(blockId);

                    if (targetBlock != BlockRegistry.AIR && targetBlock.getHardness() >= 0) {
                        miningProgress += deltaTime;

                        // Formel: Wie lange dauert der Abbau? (Base-Härte * 1.5 Sekunden als Richtwert)
                        // Später kannst du hier Werkzeuge einberechnen (z.B. miningProgress += deltaTime * toolMultiplier)
                        float requiredTime = targetBlock.getHardness() * 1.5f;

                        if (miningProgress >= requiredTime) {
                            handleSurvivalBreak(targetBlock, blockId);
                            miningProgress = 0.0f;
                            currentlyMiningPos = null;
                            interactTimer = INTERACT_COOLDOWN;
                        }
                    } else {
                        miningProgress = 0.0f; // Unzerstörbar (Bedrock) oder Luft
                    }
                } else {
                    miningProgress = 0.0f;
                    currentlyMiningPos = null;
                }
            }
        } else {
            // Maustaste losgelassen: Alles zurücksetzen
            miningProgress = 0.0f;
            currentlyMiningPos = null;

            // Platzieren-Logik (unverändert)
            if (input.isActionActive("INTERACT_PLACE") && interactTimer <= 0) {
                handleMouseClick(false);
                interactTimer = INTERACT_COOLDOWN;
            } else if (!input.isActionActive("INTERACT_PLACE")) {
                interactTimer = 0.0f;
            }
        }
    }

    private void handleSurvivalBreak(Block block, byte blockId) {
        if (selectedBlockPos == null) return;

        BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
        de.delautrer.game.events.BlockBreakEvent breakEvent = new de.delautrer.game.events.BlockBreakEvent(player, selectedBlockPos, state);

        eventBus.publish(breakEvent);

        if (!breakEvent.isCancelled()) {
            world.setBlock(selectedBlockPos, (byte) 0);

            // Item Drop generieren (Wir droppen das passende BlockItem)
            /*
            ItemType dropItemType = null;
            for (String key : ItemRegistry.getAll().keySet()) {
                ItemType type = ItemRegistry.get(key);
                if (type instanceof BlockItem blockItem) {
                    if (blockItem.getBlock().getId() == blockId) {
                        dropItemType = type;
                        break;
                    }
                }
            }

            if (dropItemType != null) {
                // Item in die Mitte des Blocks spawnen, mit einem kleinen "Plop" nach oben
                org.joml.Vector3f dropPos = new org.joml.Vector3f(
                        selectedBlockPos.x + 0.5f,
                        selectedBlockPos.y + 0.5f,
                        selectedBlockPos.z + 0.5f
                );
                org.joml.Vector3f dropVel = new org.joml.Vector3f(
                        (float)(Math.random() - 0.5) * 2.0f,
                        2.0f,
                        (float)(Math.random() - 0.5) * 2.0f
                );

                de.delautrer.game.entity.ItemEntity entity = new de.delautrer.game.entity.ItemEntity(new ItemStack(dropItemType, 1), dropPos, dropVel);
                world.spawnEntity(entity);
            }
            */
        }
    }

    private void handleMouseClick(boolean isBreak) {
        if (selectedBlockPos == null) return;

        if (isBreak) {
            BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
            BlockBreakEvent breakEvent = new BlockBreakEvent(player, selectedBlockPos, state);

            eventBus.publish(breakEvent);

            if (!breakEvent.isCancelled()) {
                world.setBlock(selectedBlockPos, (byte) 0);
            }
        } else {
            if (adjacentBlockPos == null) return;

            if (!player.isSneaking) {
                Block clickedBlock = BlockRegistry.get(world.getBlockAt(selectedBlockPos));

                if (clickedBlock instanceof IInteractable interactable) {
                    boolean handled = interactable.onInteract(world, selectedBlockPos, player);
                    if (handled) return;
                }
            }

            // --- 2. BLOCK ODER ITEM VERWENDEN ---
            ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
            if (heldStack == null || heldStack.type == null) return;

            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);
        }
    }

    public float getMiningProgressPercent() {
        if (currentlyMiningPos == null || selectedBlockPos == null || !currentlyMiningPos.equals(selectedBlockPos)) {
            return 0.0f;
        }

        byte blockId = world.getBlockAt(currentlyMiningPos);
        Block targetBlock = BlockRegistry.get(blockId);

        if (targetBlock == BlockRegistry.AIR || targetBlock.getHardness() < 0) {
            return 0.0f;
        }

        float requiredTime = targetBlock.getHardness() * 1.5f;
        return Math.min(1.0f, miningProgress / requiredTime);
    }

    public Vector3i getSelectedBlockPos() { return selectedBlockPos; }
    public PlayerInventory getInventory() { return player.getInventory(); }
    public void resetCooldown() {
        this.clickCooldown = 0.5f;
    }
    public EventBus getEventBus() {
        return eventBus;
    }
}