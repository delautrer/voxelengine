package de.delautrer.game.nbt;

import com.google.gson.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class TagIo {

    // ==========================================
    // Binary I/O (DataInputStream / DataOutputStream)
    // ==========================================

    public static void writeCompound(CompoundTag compound, DataOutputStream dos) throws IOException {
        if (compound == null) {
            dos.writeByte(VsnbtTag.TYPE_END);
            return;
        }
        dos.writeByte(VsnbtTag.TYPE_COMPOUND);
        writeTagPayload(compound, dos);
    }

    public static CompoundTag readCompound(DataInputStream dis) throws IOException {
        byte typeId = dis.readByte();
        if (typeId == VsnbtTag.TYPE_END) {
            return new CompoundTag();
        }
        if (typeId != VsnbtTag.TYPE_COMPOUND) {
            throw new IOException("Expected TYPE_COMPOUND (10) but found: " + typeId);
        }
        return (CompoundTag) readTagPayload(VsnbtTag.TYPE_COMPOUND, dis);
    }

    public static void writeTag(VsnbtTag tag, DataOutputStream dos) throws IOException {
        if (tag == null) {
            dos.writeByte(VsnbtTag.TYPE_END);
            return;
        }
        dos.writeByte(tag.getId());
        writeTagPayload(tag, dos);
    }

    public static VsnbtTag readTag(DataInputStream dis) throws IOException {
        byte typeId = dis.readByte();
        if (typeId == VsnbtTag.TYPE_END) {
            return null;
        }
        return readTagPayload(typeId, dis);
    }

    private static void writeTagPayload(VsnbtTag tag, DataOutputStream dos) throws IOException {
        switch (tag.getId()) {
            case VsnbtTag.TYPE_BYTE -> dos.writeByte(((ByteTag) tag).getValue());
            case VsnbtTag.TYPE_SHORT -> dos.writeShort(((ShortTag) tag).getValue());
            case VsnbtTag.TYPE_INT -> dos.writeInt(((IntTag) tag).getValue());
            case VsnbtTag.TYPE_LONG -> dos.writeLong(((LongTag) tag).getValue());
            case VsnbtTag.TYPE_FLOAT -> dos.writeFloat(((FloatTag) tag).getValue());
            case VsnbtTag.TYPE_DOUBLE -> dos.writeDouble(((DoubleTag) tag).getValue());
            case VsnbtTag.TYPE_STRING -> dos.writeUTF(((StringTag) tag).getValue());
            case VsnbtTag.TYPE_BYTE_ARRAY -> {
                byte[] bytes = ((ByteArrayTag) tag).getValue();
                dos.writeInt(bytes.length);
                dos.write(bytes);
            }
            case VsnbtTag.TYPE_INT_ARRAY -> {
                int[] ints = ((IntArrayTag) tag).getValue();
                dos.writeInt(ints.length);
                for (int val : ints) dos.writeInt(val);
            }
            case VsnbtTag.TYPE_LONG_ARRAY -> {
                long[] longs = ((LongArrayTag) tag).getValue();
                dos.writeInt(longs.length);
                for (long val : longs) dos.writeLong(val);
            }
            case VsnbtTag.TYPE_LIST -> {
                ListTag listTag = (ListTag) tag;
                dos.writeByte(listTag.getElementType());
                dos.writeInt(listTag.size());
                for (VsnbtTag element : listTag.getList()) {
                    writeTagPayload(element, dos);
                }
            }
            case VsnbtTag.TYPE_COMPOUND -> {
                CompoundTag compoundTag = (CompoundTag) tag;
                for (Map.Entry<String, VsnbtTag> entry : compoundTag.entrySet()) {
                    dos.writeByte(entry.getValue().getId());
                    dos.writeUTF(entry.getKey());
                    writeTagPayload(entry.getValue(), dos);
                }
                dos.writeByte(VsnbtTag.TYPE_END);
            }
            default -> throw new IOException("Unknown tag type id: " + tag.getId());
        }
    }

    private static VsnbtTag readTagPayload(byte typeId, DataInputStream dis) throws IOException {
        return switch (typeId) {
            case VsnbtTag.TYPE_BYTE -> new ByteTag(dis.readByte());
            case VsnbtTag.TYPE_SHORT -> new ShortTag(dis.readShort());
            case VsnbtTag.TYPE_INT -> new IntTag(dis.readInt());
            case VsnbtTag.TYPE_LONG -> new LongTag(dis.readLong());
            case VsnbtTag.TYPE_FLOAT -> new FloatTag(dis.readFloat());
            case VsnbtTag.TYPE_DOUBLE -> new DoubleTag(dis.readDouble());
            case VsnbtTag.TYPE_STRING -> new StringTag(dis.readUTF());
            case VsnbtTag.TYPE_BYTE_ARRAY -> {
                int len = dis.readInt();
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                yield new ByteArrayTag(bytes);
            }
            case VsnbtTag.TYPE_INT_ARRAY -> {
                int len = dis.readInt();
                int[] ints = new int[len];
                for (int i = 0; i < len; i++) ints[i] = dis.readInt();
                yield new IntArrayTag(ints);
            }
            case VsnbtTag.TYPE_LONG_ARRAY -> {
                int len = dis.readInt();
                long[] longs = new long[len];
                for (int i = 0; i < len; i++) longs[i] = dis.readLong();
                yield new LongArrayTag(longs);
            }
            case VsnbtTag.TYPE_LIST -> {
                byte elemType = dis.readByte();
                int len = dis.readInt();
                ListTag listTag = new ListTag(elemType);
                for (int i = 0; i < len; i++) {
                    listTag.add(readTagPayload(elemType, dis));
                }
                yield listTag;
            }
            case VsnbtTag.TYPE_COMPOUND -> {
                CompoundTag compoundTag = new CompoundTag();
                byte entryTypeId;
                while ((entryTypeId = dis.readByte()) != VsnbtTag.TYPE_END) {
                    String name = dis.readUTF();
                    VsnbtTag entry = readTagPayload(entryTypeId, dis);
                    compoundTag.put(name, entry);
                }
                yield compoundTag;
            }
            default -> throw new IOException("Unknown tag type id: " + typeId);
        };
    }

    // ==========================================
    // JSON I/O (Gson JsonElement)
    // ==========================================

    public static JsonElement toJson(VsnbtTag tag) {
        if (tag == null) return JsonNull.INSTANCE;

        return switch (tag.getId()) {
            case VsnbtTag.TYPE_BYTE -> new JsonPrimitive(((ByteTag) tag).getValue());
            case VsnbtTag.TYPE_SHORT -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "short");
                obj.addProperty("value", ((ShortTag) tag).getValue());
                yield obj;
            }
            case VsnbtTag.TYPE_INT -> new JsonPrimitive(((IntTag) tag).getValue());
            case VsnbtTag.TYPE_LONG -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "long");
                obj.addProperty("value", ((LongTag) tag).getValue());
                yield obj;
            }
            case VsnbtTag.TYPE_FLOAT -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "float");
                obj.addProperty("value", ((FloatTag) tag).getValue());
                yield obj;
            }
            case VsnbtTag.TYPE_DOUBLE -> new JsonPrimitive(((DoubleTag) tag).getValue());
            case VsnbtTag.TYPE_STRING -> new JsonPrimitive(((StringTag) tag).getValue());
            case VsnbtTag.TYPE_BYTE_ARRAY -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "byte_array");
                JsonArray arr = new JsonArray();
                for (byte b : ((ByteArrayTag) tag).getValue()) arr.add(b);
                obj.add("value", arr);
                yield obj;
            }
            case VsnbtTag.TYPE_INT_ARRAY -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "int_array");
                JsonArray arr = new JsonArray();
                for (int i : ((IntArrayTag) tag).getValue()) arr.add(i);
                obj.add("value", arr);
                yield obj;
            }
            case VsnbtTag.TYPE_LONG_ARRAY -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", "long_array");
                JsonArray arr = new JsonArray();
                for (long l : ((LongArrayTag) tag).getValue()) arr.add(l);
                obj.add("value", arr);
                yield obj;
            }
            case VsnbtTag.TYPE_LIST -> {
                JsonArray arr = new JsonArray();
                for (VsnbtTag elem : ((ListTag) tag).getList()) {
                    arr.add(toJson(elem));
                }
                yield arr;
            }
            case VsnbtTag.TYPE_COMPOUND -> {
                JsonObject obj = new JsonObject();
                CompoundTag compound = (CompoundTag) tag;
                for (Map.Entry<String, VsnbtTag> entry : compound.entrySet()) {
                    obj.add(entry.getKey(), toJson(entry.getValue()));
                }
                yield obj;
            }
            default -> JsonNull.INSTANCE;
        };
    }

    public static VsnbtTag fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) return null;

        if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return new ByteTag(prim.getAsBoolean());
            }
            if (prim.isString()) {
                return new StringTag(prim.getAsString());
            }
            if (prim.isNumber()) {
                String strVal = prim.getAsString();
                if (strVal.contains(".") || strVal.contains("e") || strVal.contains("E")) {
                    return new DoubleTag(prim.getAsDouble());
                } else {
                    try {
                        return new IntTag(prim.getAsInt());
                    } catch (NumberFormatException e) {
                        return new LongTag(prim.getAsLong());
                    }
                }
            }
        }

        if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            ListTag listTag = new ListTag();
            for (JsonElement elem : arr) {
                VsnbtTag tag = fromJson(elem);
                if (tag != null) {
                    listTag.add(tag);
                }
            }
            return listTag;
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();

            // Explicit Typed Wrapper Check (type + value)
            if (obj.has("type") && obj.has("value") && obj.entrySet().size() == 2) {
                String typeStr = obj.get("type").getAsString().toLowerCase();
                JsonElement val = obj.get("value");
                switch (typeStr) {
                    case "byte" -> {
                        return new ByteTag(val.getAsByte());
                    }
                    case "short" -> {
                        return new ShortTag(val.getAsShort());
                    }
                    case "int" -> {
                        return new IntTag(val.getAsInt());
                    }
                    case "long" -> {
                        return new LongTag(val.getAsLong());
                    }
                    case "float" -> {
                        return new FloatTag(val.getAsFloat());
                    }
                    case "double" -> {
                        return new DoubleTag(val.getAsDouble());
                    }
                    case "string" -> {
                        return new StringTag(val.getAsString());
                    }
                    case "byte_array" -> {
                        JsonArray arr = val.getAsJsonArray();
                        byte[] bytes = new byte[arr.size()];
                        for (int i = 0; i < arr.size(); i++) bytes[i] = arr.get(i).getAsByte();
                        return new ByteArrayTag(bytes);
                    }
                    case "int_array" -> {
                        JsonArray arr = val.getAsJsonArray();
                        int[] ints = new int[arr.size()];
                        for (int i = 0; i < arr.size(); i++) ints[i] = arr.get(i).getAsInt();
                        return new IntArrayTag(ints);
                    }
                    case "long_array" -> {
                        JsonArray arr = val.getAsJsonArray();
                        long[] longs = new long[arr.size()];
                        for (int i = 0; i < arr.size(); i++) longs[i] = arr.get(i).getAsLong();
                        return new LongArrayTag(longs);
                    }
                }
            }

            // Normal CompoundTag
            CompoundTag compound = new CompoundTag();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                VsnbtTag tag = fromJson(entry.getValue());
                if (tag != null) {
                    compound.put(entry.getKey(), tag);
                }
            }
            return compound;
        }

        return null;
    }
}
