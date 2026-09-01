package de.delautrer.engine.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    public enum Priority {
        LOWEST, LOW, NORMAL, HIGH, HIGHEST
    }

    private static class PrioritizedListener<T extends Event> implements Comparable<PrioritizedListener<?>> {
        final EventListener<T> listener;
        final Priority priority;

        PrioritizedListener(EventListener<T> listener, Priority priority) {
            this.listener = listener;
            this.priority = priority != null ? priority : Priority.NORMAL;
        }

        @Override
        public int compareTo(PrioritizedListener<?> o) {
            return Integer.compare(o.priority.ordinal(), this.priority.ordinal());
        }
    }

    private final Map<Class<? extends Event>, List<PrioritizedListener<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        subscribe(eventType, listener, Priority.NORMAL);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener, Priority priority) {
        listeners.compute(eventType, (k, existing) -> {
            List<PrioritizedListener<? extends Event>> copy = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            copy.add(new PrioritizedListener<>(listener, priority));
            Collections.sort((List) copy);
            return new CopyOnWriteArrayList<>(copy);
        });
    }

    public void cleanup() {
        listeners.clear();
    }

    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<PrioritizedListener<? extends Event>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.removeIf(p -> p.listener.equals(listener));
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        if (event == null) return;
        Class<?> current = event.getClass();

        boolean isCancellable = event instanceof CancellableEvent;

        while (current != null && Event.class.isAssignableFrom(current)) {
            List<PrioritizedListener<? extends Event>> eventListeners = listeners.get(current);
            if (eventListeners != null && !eventListeners.isEmpty()) {
                if (isCancellable && ((CancellableEvent) event).isCancelled()) {
                    break;
                }
                for (PrioritizedListener<? extends Event> entry : eventListeners) {
                    if (isCancellable && ((CancellableEvent) event).isCancelled()) {
                        break;
                    }
                    ((EventListener<Event>) entry.listener).onEvent(event);
                }
            }
            if (isCancellable && ((CancellableEvent) event).isCancelled()) {
                break;
            }
            current = current.getSuperclass();
        }
    }
}