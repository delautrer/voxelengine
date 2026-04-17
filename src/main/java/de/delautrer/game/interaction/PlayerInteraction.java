package de.delautrer.game.interaction;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.player.Inventory;
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

    public void update(InputManager input, float deltaTime) {
        if (player.getInventory().isOpen()) return;
        if (clickCooldown > 0) {
            clickCooldown -= deltaTime;
            return; // WICHTIG: Bricht das Update ab, keine Klicks für diese Zeit!
        }

        // 1. Raycast
        Raycaster.RaycastResult result = Raycaster.raycast(world, camera.getPosition(), camera.getFront(), 6.0f);
        if (result != null) {
            selectedBlockPos = result.hitPos;
            adjacentBlockPos = result.adjacentPos;
        } else {
            selectedBlockPos = null;
            adjacentBlockPos = null;
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
            world.setBlock(selectedBlockPos, (byte) 0);
        } else {
            if (adjacentBlockPos == null) return;
            ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
            if (heldStack == null) return;

            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);
        }
    }

    public Vector3i getSelectedBlockPos() { return selectedBlockPos; }
    public Inventory getInventory() { return player.getInventory(); }
    public void resetCooldown() {
        this.clickCooldown = 0.2f;
    }
}