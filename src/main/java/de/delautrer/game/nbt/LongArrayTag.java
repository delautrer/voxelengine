package de.delautrer.game.nbt;

import java.util.Arrays;

public class LongArrayTag extends VsnbtTag {
    private final long[] value;

    public LongArrayTag(long[] value) {
        this.value = value != null ? value : new long[0];
    }

    public long[] getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_LONG_ARRAY;
    }

    @Override
    public VsnbtTag copy() {
        long[] copy = new long[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return new LongArrayTag(copy);
    }

    @Override
    public String asString() {
        return "[L; " + value.length + " longs]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongArrayTag that = (LongArrayTag) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
