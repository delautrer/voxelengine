package de.delautrer.game.interaction;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.World;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3i;
import de.delautrer.engine.events.EventBus;

public class PlayerInteraction {

    private final World world;
    private final Camera camera;
    private final Player player;
    private final VulkanContext vulkanContext;
    private final EventBus eventBus;
    private final Inventory inventory;

    private Vector3i selectedBlockPos = null;
    private Vector3i adjacentBlockPos = null;

    private float interactTimer = 0.0f;
    private final float INTERACT_COOLDOWN = 0.2f; // Cooldown etwas flüssiger gemacht

    public PlayerInteraction(World world, Camera camera, Player player, VulkanContext vulkanContext, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.vulkanContext = vulkanContext;
        this.eventBus = eventBus;
        this.inventory = new Inventory();
    }

    public void update(InputManager input, float deltaTime) {
        if (input.isActionJustPressed("INVENTORY")) {
            inventory.toggle();
            eventBus.publish(new de.delautrer.game.events.InventoryToggleEvent(inventory.isOpen()));
        }

        if (inventory.isOpen()) return;

        // Raycast
        World.RaycastResult result = world.raycast(camera.getPosition(), camera.getFront(), 6.0f);
        if (result != null) {
            selectedBlockPos = result.hitPos;
            adjacentBlockPos = result.adjacentPos;
        } else {
            selectedBlockPos = null;
            adjacentBlockPos = null;
        }

        if (!camera.isCursorCaptured()) return;

        // Tasten 1-9
        for (int i = 0; i < 9; i++) {
            if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                inventory.setSelectedSlot(i);
                eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(i));
            }
        }

        // Mausrad
        double scroll = input.consumeScroll();
        if (scroll != 0) {
            int newSlot = inventory.getSelectedSlot() - (int) Math.signum(scroll);
            if (newSlot < 0) newSlot = 8;
            else if (newSlot > 8) newSlot = 0;

            inventory.setSelectedSlot(newSlot);
            eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(newSlot));
        }

        // Pick Block
        if (input.isActionJustPressed("PICK_BLOCK") && selectedBlockPos != null) {
            byte targetId = world.getBlockAt(selectedBlockPos);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.getStack(i);
                if (stack != null && stack.type instanceof BlockItem) {
                    if (((BlockItem)stack.type).block.getId() == targetId) {
                        inventory.setSelectedSlot(i);
                        eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(i));
                        break;
                    }
                }
            }
        }

        // Interaktion (Abbauen / Bauen)
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
            ItemStack heldStack = inventory.getSelectedHotbarStack();
            if (heldStack == null) return;

            // Delegiert das Platzieren an das Item.
            // WICHTIG: Das BlockItem muss in 'onUseRightClick' jetzt ebenfalls 'world.setBlock()' aufrufen!
            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);
        }
    }

    public Vector3i getSelectedBlockPos() { return selectedBlockPos; }
    public Inventory getInventory() { return inventory; }
}