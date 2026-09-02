package de.delautrer.game.nbt;

import java.util.Objects;

public class IntTag extends VsnbtTag {
    private final int value;

    public IntTag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_INT;
    }

    @Override
    public VsnbtTag copy() {
        return new IntTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntTag intTag = (IntTag) o;
        return value == intTag.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
