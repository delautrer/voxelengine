package de.delautrer.game.nbt;

import java.util.Objects;

public class StringTag extends VsnbtTag {
    private final String value;

    public StringTag(String value) {
        this.value = value != null ? value : "";
    }

    public String getValue() {
        return value;
    }

    @Override
    public byte getId() {
        return TYPE_STRING;
    }

    @Override
    public VsnbtTag copy() {
        return new StringTag(value);
    }

    @Override
    public String asString() {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringTag stringTag = (StringTag) o;
        return Objects.equals(value, stringTag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
