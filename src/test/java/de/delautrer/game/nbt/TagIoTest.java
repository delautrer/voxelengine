package de.delautrer.game.nbt;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class TagIoTest {

    public static void main(String[] args) throws Exception {
        TagIoTest test = new TagIoTest();
        test.testBinaryRoundtrip();
        test.testJsonRoundtrip();
        System.out.println("VSNBT TagIoTest: All binary and JSON roundtrip tests passed successfully!");
    }

    @Test
    public void testBinaryRoundtrip() throws Exception {
        CompoundTag original = new CompoundTag();
        original.putByte("byteVal", (byte) 42);
        original.putShort("shortVal", (short) 1337);
        original.putInt("intVal", 999999);
        original.putLong("longVal", 123456789012345L);
        original.putFloat("floatVal", 3.14f);
        original.putDouble("doubleVal", 2.718281828459);
        original.putString("stringVal", "Veinstride VSNBT Test!");
        original.putByteArray("byteArrayVal", new byte[]{1, 2, 3, 4, 5});
        original.putIntArray("intArrayVal", new int[]{100, 200, 300});
        original.putLongArray("longArrayVal", new long[]{1000L, 2000L, 3000L});

        ListTag listTag = new ListTag();
        listTag.add(new StringTag("item1"));
        listTag.add(new StringTag("item2"));
        original.put("listVal", listTag);

        CompoundTag child = new CompoundTag();
        child.putString("childKey", "childValue");
        original.put("childCompound", child);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        TagIo.writeCompound(original, dos);
        dos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        CompoundTag deserialized = TagIo.readCompound(dis);

        try {
            Assertions.assertEquals(original, deserialized);
            Assertions.assertEquals((byte) 42, deserialized.getByte("byteVal"));
            Assertions.assertEquals((short) 1337, deserialized.getShort("shortVal"));
            Assertions.assertEquals(999999, deserialized.getInt("intVal"));
            Assertions.assertEquals(123456789012345L, deserialized.getLong("longVal"));
            Assertions.assertEquals(3.14f, deserialized.getFloat("floatVal"), 0.0001f);
            Assertions.assertEquals(2.718281828459, deserialized.getDouble("doubleVal"), 0.0000001);
            Assertions.assertEquals("Veinstride VSNBT Test!", deserialized.getString("stringVal"));
            Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, deserialized.getByteArray("byteArrayVal"));
            Assertions.assertArrayEquals(new int[]{100, 200, 300}, deserialized.getIntArray("intArrayVal"));
            Assertions.assertArrayEquals(new long[]{1000L, 2000L, 3000L}, deserialized.getLongArray("longArrayVal"));
            Assertions.assertEquals(2, deserialized.getList("listVal").size());
            Assertions.assertEquals("childValue", deserialized.getCompound("childCompound").getString("childKey"));
        } catch (Throwable t) {
            System.err.println("TEST FAIL BINARY: " + t.getMessage());
            t.printStackTrace();
            throw t;
        }
    }

    @Test
    public void testJsonRoundtrip() {
        CompoundTag original = new CompoundTag();
        original.putByte("byteVal", (byte) 10);
        original.putShort("shortVal", (short) 500);
        original.putInt("intVal", 12345);
        original.putLong("longVal", 9876543210L);
        original.putFloat("floatVal", 1.25f);
        original.putDouble("doubleVal", 9.876);
        original.putString("stringVal", "JSON Test");

        try {
            JsonElement json = TagIo.toJson(original);
            Assertions.assertNotNull(json);

            VsnbtTag deserialized = TagIo.fromJson(json);
            Assertions.assertTrue(deserialized instanceof CompoundTag);

            CompoundTag comp = (CompoundTag) deserialized;
            Assertions.assertEquals("JSON Test", comp.getString("stringVal"));
            Assertions.assertEquals(12345, comp.getInt("intVal"));
            Assertions.assertEquals(9.876, comp.getDouble("doubleVal"), 0.0001);
        } catch (Throwable t) {
            System.err.println("TEST FAIL JSON: " + t.getMessage());
            t.printStackTrace();
            throw t;
        }
    }
}
