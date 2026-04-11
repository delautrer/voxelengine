package de.delautrer.engine.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class VulkanFont {
    private STBTTBakedChar.Buffer charData;
    private ByteBuffer rgbaBitmap;

    public final int BITMAP_SIZE = 512;

    public VulkanFont(String fontPath, float fontHeight) {
        charData = STBTTBakedChar.malloc(96);

        try {
            byte[] fontBytes = Files.readAllBytes(Paths.get(fontPath));
            ByteBuffer ttfBuffer = BufferUtils.createByteBuffer(fontBytes.length);
            ttfBuffer.put(fontBytes).flip();

            ByteBuffer alphaBitmap = BufferUtils.createByteBuffer(BITMAP_SIZE * BITMAP_SIZE);
            STBTruetype.stbtt_BakeFontBitmap(ttfBuffer, fontHeight, alphaBitmap, BITMAP_SIZE, BITMAP_SIZE, 32, charData);

            rgbaBitmap = MemoryUtil.memAlloc(BITMAP_SIZE * BITMAP_SIZE * 4);
            for (int i = 0; i < BITMAP_SIZE * BITMAP_SIZE; i++) {
                byte alpha = alphaBitmap.get(i);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put(alpha);
            }
            rgbaBitmap.flip();

        } catch (IOException e) {
            System.err.println("Error loading font: " + fontPath);
            e.printStackTrace();
        }
    }

    public STBTTBakedChar.Buffer getCharData() { return charData; }
    public ByteBuffer getRgbaPixels() { return rgbaBitmap; }

    public void cleanup() {
        charData.free();
        if (rgbaBitmap != null) MemoryUtil.memFree(rgbaBitmap);
    }
}