package de.delautrer.engine.utils;

import org.lwjgl.BufferUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AssetManager {

    public static ByteBuffer loadResource(String path) {
        java.io.InputStream is = null;
        boolean isFile = false;

        if (new java.io.File(path).isAbsolute() && new java.io.File(path).exists()) {
            try {
                is = new java.io.FileInputStream(path);
                isFile = true;
            } catch (java.io.FileNotFoundException e) {
                // Ignore, will throw later
            }
        }

        if (!isFile) {
            if (path.startsWith("src/main/resources/")) {
                path = path.replace("src/main/resources/", "");
            }

            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            is = AssetManager.class.getResourceAsStream(path);
        }

        try (InputStream inputStream = is) {
            if (inputStream == null) {
                throw new RuntimeException("Resource was not found in Classpath or FileSystem: " + path);
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