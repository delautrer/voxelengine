package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.Block;
import org.joml.Vector3i;

public class BlockNeighborUpdateEvent implements Event {
    public final Vector3i pos;
    public final Vector3i source;
    public final Block changedBlock;

    public BlockNeighborUpdateEvent(Vector3i pos, Vector3i source, Block changedBlock) {
        this.pos = pos;
        this.source = source;
        this.changedBlock = changedBlock;
    }
}