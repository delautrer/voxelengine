package de.delautrer.game.nbt;

import java.util.Objects;

public class ByteTag extends VsnbtTag {
    private final byte value;

    public ByteTag(byte value) {
        this.value = value;
    }

    public ByteTag(boolean value) {
        this.value = (byte) (value ? 1 : 0);
    }

    public byte getValue() {
        return value;
    }

    public boolean getAsBoolean() {
        return value != 0;
    }

    @Override
    public byte getId() {
        return TYPE_BYTE;
    }

    @Override
    public VsnbtTag copy() {
        return new ByteTag(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value) + "b";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteTag byteTag = (ByteTag) o;
        return value == byteTag.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
