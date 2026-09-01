package de.delautrer.game.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Registry<T> {
    private final Map<NamespacedKey, T> registryMap = new HashMap<>();
    private final Map<T, NamespacedKey> reverseMap = new HashMap<>();
    private final java.util.List<T> rawIdList = new java.util.ArrayList<>();
    private final Map<T, Integer> rawIdMap = new HashMap<>();
    private boolean frozen = false;

    public void register(NamespacedKey key, T item) {
        if (frozen) {
            throw new IllegalStateException("Registry is frozen! Cannot register key: " + key);
        }
        if (registryMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already registered: " + key);
        }
        registryMap.put(key, item);
        reverseMap.put(item, key);
        if (!rawIdMap.containsKey(item)) {
            rawIdList.add(item);
            rawIdMap.put(item, rawIdList.size() - 1);
        }
    }

    public void freeze() {
        this.frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public T get(NamespacedKey key) {
        return registryMap.get(key);
    }

    public T get(String keyString) {
        if (keyString == null) return null;
        return get(NamespacedKey.fromString(keyString));
    }

    public T getOrThrow(NamespacedKey key) {
        T val = get(key);
        if (val == null) {
            throw new IllegalArgumentException("Registry value missing for key: " + key);
        }
        return val;
    }

    public T getOrThrow(String keyString) {
        NamespacedKey nk = NamespacedKey.fromString(keyString);
        T val = get(nk);
        if (val == null) {
            throw new IllegalArgumentException("Registry value missing for key: " + keyString);
        }
        return val;
    }

    public int getRawId(T item) {
        return rawIdMap.getOrDefault(item, -1);
    }

    public T getByRawId(int rawId) {
        if (rawId >= 0 && rawId < rawIdList.size()) {
            return rawIdList.get(rawId);
        }
        return null;
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
