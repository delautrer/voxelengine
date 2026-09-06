package de.delautrer.game.world;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.events.BlockChangeEvent;
import de.delautrer.game.events.BlockNeighborUpdateEvent;

public class WorldEventHandler {

    private final World world;
    private final EventBus eventBus;

    // Listener-Referenzen als Felder gespeichert, damit unsubscribe() korrekt funktioniert.
    // this::onBlockChange erzeugt jedes Mal ein neues Objekt — als Feld ist die Referenz stabil.
    private final EventListener<BlockChangeEvent> blockChangeListener = this::onBlockChange;
    private final EventListener<BlockNeighborUpdateEvent> neighborUpdateListener = this::onNeighborUpdate;

    public WorldEventHandler(World world, EventBus eventBus) {
        this.world = world;
        this.eventBus = eventBus;

        eventBus.subscribe(BlockChangeEvent.class, blockChangeListener);
        eventBus.subscribe(BlockNeighborUpdateEvent.class, neighborUpdateListener);
    }

    private void onBlockChange(BlockChangeEvent event) {
        int x = event.pos.x;
        int y = event.pos.y;
        int z = event.pos.z;
        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        ChunkManager cm = world.getChunkManager();

        if (event.playSound) {
            // Spieler-Edit: Haupt-Chunk sofort auf dem Main-Thread neu bauen
            cm.rebuildChunkMeshImmediate(event.chunk);

            // Direkte Chunk-Naht-Nachbarn sofort neu bauen (verhindert Lücken auf Chunk-Grenzen)
            if (localX == 0) rebuildChunkMeshImmediateAtBlock(x - 1, z);
            if (localX == Chunk.SIZE - 1) rebuildChunkMeshImmediateAtBlock(x + 1, z);
            if (localZ == 0) rebuildChunkMeshImmediateAtBlock(x, z - 1);
            if (localZ == Chunk.SIZE - 1) rebuildChunkMeshImmediateAtBlock(x, z + 1);
        } else {
            // Worldgen / Jigsaw / Strukturen: Async belassen
            cm.requestMeshUpdate(event.chunk);

            if (localX == 0) markChunkMeshDirty(x - 1, z);
            if (localX == Chunk.SIZE - 1) markChunkMeshDirty(x + 1, z);
            if (localZ == 0) markChunkMeshDirty(x, z - 1);
            if (localZ == Chunk.SIZE - 1) markChunkMeshDirty(x, z + 1);
        }

        // Diagonale Ecken in allen Fällen async belassen (requestMeshUpdate)
        if (localX == 0 && localZ == 0) markChunkMeshDirty(x - 1, z - 1);
        if (localX == 0 && localZ == Chunk.SIZE - 1) markChunkMeshDirty(x - 1, z + 1);
        if (localX == Chunk.SIZE - 1 && localZ == 0) markChunkMeshDirty(x + 1, z - 1);
        if (localX == Chunk.SIZE - 1 && localZ == Chunk.SIZE - 1) markChunkMeshDirty(x + 1, z + 1);
    }

    private void rebuildChunkMeshImmediateAtBlock(int blockX, int blockZ) {
        Chunk c = world.getChunkManager().getChunkAtBlock(blockX, 0, blockZ);
        if (c != null) world.getChunkManager().rebuildChunkMeshImmediate(c);
    }

    private void markChunkMeshDirty(int blockX, int blockZ) {
        Chunk c = world.getChunkManager().getChunkAtBlock(blockX, 0, blockZ);
        if (c != null) world.getChunkManager().requestMeshUpdate(c);
    }

    private void onNeighborUpdate(BlockNeighborUpdateEvent event) {
        Block receiverBlock = world.getBlock(event.pos.x, event.pos.y, event.pos.z);
        if (receiverBlock != null && !receiverBlock.isAir()) {
            receiverBlock.onNeighborChanged(world, event.pos.x, event.pos.y, event.pos.z, event.source, event.changedBlock);
        }
    }

    public void cleanup() {
        if (eventBus != null) {
            eventBus.unsubscribe(BlockChangeEvent.class, blockChangeListener);
            eventBus.unsubscribe(BlockNeighborUpdateEvent.class, neighborUpdateListener);
        }
    }
}
