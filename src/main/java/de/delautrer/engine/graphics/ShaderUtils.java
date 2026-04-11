package de.delautrer.engine.graphics;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderUtils {

    public static ByteBuffer readSPIRV(String filename) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filename));
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader file: " + filename, e);
        }
    }
}