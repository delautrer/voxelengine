package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import org.joml.Vector3i;

public class BlockNeighborUpdateEvent implements Event {
    public final Vector3i pos;
    public final Vector3i neighborPos;
    public final byte changedNeighborId;

    public BlockNeighborUpdateEvent(Vector3i pos, Vector3i neighborPos, byte changedNeighborId) {
        this.pos = pos;
        this.neighborPos = neighborPos;
        this.changedNeighborId = changedNeighborId;
    }
}