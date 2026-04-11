package de.delautrer.game.interaction;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3i;
import org.lwjgl.vulkan.VK10;
import de.delautrer.engine.events.EventBus;

public class PlayerInteraction {

    private final World world;
    private final Camera camera;
    private final Player player;
    private final VulkanContext vulkanContext;
    private final EventBus eventBus;
    private final Inventory inventory;

    private int hoveredSlot = -1;
    private Vector3i selectedBlockPos = null;
    private Vector3i adjacentBlockPos = null; // NEU: Merken wir uns direkt aus dem Raycast

    public PlayerInteraction(World world, Camera camera, Player player, VulkanContext vulkanContext, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.vulkanContext = vulkanContext;
        this.eventBus = eventBus;
        this.inventory = new Inventory();
    }

    public void update(InputManager input) {
        if (input.isActionJustPressed("INVENTORY")) {
            inventory.toggle();
            eventBus.publish(new de.delautrer.game.events.InventoryToggleEvent(inventory.isOpen()));
        }

        if (inventory.isOpen()) {
            hoveredSlot = inventory.getClickedSlot(input.getMouseX(), input.getMouseY(), input.getWindowWidth(), input.getWindowHeight());
            boolean hasItem = (hoveredSlot != -1 && inventory.getStack(hoveredSlot) != null);
            boolean holdingItem = (inventory.getMouseStack() != null);
            input.setCursorHover(hasItem || holdingItem);

            if (input.isActionJustPressed("INTERACT_BREAK")) {
                if (hoveredSlot != -1) inventory.handleSlotClick(hoveredSlot);
            }
            return;
        } else {
            hoveredSlot = -1;
        }

        // --- DER NEUE, SAUBERE RAYCAST ---
        World.RaycastResult result = world.raycast(camera.getPosition(), camera.getFront(), 6.0f);
        if (result != null) {
            selectedBlockPos = result.hitPos;
            adjacentBlockPos = result.adjacentPos;
        } else {
            selectedBlockPos = null;
            adjacentBlockPos = null;
        }

        if (!camera.isCursorCaptured()) return;

        for (int i = 0; i < 9; i++) {
            if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                inventory.setSelectedSlot(i);
                eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(i));
            }
        }

        if (input.isActionJustPressed("INTERACT_BREAK")) handleMouseClick(true);
        else if (input.isActionJustPressed("INTERACT_PLACE")) handleMouseClick(false);
    }

    public int getHoveredSlot() { return hoveredSlot; }

    private void handleMouseClick(boolean isBreak) {
        if (selectedBlockPos == null) return;

        if (isBreak) {
            Chunk targetChunk = world.getChunkManager().getChunkAtBlock(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
            if (targetChunk != null) {
                targetChunk.setBlock(Math.floorMod(selectedBlockPos.x, Chunk.SIZE), selectedBlockPos.y, Math.floorMod(selectedBlockPos.z, Chunk.SIZE), (byte)0);
                updateChunkMesh(targetChunk, Math.floorMod(selectedBlockPos.x, Chunk.SIZE), Math.floorMod(selectedBlockPos.z, Chunk.SIZE));
            }
        } else {
            ItemStack heldStack = inventory.getSelectedHotbarStack();
            if (heldStack == null) return;

            // Wir übergeben einfach die adjacentBlockPos, die wir vom DDA-Algorithmus bekommen haben!
            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);
        }
    }

    // ACHTUNG: Die Methode calculateAdjacentPos() existiert hier nicht mehr! (Einfach löschen)

    public void updateChunkMesh(Chunk c, int localX, int localZ) {
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
        c.rebuildMesh(world.getChunkManager());

        de.delautrer.engine.graphics.VulkanMesh mesh = world.getChunkManager().getMeshes().get(new org.joml.Vector2i(c.getWorldX(), c.getWorldZ()));
        if (mesh != null) mesh.updateMesh(c);

        if (localX == 0) rebuildNeighbor(c.getWorldX() - 1, c.getWorldZ());
        if (localX == Chunk.SIZE - 1) rebuildNeighbor(c.getWorldX() + 1, c.getWorldZ());
        if (localZ == 0) rebuildNeighbor(c.getWorldX(), c.getWorldZ() - 1);
        if (localZ == Chunk.SIZE - 1) rebuildNeighbor(c.getWorldX(), c.getWorldZ() + 1);
    }

    private void rebuildNeighbor(int worldX, int worldZ) {
        Chunk neighbor = world.getChunkManager().getChunkAtBlock(worldX * Chunk.SIZE, 0, worldZ * Chunk.SIZE);
        if (neighbor != null) {
            neighbor.rebuildMesh(world.getChunkManager());
            de.delautrer.engine.graphics.VulkanMesh mesh = world.getChunkManager().getMeshes().get(new org.joml.Vector2i(worldX, worldZ));
            if (mesh != null) mesh.updateMesh(neighbor);
        }
    }

    public Vector3i getSelectedBlockPos() { return selectedBlockPos; }
    public Inventory getInventory() { return inventory; }

    public byte getSelectedBlockType() {
        ItemStack stack = inventory.getSelectedHotbarStack();
        if (stack == null || !(stack.type instanceof BlockItem)) return 0;
        return ((BlockItem)stack.type).block.getId();
    }
}