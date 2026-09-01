package de.delautrer.game.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Tag<T> {
    private final NamespacedKey key;
    private final boolean replace;
    private final Set<String> rawValues;
    private final Set<T> resolvedElements = new HashSet<>();

    public Tag(NamespacedKey key, boolean replace, Set<String> rawValues) {
        this.key = key;
        this.replace = replace;
        this.rawValues = rawValues;
    }

    public NamespacedKey getKey() { return key; }
    public boolean isReplace() { return replace; }
    public Set<String> getRawValues() { return rawValues; }
    public Set<T> getElements() { return Collections.unmodifiableSet(resolvedElements); }
    public void addElement(T element) { if (element != null) resolvedElements.add(element); }
    public boolean contains(T element) { return resolvedElements.contains(element); }
}