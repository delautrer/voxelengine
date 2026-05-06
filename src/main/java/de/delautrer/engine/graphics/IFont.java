package de.delautrer.engine.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.stb.STBTTBakedChar;

public interface IFont {
    void cleanup();
    ByteBuffer getRgbaPixels();
    int getBitmapSize();
    STBTTBakedChar.Buffer getCharData();
}
