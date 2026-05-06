package de.delautrer.game.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Registry<T> {
    private final Map<NamespacedKey, T> registryMap = new HashMap<>();
    private final Map<T, NamespacedKey> reverseMap = new HashMap<>();

    public void register(NamespacedKey key, T item) {
        if (registryMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already registered: " + key);
        }
        registryMap.put(key, item);
        reverseMap.put(item, key);
    }

    public T get(NamespacedKey key) {
        return registryMap.get(key);
    }

    public T get(String keyString) {
        return get(NamespacedKey.fromString(keyString));
    }

    public NamespacedKey getKey(T item) {
        return reverseMap.get(item);
    }

    public boolean contains(NamespacedKey key) {
        return registryMap.containsKey(key);
    }

    public Collection<T> values() {
        return Collections.unmodifiableCollection(registryMap.values());
    }

    public Set<NamespacedKey> keys() {
        return Collections.unmodifiableSet(registryMap.keySet());
    }

    public Set<Map.Entry<NamespacedKey, T>> entrySet() {
        return Collections.unmodifiableSet(registryMap.entrySet());
    }

    public int size() {
        return registryMap.size();
    }
}
