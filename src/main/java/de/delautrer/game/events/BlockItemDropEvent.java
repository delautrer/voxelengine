package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.ItemStack;
import org.joml.Vector3i;

public class BlockItemDropEvent implements Event {
    public final Vector3i blockPos;
    public final BlockState brokenState;
    private ItemStack drop;
    private boolean cancelled = false;

    public BlockItemDropEvent(Vector3i blockPos, BlockState brokenState, ItemStack initialDrop) {
        this.blockPos = blockPos;
        this.brokenState = brokenState;
        this.drop = initialDrop;
    }

    public ItemStack getDrop() { return drop; }
    public void setDrop(ItemStack drop) { this.drop = drop; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}