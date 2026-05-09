package de.delautrer.engine.graphics.vulkan.texture;

import de.delautrer.engine.utils.AssetManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

public class VulkanFont implements de.delautrer.engine.graphics.IFont {
    private STBTTBakedChar.Buffer charData;
    private ByteBuffer rgbaBitmap;

    public final int BITMAP_SIZE = 512;

    public VulkanFont(String fontPath, float fontHeight) {
        charData = STBTTBakedChar.malloc(224);
        try {
            ByteBuffer ttfBuffer = AssetManager.loadResource(fontPath);

            ByteBuffer alphaBitmap = BufferUtils.createByteBuffer(BITMAP_SIZE * BITMAP_SIZE);
            STBTruetype.stbtt_BakeFontBitmap(ttfBuffer, fontHeight, alphaBitmap, BITMAP_SIZE, BITMAP_SIZE, 32,
                    charData); // Bake chars (up to 256)

            rgbaBitmap = MemoryUtil.memAlloc(BITMAP_SIZE * BITMAP_SIZE * 4);
            for (int i = 0; i < BITMAP_SIZE * BITMAP_SIZE; i++) {
                byte alpha = alphaBitmap.get(i);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put((byte) 255);
                rgbaBitmap.put(alpha);
            }
            rgbaBitmap.flip();

        } catch (Exception e) {
            System.err.println("Error loading font: " + fontPath);
            e.printStackTrace();
        }
    }

    public STBTTBakedChar.Buffer getCharData() {
        return charData;
    }

    public ByteBuffer getRgbaPixels() {
        return rgbaBitmap;
    }

    public void cleanup() {
        charData.free();
        if (rgbaBitmap != null)
            MemoryUtil.memFree(rgbaBitmap);
    }

    @Override
    public int getBitmapSize() {
        return BITMAP_SIZE;
    }
}
