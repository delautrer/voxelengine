package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import org.joml.Vector3i;

public class BlockPlaceEvent implements Event {
    public final LocalPlayer player;
    public final Vector3i pos;
    public final BlockState stateToPlace;
    private boolean cancelled = false;

    public BlockPlaceEvent(LocalPlayer player, Vector3i pos, BlockState stateToPlace) {
        this.player = player;
        this.pos = pos;
        this.stateToPlace = stateToPlace;
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}