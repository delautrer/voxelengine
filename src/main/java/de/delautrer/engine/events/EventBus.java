package de.delautrer.engine.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private final Map<Class<? extends Event>, List<EventListener>> listeners = new HashMap<>();

    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void cleanup() {
        listeners.clear();
    }

    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);

            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (int i = 0; i < eventListeners.size(); i++) {
                eventListeners.get(i).onEvent(event);
            }
        }
    }

}