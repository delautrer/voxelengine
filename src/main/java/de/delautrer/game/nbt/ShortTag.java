package de.delautrer.game.nbt;

import java.util.Objects;

public class ShortTag extends VsnbtTag {
    private final short value;

    public ShortTag(short value) {
        this.value = value;
    }

    public short getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_SHORT;
    }

    @Override
    public VsnbtTag copy() {
        return new ShortTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value) + "s";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortTag shortTag = (ShortTag) o;
        return value == shortTag.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
