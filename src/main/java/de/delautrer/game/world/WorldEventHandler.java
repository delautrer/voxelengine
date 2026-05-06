package de.delautrer.game.world;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.events.BlockChangeEvent;
import de.delautrer.game.events.BlockNeighborUpdateEvent;

public class WorldEventHandler {

    private final World world;
    private final VulkanContext vulkanContext;
    private final EventBus eventBus;

    public WorldEventHandler(World world, VulkanContext context, EventBus eventBus) {
        this.world = world;
        this.vulkanContext = context;
        this.eventBus = eventBus;

        // EventListener registrieren
        eventBus.subscribe(BlockChangeEvent.class, this::onBlockChange);
        eventBus.subscribe(BlockNeighborUpdateEvent.class, this::onNeighborUpdate);
    }

    private void onBlockChange(BlockChangeEvent event) {
        int x = event.pos.x;
        int y = event.pos.y;
        int z = event.pos.z;
        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        LightEngine le = world.getChunkManager().getLightEngine();

        int oldBlockLight = le.getBlockLight(x, y, z);
        if (oldBlockLight > 0) {
            le.removeBlockLight(x, y, z, oldBlockLight);
        }

        int oldSkyLight = le.getSkyLight(x, y, z);
        if (oldSkyLight > 0) {
            le.setSkyLight(x, y, z, 0);
            le.addSkyLightRemoval(x, y, z, oldSkyLight);
        }

        event.chunk.recalculateSunlightColumn(localX, localZ, le);

        Block newBlock = BlockRegistry.get(event.newBlockId);
        int newEmission = newBlock.getLightEmission();
        if (newEmission > 0) {
            le.addBlockLightSource(x, y, z, newEmission);
        }

        le.notifyBlockChanged(x, y, z);

        event.chunk.requestMeshUpdate();

        if (localX == 0) markChunkMeshDirty(x - 1, z);
        if (localX == Chunk.SIZE - 1) markChunkMeshDirty(x + 1, z);
        if (localZ == 0) markChunkMeshDirty(x, z - 1);
        if (localZ == Chunk.SIZE - 1) markChunkMeshDirty(x, z + 1);
    }

    private void markChunkMeshDirty(int blockX, int blockZ) {
        Chunk c = world.getChunkManager().getChunkAtBlock(blockX, 0, blockZ);
        if (c != null) c.requestMeshUpdate();
    }

    private void onNeighborUpdate(BlockNeighborUpdateEvent event) {
        byte receiverId = world.getBlockAt(event.pos.x, event.pos.y, event.pos.z);
        if (receiverId != 0) {
            Block receiverBlock = BlockRegistry.get(receiverId);
            receiverBlock.onNeighborChanged(world, event.pos.x, event.pos.y, event.pos.z, event.neighborPos, event.changedNeighborId);
        }
    }


    public void cleanup() {
        if(eventBus != null) {
            eventBus.unsubscribe(BlockChangeEvent.class, this::onBlockChange);
            eventBus.unsubscribe(BlockNeighborUpdateEvent.class, this::onNeighborUpdate);
        }
    }
}
