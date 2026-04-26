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

        if (input.isActionActive("INTERACT_BREAK") && interactTimer <= 0) {
            handleMouseClick(true);
            interactTimer = INTERACT_COOLDOWN;
        } else if (input.isActionActive("INTERACT_PLACE") && interactTimer <= 0) {
            handleMouseClick(false);
            interactTimer = INTERACT_COOLDOWN;
        } else if (!input.isActionActive("INTERACT_BREAK") && !input.isActionActive("INTERACT_PLACE")){
            interactTimer = 0.0f;
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

    public Vector3i getSelectedBlockPos() { return selectedBlockPos; }
    public PlayerInventory getInventory() { return player.getInventory(); }
    public void resetCooldown() {
        this.clickCooldown = 0.5f;
    }
    public EventBus getEventBus() {
        return eventBus;
    }
}