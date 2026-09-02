package de.delautrer.game.nbt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CompoundTag extends VsnbtTag {
    private final Map<String, VsnbtTag> map = new HashMap<>();

    public void put(String key, VsnbtTag tag) {
        if (key != null && tag != null) {
            map.put(key, tag);
        }
    }

    public VsnbtTag get(String key) {
        return map.get(key);
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public boolean contains(String key, byte typeId) {
        VsnbtTag tag = map.get(key);
        return tag != null && tag.getId() == typeId;
    }

    public void remove(String key) {
        map.remove(key);
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public Set<Map.Entry<String, VsnbtTag>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    // --- Put Helpers ---
    public void putByte(String key, byte value) { put(key, new ByteTag(value)); }
    public void putBoolean(String key, boolean value) { put(key, new ByteTag(value)); }
    public void putShort(String key, short value) { put(key, new ShortTag(value)); }
    public void putInt(String key, int value) { put(key, new IntTag(value)); }
    public void putLong(String key, long value) { put(key, new LongTag(value)); }
    public void putFloat(String key, float value) { put(key, new FloatTag(value)); }
    public void putDouble(String key, double value) { put(key, new DoubleTag(value)); }
    public void putString(String key, String value) { put(key, new StringTag(value)); }
    public void putByteArray(String key, byte[] value) { put(key, new ByteArrayTag(value)); }
    public void putIntArray(String key, int[] value) { put(key, new IntArrayTag(value)); }
    public void putLongArray(String key, long[] value) { put(key, new LongArrayTag(value)); }

    // --- Get Helpers ---
    public byte getByte(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof ByteTag bt) return bt.getValue();
        return 0;
    }

    public boolean getBoolean(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof ByteTag bt) return bt.getAsBoolean();
        return false;
    }

    public short getShort(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof ShortTag st) return st.getValue();
        if (tag instanceof IntTag it) return (short) it.getValue();
        return 0;
    }

    public int getInt(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof IntTag it) return it.getValue();
        if (tag instanceof ByteTag bt) return bt.getValue();
        if (tag instanceof ShortTag st) return st.getValue();
        return 0;
    }

    public long getLong(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof LongTag lt) return lt.getValue();
        if (tag instanceof IntTag it) return it.getValue();
        return 0L;
    }

    public float getFloat(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof FloatTag ft) return ft.getValue();
        if (tag instanceof DoubleTag dt) return (float) dt.getValue();
        return 0.0f;
    }

    public double getDouble(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof DoubleTag dt) return dt.getValue();
        if (tag instanceof FloatTag ft) return ft.getValue();
        return 0.0;
    }

    public String getString(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof StringTag st) return st.getValue();
        return "";
    }

    public byte[] getByteArray(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof ByteArrayTag bat) return bat.getValue();
        return new byte[0];
    }

    public int[] getIntArray(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof IntArrayTag iat) return iat.getValue();
        return new int[0];
    }

    public long[] getLongArray(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof LongArrayTag lat) return lat.getValue();
        return new long[0];
    }

    public CompoundTag getCompound(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof CompoundTag ct) return ct;
        return new CompoundTag();
    }

    public ListTag getList(String key) {
        VsnbtTag tag = get(key);
        if (tag instanceof ListTag lt) return lt;
        return new ListTag();
    }

    @Override
    public byte getId() {
        return TYPE_COMPOUND;
    }

    @Override
    public VsnbtTag copy() {
        CompoundTag copy = new CompoundTag();
        for (Map.Entry<String, VsnbtTag> entry : map.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, VsnbtTag> entry : map.entrySet()) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue().asString());
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompoundTag that = (CompoundTag) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}
