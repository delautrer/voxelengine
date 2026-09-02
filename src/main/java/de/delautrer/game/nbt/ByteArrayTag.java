package de.delautrer.game.nbt;

import java.util.Arrays;

public class ByteArrayTag extends VsnbtTag {
    private final byte[] value;

    public ByteArrayTag(byte[] value) {
        this.value = value != null ? value : new byte[0];
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_BYTE_ARRAY;
    }

    @Override
    public VsnbtTag copy() {
        byte[] copy = new byte[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return new ByteArrayTag(copy);
    }

    @Override
    public String asString() {
        return "[B; " + value.length + " bytes]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteArrayTag that = (ByteArrayTag) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
