package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.entity.player.LocalPlayer;

public class InventoryOpenedEvent implements Event {
    public final LocalPlayer player;
    public final IInventory inventory;

    public InventoryOpenedEvent(LocalPlayer player, IInventory inventory) {
        this.player = player;
        this.inventory = inventory;
    }
}