package de.delautrer.engine.graphics;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.utils.AssetManager;
import java.nio.ByteBuffer;

public class ShaderUtils {
    public static ByteBuffer readSPIRV(String filename) {
        return AssetManager.loadResource(filename);
    }
}
