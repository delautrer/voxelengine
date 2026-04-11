package de.delautrer.engine.graphics;

import de.delautrer.engine.utils.AssetManager;
import java.nio.ByteBuffer;

public class ShaderUtils {
    public static ByteBuffer readSPIRV(String filename) {
        return AssetManager.loadResource(filename);
    }
}