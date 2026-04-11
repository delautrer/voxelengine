package de.delautrer.game.events;
import de.delautrer.engine.events.Event;

public class DebugToggleEvent implements Event {
    public final boolean isVisible;
    public DebugToggleEvent(boolean isVisible) {
        this.isVisible = isVisible;
    }
}