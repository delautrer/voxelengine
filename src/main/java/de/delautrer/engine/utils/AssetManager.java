package de.delautrer.engine.utils;

import org.lwjgl.BufferUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AssetManager {

    public static ByteBuffer loadResource(String path) {
        if (path.startsWith("src/main/resources/")) {
            path = path.replace("src/main/resources/", "");
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try (InputStream is = AssetManager.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource was not found in Classpath: " + path);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();

            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();

            return byteBuffer;
        } catch (Exception e) {
            throw new RuntimeException("Error whilst loading resource: " + path, e);
        }
    }
}