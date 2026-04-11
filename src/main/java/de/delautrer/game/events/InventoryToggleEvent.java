package de.delautrer.game.events;
import de.delautrer.engine.events.Event;

public class InventoryToggleEvent implements Event {
    public final boolean isOpen;
    public InventoryToggleEvent(boolean isOpen) { this.isOpen = isOpen; }
}