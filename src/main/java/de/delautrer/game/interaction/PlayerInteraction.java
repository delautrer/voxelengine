package de.delautrer.game.interaction;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.vulkan.VK10;
import de.delautrer.engine.events.EventBus;
import de.delautrer.game.items.ItemStack;

public class PlayerInteraction {

    private final World world;
    private final Camera camera;
    private final Player player;
    private final VulkanContext vulkanContext;
    private final EventBus eventBus;
    private final Inventory inventory;

    private int hoveredSlot = -1;
    private Vector3i selectedBlockPos = null;

    public PlayerInteraction(World world, Camera camera, Player player, VulkanContext vulkanContext, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.vulkanContext = vulkanContext;
        this.eventBus = eventBus;
        this.inventory = new Inventory();
    }

    public void update(InputManager input) {
        // Inventar öffnen/schließen
        if (input.isActionJustPressed("INVENTORY")) {
            inventory.toggle();
            eventBus.publish(new de.delautrer.game.events.InventoryToggleEvent(inventory.isOpen()));
        }

        // --- INVENTAR LOGIK ---
        if (inventory.isOpen()) {
            // 1. Welcher Slot liegt unter der Maus?
            hoveredSlot = inventory.getClickedSlot(input.getMouseX(), input.getMouseY(), input.getWindowWidth(), input.getWindowHeight());

            // 2. Greifer-Maus anzeigen, wenn ein Item da ist oder wir eins in der Hand haben!
            boolean hasItem = (hoveredSlot != -1 && inventory.getStack(hoveredSlot) != null);
            boolean holdingItem = (inventory.getMouseStack() != null);
            input.setCursorHover(hasItem || holdingItem);

            // 3. Klick verarbeiten (DRAG AND DROP)
            if (input.isActionJustPressed("INTERACT_BREAK")) {
                if (hoveredSlot != -1) {
                    inventory.handleSlotClick(hoveredSlot);
                }
            }
            return; // WICHTIG: Keine Welt-Blöcke abbauen!
        } else {
            hoveredSlot = -1;
        }

        // --- NORMALE WELT INTERAKTION ---
        selectedBlockPos = world.raycast(camera.getPosition(), camera.getFront(), 6.0f);
        if (!camera.isCursorCaptured()) return;

        for (int i = 0; i < 9; i++) {
            if (input.isActionJustPressed("SLOT_" + (i + 1))) inventory.setSelectedSlot(i);
        }

        if (input.isActionJustPressed("INTERACT_BREAK")) handleMouseClick(true);
        else if (input.isActionJustPressed("INTERACT_PLACE")) handleMouseClick(false);
    }

    public int getHoveredSlot() { return hoveredSlot; }

    private void handleInventoryLogic(InputManager input) {
        if (input.isActionJustPressed("INTERACT_BREAK")) { // Linksklick im Inventar
            int slot = inventory.getClickedSlot(input.getMouseX(), input.getMouseY(), input.getWindowWidth(), input.getWindowHeight());
            if (slot != -1) inventory.handleSlotClick(slot);
        }
    }

    private void handleMouseClick(boolean breakBlock) {
        if (selectedBlockPos == null) return;

        if (breakBlock) {
            Chunk targetChunk = world.getChunkManager().getChunkAtBlock(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
            if (targetChunk != null) {
                targetChunk.setBlock(Math.floorMod(selectedBlockPos.x, Chunk.SIZE), selectedBlockPos.y, Math.floorMod(selectedBlockPos.z, Chunk.SIZE), (byte)0);
                updateChunkMesh(targetChunk, Math.floorMod(selectedBlockPos.x, Chunk.SIZE), Math.floorMod(selectedBlockPos.z, Chunk.SIZE));
            }
        } else {
            // Logik für das Platzieren eines Blocks aus dem Inventar
            ItemStack heldStack = inventory.getSelectedHotbarStack();
            if (heldStack == null || !heldStack.type.isPlaceable) return;

            byte typeToPlace = heldStack.type.associatedBlock.id;

            Vector3f pos = new Vector3f(camera.getPosition());
            Vector3f dir = new Vector3f(camera.getFront());
            float step = 0.01f;
            Vector3f currentPos = new Vector3f(pos);
            Vector3i lastEmptyPos = new Vector3i((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));

            for(float d = 0; d < 6.0f; d += step) {
                int bx = (int) Math.floor(currentPos.x);
                int by = (int) Math.floor(currentPos.y);
                int bz = (int) Math.floor(currentPos.z);

                byte hitBlock = world.getBlockAt(bx, by, bz);

                if (hitBlock != 0 && hitBlock != 4) {
                    de.delautrer.engine.physics.AABB blockBB = new de.delautrer.engine.physics.AABB(
                            new Vector3f(lastEmptyPos.x, lastEmptyPos.y, lastEmptyPos.z),
                            new Vector3f(lastEmptyPos.x + 1, lastEmptyPos.y + 1, lastEmptyPos.z + 1)
                    );

                    if (!de.delautrer.engine.physics.AABB.isColliding(player.getAABB(), blockBB)) {
                        Chunk placeChunk = world.getChunkManager().getChunkAtBlock(lastEmptyPos.x, lastEmptyPos.y, lastEmptyPos.z);
                        if (placeChunk != null) {
                            int lx = Math.floorMod(lastEmptyPos.x, Chunk.SIZE);
                            int lz = Math.floorMod(lastEmptyPos.z, Chunk.SIZE);

                            if (typeToPlace == 4) placeChunk.setBlock(lx, lastEmptyPos.y, lz, (byte)4, (byte)8);
                            else placeChunk.setBlock(lx, lastEmptyPos.y, lz, typeToPlace);

                            updateChunkMesh(placeChunk, lx, lz);
                        }
                    }
                    break;
                }
                lastEmptyPos.set(bx, by, bz);
                currentPos.add(dir.x * step, dir.y * step, dir.z * step);
            }
        }
    }

    private void updateChunkMesh(Chunk c, int localX, int localZ) {
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
        if (stack == null || stack.type.associatedBlock == null) return 0;
        return stack.type.associatedBlock.id;
    }
}