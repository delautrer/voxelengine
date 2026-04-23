package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import org.joml.Vector3i;

public class BlockBreakEvent implements Event {
    public final LocalPlayer player;
    public final Vector3i pos;
    public final BlockState state;
    private boolean cancelled = false;

    public BlockBreakEvent(LocalPlayer player, Vector3i pos, BlockState state) {
        this.player = player;
        this.pos = pos;
        this.state = state;
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}