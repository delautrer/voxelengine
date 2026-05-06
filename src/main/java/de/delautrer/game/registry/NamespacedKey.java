package de.delautrer.game.registry;

import java.util.Objects;
import de.delautrer.Constants;

public class NamespacedKey {
    private final String namespace;
    private final String key;

    public NamespacedKey(String namespace, String key) {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        this.namespace = namespace.toLowerCase();
        this.key = key.toLowerCase();
    }

    public static NamespacedKey fromString(String value) {
        if (value == null)
            return null;
        String[] parts = value.split(":");
        if (parts.length == 2) {
            return new NamespacedKey(parts[0], parts[1]);
        } else if (parts.length == 1) {
            return new NamespacedKey(Constants.NAMESPACE, parts[0]); // Default fallback
        }
        throw new IllegalArgumentException("Invalid NamespacedKey format: " + value);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return namespace + ":" + key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NamespacedKey that = (NamespacedKey) o;
        return namespace.equals(that.namespace) && key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }
}
