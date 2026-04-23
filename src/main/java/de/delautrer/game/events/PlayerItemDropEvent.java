package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.items.ItemStack;

public class PlayerItemDropEvent implements Event {
    public final LocalPlayer player;
    public final ItemStack stack;
    private boolean cancelled = false;

    public PlayerItemDropEvent(LocalPlayer player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}