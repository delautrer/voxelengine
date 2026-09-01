package de.delautrer.engine.events;

public interface CancellableEvent extends Event {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}