package de.delautrer.game.nbt;

import java.util.Objects;

public class LongTag extends VsnbtTag {
    private final long value;

    public LongTag(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_LONG;
    }

    @Override
    public VsnbtTag copy() {
        return new LongTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value) + "L";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongTag longTag = (LongTag) o;
        return value == longTag.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
