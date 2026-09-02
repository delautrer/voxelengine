package de.delautrer.game.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ListTag extends VsnbtTag {
    private final List<VsnbtTag> list = new ArrayList<>();
    private byte elementType = TYPE_END;

    public ListTag() {}

    public ListTag(byte elementType) {
        this.elementType = elementType;
    }

    public byte getElementType() {
        return elementType;
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public VsnbtTag get(int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    public boolean add(VsnbtTag tag) {
        if (tag == null) return false;
        if (list.isEmpty()) {
            this.elementType = tag.getId();
        } else if (tag.getId() != this.elementType) {
            return false; // Typed list enforcement
        }
        list.add(tag);
        return true;
    }

    public List<VsnbtTag> getList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public byte getId() {
        return TYPE_LIST;
    }

    @Override
    public VsnbtTag copy() {
        ListTag copy = new ListTag(this.elementType);
        for (VsnbtTag tag : list) {
            copy.add(tag.copy());
        }
        return copy;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i).asString());
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListTag listTag = (ListTag) o;
        return elementType == listTag.elementType && Objects.equals(list, listTag.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, elementType);
    }
}
