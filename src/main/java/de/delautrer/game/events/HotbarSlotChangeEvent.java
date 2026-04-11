package de.delautrer.game.events;
import de.delautrer.engine.events.Event;

public class HotbarSlotChangeEvent implements Event {
    public final int newSlot;
    public HotbarSlotChangeEvent(int newSlot) {
        this.newSlot = newSlot;
    }
}