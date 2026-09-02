package de.delautrer.game.nbt;

import java.util.Arrays;

public class IntArrayTag extends VsnbtTag {
    private final int[] value;

    public IntArrayTag(int[] value) {
        this.value = value != null ? value : new int[0];
    }

    public int[] getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_INT_ARRAY;
    }

    @Override
    public VsnbtTag copy() {
        int[] copy = new int[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return new IntArrayTag(copy);
    }

    @Override
    public String asString() {
        return "[I; " + value.length + " ints]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntArrayTag that = (IntArrayTag) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
