package de.delautrer.engine.graphics.utils;

import de.delautrer.engine.utils.AssetManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class TextureStitcher {

    public static final int TEXTURE_SIZE = 16;

    public static class AtlasRegion {
        public final float u0, v0, u1, v1;
        public final float layer; // NEU: Der Layer im Texture Array!

        public AtlasRegion(float u0, float v0, float u1, float v1, float layer) {
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1; this.layer = layer;
        }
    }

    public static class AtlasResult {
        public final ByteBuffer atlasPixels;
        public final int atlasWidth, atlasHeight;
        public final Map<String, AtlasRegion> regions;

        public AtlasResult(ByteBuffer atlasPixels, int atlasWidth, int atlasHeight, Map<String, AtlasRegion> regions) {
            this.atlasPixels = atlasPixels;
            this.atlasWidth = atlasWidth;
            this.atlasHeight = atlasHeight;
            this.regions = regions;
        }

        public void cleanup() {
            if (atlasPixels != null) {
                MemoryUtil.memFree(atlasPixels);
            }
        }
    }

    public static AtlasResult buildAtlas(java.util.Set<String> textureNames, String debugOutputPath) {
        if (textureNames == null || textureNames.isEmpty()) {
            throw new RuntimeException("Lmao I need some textures bro.");
        }

        int numImages = textureNames.size();
        int gridSize = (int) Math.ceil(Math.sqrt(numImages));
        int atlasWidth = gridSize * TEXTURE_SIZE;
        int atlasHeight = gridSize * TEXTURE_SIZE;

        ByteBuffer atlasPixels = MemoryUtil.memAlloc(atlasWidth * atlasHeight * 4);
        Map<String, AtlasRegion> regions = new HashMap<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            int i = 0;
            for (String name : textureNames) {
                String assetPath = "assets/textures/block/" + name + ".png";

                try {
                    ByteBuffer fileBuffer = AssetManager.loadResource(assetPath);
                    ByteBuffer imagePixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);

                    if (imagePixels != null) {
                        int gridX = i % gridSize;
                        int gridY = i / gridSize;

                        int pixelStartX = gridX * TEXTURE_SIZE;
                        int pixelStartY = gridY * TEXTURE_SIZE;

                        for (int row = 0; row < TEXTURE_SIZE; row++) {
                            for (int col = 0; col < TEXTURE_SIZE; col++) {
                                int srcIndex = (row * TEXTURE_SIZE + col) * 4;
                                int dstIndex = ((pixelStartY + row) * atlasWidth + (pixelStartX + col)) * 4;

                                atlasPixels.put(dstIndex, imagePixels.get(srcIndex));
                                atlasPixels.put(dstIndex + 1, imagePixels.get(srcIndex + 1));
                                atlasPixels.put(dstIndex + 2, imagePixels.get(srcIndex + 2));
                                atlasPixels.put(dstIndex + 3, imagePixels.get(srcIndex + 3));
                            }
                        }

                        STBImage.stbi_image_free(imagePixels);
                        regions.put(name, new AtlasRegion(0.0f, 0.0f, 1.0f, 1.0f, (float) i));
                    } else {
                        System.err.println("Error loading texture: " + assetPath);
                    }
                } catch (Exception e) {
                    System.err.println("Texture could NOT be found in classpath: " + assetPath);
                }

                i++;
            }
        }

        if (debugOutputPath != null) {
            try {
                STBImageWrite.stbi_write_png(debugOutputPath, atlasWidth, atlasHeight, 4, atlasPixels, atlasWidth * 4);
            } catch (Exception ignored) {

            }
        }

        return new AtlasResult(atlasPixels, atlasWidth, atlasHeight, regions);
    }
}