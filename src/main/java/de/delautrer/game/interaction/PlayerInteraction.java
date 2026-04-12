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

    private Vector3i selectedBlockPos = null;
    private Vector3i adjacentBlockPos = null;

    private float interactTimer = 0.0f;
    private final float INTERACT_COOLDOWN = 0.52f;

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

        if (inventory.isOpen()) {
            return;
        }

        World.RaycastResult result = world.raycast(camera.getPosition(), camera.getFront(), 6.0f);
        if (result != null) {
            selectedBlockPos = result.hitPos;
            adjacentBlockPos = result.adjacentPos;
        } else {
            selectedBlockPos = null;
            adjacentBlockPos = null;
        }

        if (!camera.isCursorCaptured()) return;

        // --- FEATURE: TASTEN 1-9 ---
        for (int i = 0; i < 9; i++) {
            if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                inventory.setSelectedSlot(i);
                eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(i));
            }
        }

        // --- FEATURE: MAUSRAD SCROLLEN ---
        double scroll = input.consumeScroll();
        if (scroll != 0) {
            int newSlot = inventory.getSelectedSlot() - (int) Math.signum(scroll);

            if (newSlot < 0) newSlot = 8;
            else if (newSlot > 8) newSlot = 0;

            inventory.setSelectedSlot(newSlot);
            eventBus.publish(new de.delautrer.game.events.HotbarSlotChangeEvent(newSlot));
        }

        // --- FEATURE: PICK BLOCK (Mittlere Maustaste) ---
        if (input.isActionJustPressed("PICK_BLOCK")) {
            if (selectedBlockPos != null) {
                byte targetId = world.getBlockAt(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
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
        }

        if (interactTimer > 0) {
            interactTimer -= deltaTime;
        }

        if (input.isActionActive("INTERACT_BREAK")) {
            if (interactTimer <= 0) {
                handleMouseClick(true);
                interactTimer = INTERACT_COOLDOWN;
            }
        } else if (input.isActionActive("INTERACT_PLACE")) {
            if (interactTimer <= 0) {
                handleMouseClick(false);
                interactTimer = INTERACT_COOLDOWN;
            }
        } else {
            interactTimer = 0.0f;
        }
    }

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

            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);
        }
    }

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

}