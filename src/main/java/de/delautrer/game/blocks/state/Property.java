package de.delautrer.game.blocks.state;

import java.util.Collection;

public abstract class Property<T extends Comparable<T>> {
    private final String name;
    private final Class<T> valueClass;

    protected Property(String name, Class<T> valueClass) {
        this.name = name;
        this.valueClass = valueClass;
    }

    public String getName() { return name; }
    public Class<T> getValueClass() { return valueClass; }

    public abstract Collection<T> getAllowedValues();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Property<?> other) return this.name.equals(other.name);
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}