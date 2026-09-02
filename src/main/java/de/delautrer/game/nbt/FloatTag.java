package de.delautrer.game.nbt;

import java.util.Objects;

public class FloatTag extends VsnbtTag {
    private final float value;

    public FloatTag(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_FLOAT;
    }

    @Override
    public VsnbtTag copy() {
        return new FloatTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value) + "f";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatTag floatTag = (FloatTag) o;
        return Float.compare(floatTag.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
