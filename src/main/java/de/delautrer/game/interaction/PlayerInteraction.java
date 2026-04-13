package de.delautrer.game.interaction;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.physics.AABB;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.LightEngine;
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
                int localX = Math.floorMod(selectedBlockPos.x, Chunk.SIZE);
                int localY = selectedBlockPos.y;
                int localZ = Math.floorMod(selectedBlockPos.z, Chunk.SIZE);

                byte oldBlockId = targetChunk.getBlock(localX, localY, localZ);
                int oldEmission = de.delautrer.game.blocks.BlockRegistry.get(oldBlockId).getLightEmission();

                if (oldEmission > 0) {
                    world.getChunkManager().getLightEngine().removeBlockLight(selectedBlockPos.x, localY, selectedBlockPos.z, oldEmission);
                }

                targetChunk.setBlock(localX, localY, localZ, (byte)0);
                world.getChunkManager().getLightEngine().notifyBlockChanged(selectedBlockPos.x, localY, selectedBlockPos.z);
                updateChunkMesh(targetChunk, localX, localZ);
            }
        } else {
            ItemStack heldStack = inventory.getSelectedHotbarStack();
            if (heldStack == null) return;

            int oldBlockLight = 0;
            if (adjacentBlockPos != null) {
                oldBlockLight = world.getChunkManager().getLightEngine().getBlockLight(adjacentBlockPos.x, adjacentBlockPos.y, adjacentBlockPos.z);
            }

            heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);

            if (adjacentBlockPos != null) {
                byte placedBlockId = world.getBlockAt(adjacentBlockPos.x, adjacentBlockPos.y, adjacentBlockPos.z);
                if (placedBlockId != 0) {

                    LightEngine le = world.getChunkManager().getLightEngine();
                    de.delautrer.game.blocks.Block placedBlock = de.delautrer.game.blocks.BlockRegistry.get(placedBlockId);

                    if (!placedBlock.isTransparent && oldBlockLight > 0) {
                        le.removeBlockLight(adjacentBlockPos.x, adjacentBlockPos.y, adjacentBlockPos.z, oldBlockLight);
                    }

                    int newEmission = placedBlock.getLightEmission();
                    if (newEmission > 0) {
                        le.addBlockLightSource(adjacentBlockPos.x, adjacentBlockPos.y, adjacentBlockPos.z, newEmission);
                    }

                    Chunk targetChunk = world.getChunkManager().getChunkAtBlock(adjacentBlockPos.x, adjacentBlockPos.y, adjacentBlockPos.z);
                    if (targetChunk != null) {
                        int localX = Math.floorMod(adjacentBlockPos.x, Chunk.SIZE);
                        int localZ = Math.floorMod(adjacentBlockPos.z, Chunk.SIZE);
                        updateChunkMesh(targetChunk, localX, localZ);
                    }
                }
            }
        }
    }

    public void updateChunkMesh(Chunk c, int localX, int localZ) {
        LightEngine le = world.getChunkManager().getLightEngine();

        c.recalculateSunlightColumn(localX, localZ, le);
        le.processLightUpdates();

        java.util.Set<Chunk> chunksToRebuild = le.getAndClearDirtiedChunks();
        chunksToRebuild.add(c);

        if (localX == 0) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock((c.getWorldX() - 1) * Chunk.SIZE, 0, c.getWorldZ() * Chunk.SIZE));
        if (localX == Chunk.SIZE - 1) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock((c.getWorldX() + 1) * Chunk.SIZE, 0, c.getWorldZ() * Chunk.SIZE));
        if (localZ == 0) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock(c.getWorldX() * Chunk.SIZE, 0, (c.getWorldZ() - 1) * Chunk.SIZE));
        if (localZ == Chunk.SIZE - 1) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock(c.getWorldX() * Chunk.SIZE, 0, (c.getWorldZ() + 1) * Chunk.SIZE));

        chunksToRebuild.remove(null);

        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());

        for (Chunk chunkToUpdate : chunksToRebuild) {
            chunkToUpdate.rebuildMesh(world.getChunkManager());
            de.delautrer.engine.graphics.VulkanMesh mesh = world.getChunkManager().getMeshes().get(new org.joml.Vector2i(chunkToUpdate.getWorldX(), chunkToUpdate.getWorldZ()));
            if (mesh != null) mesh.updateMesh(chunkToUpdate);
        }
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