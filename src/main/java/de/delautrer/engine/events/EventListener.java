package de.delautrer.engine.events;

public interface EventListener<T extends Event> {
    void onEvent(T event);
}