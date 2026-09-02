package de.delautrer.game.nbt;

import java.util.Objects;

public class DoubleTag extends VsnbtTag {
    private final double value;

    public DoubleTag(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_DOUBLE;
    }

    @Override
    public VsnbtTag copy() {
        return new DoubleTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value) + "d";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleTag doubleTag = (DoubleTag) o;
        return Double.compare(doubleTag.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
