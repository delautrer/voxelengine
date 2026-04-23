package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.state.BlockState;
import org.joml.Vector3i;

public class BlockFallingEvent implements Event {
    public final Vector3i originalPos;
    public final BlockState state;

    public BlockFallingEvent(Vector3i originalPos, BlockState state) {
        this.originalPos = originalPos;
        this.state = state;
    }
}