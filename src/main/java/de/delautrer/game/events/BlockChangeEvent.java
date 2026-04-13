package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.world.Chunk;
import org.joml.Vector3i;

public class BlockChangeEvent implements Event {
    public final Vector3i pos;
    public final byte oldBlockId;
    public final byte newBlockId;
    public final Chunk chunk;

    public BlockChangeEvent(Vector3i pos, byte oldBlockId, byte newBlockId, Chunk chunk) {
        this.pos = pos;
        this.oldBlockId = oldBlockId;
        this.newBlockId = newBlockId;
        this.chunk = chunk;
    }
}