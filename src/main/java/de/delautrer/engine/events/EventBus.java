package de.delautrer.engine.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners = new HashMap<>();

    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void cleanup() {
        listeners.clear();
    }

    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<? extends Event>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);

            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        List<EventListener<? extends Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<? extends Event> listener : eventListeners) {
                ((EventListener<Event>) listener).onEvent(event);
            }
        }
    }

}