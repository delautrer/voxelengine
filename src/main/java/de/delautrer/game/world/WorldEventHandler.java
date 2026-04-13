package de.delautrer.game.world;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.events.BlockChangeEvent;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK10;
import java.util.Set;

public class WorldEventHandler {

    private final World world;
    private final VulkanContext vulkanContext;

    public WorldEventHandler(World world, VulkanContext context, EventBus eventBus) {
        this.world = world;
        this.vulkanContext = context;
        eventBus.subscribe(BlockChangeEvent.class, this::onBlockChange);
    }

    private void onBlockChange(BlockChangeEvent event) {
        int x = event.pos.x;
        int y = event.pos.y;
        int z = event.pos.z;
        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        LightEngine le = world.getChunkManager().getLightEngine();

        // Altes Licht entfernen
        int oldEmission = BlockRegistry.get(event.oldBlockId).getLightEmission();
        if (oldEmission > 0) {
            le.removeBlockLight(x, y, z, oldEmission);
        }

        // Neues Licht hinzufügen & Updates benachrichtigen
        de.delautrer.game.blocks.Block newBlock = BlockRegistry.get(event.newBlockId);
        int newEmission = newBlock.getLightEmission();
        if (newEmission > 0) {
            le.addBlockLightSource(x, y, z, newEmission);
        }
        le.notifyBlockChanged(x, y, z);

        // Licht berechnen
        event.chunk.recalculateSunlightColumn(localX, localZ, le);
        le.processLightUpdates();

        Set<Chunk> chunksToRebuild = le.getAndClearDirtiedChunks();
        chunksToRebuild.add(event.chunk);

        if (localX == 0) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock((event.chunk.getWorldX() - 1) * Chunk.SIZE, 0, event.chunk.getWorldZ() * Chunk.SIZE));
        if (localX == Chunk.SIZE - 1) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock((event.chunk.getWorldX() + 1) * Chunk.SIZE, 0, event.chunk.getWorldZ() * Chunk.SIZE));
        if (localZ == 0) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock(event.chunk.getWorldX() * Chunk.SIZE, 0, (event.chunk.getWorldZ() - 1) * Chunk.SIZE));
        if (localZ == Chunk.SIZE - 1) chunksToRebuild.add(world.getChunkManager().getChunkAtBlock(event.chunk.getWorldX() * Chunk.SIZE, 0, (event.chunk.getWorldZ() + 1) * Chunk.SIZE));

        chunksToRebuild.remove(null);

        AsyncChunkBuilder asyncBuilder = world.getChunkManager().getAsyncBuilder();
        for (Chunk chunkToUpdate : chunksToRebuild) {
            asyncBuilder.queueRebuild(chunkToUpdate, world.getChunkManager());
        }
    }
}