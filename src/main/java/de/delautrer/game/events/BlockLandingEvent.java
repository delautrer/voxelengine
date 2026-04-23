package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.state.BlockState;
import org.joml.Vector3i;

public class BlockLandingEvent implements Event {
    public final Vector3i landingPos;
    public final BlockState state;
    private boolean cancelled = false;

    public BlockLandingEvent(Vector3i landingPos, BlockState state) {
        this.landingPos = landingPos;
        this.state = state;
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}