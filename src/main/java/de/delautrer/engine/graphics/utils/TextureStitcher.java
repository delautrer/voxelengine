package de.delautrer.engine.graphics.utils;

import de.delautrer.Constants;
import de.delautrer.engine.utils.AssetManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextureStitcher {

    public static final int TEXTURE_SIZE = 16;

    public static class AtlasRegion {
        public final float u0, v0, u1, v1;
        public final float layer; // NEU: Der Layer im Texture Array!

        public AtlasRegion(float u0, float v0, float u1, float v1, float layer) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.layer = layer;
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

    public static AtlasResult buildAtlas(Set<String> textureNames, String debugOutputPath, String textureFolder,
            boolean is2DAtlas) {
        if (textureNames == null || textureNames.isEmpty()) {
            throw new RuntimeException("lmao I need textures bro...");
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
                String assetPath = textureFolder + "/" + name + ".png";

                try {
                    ByteBuffer fileBuffer = AssetManager.loadResource(assetPath);
                    ByteBuffer imagePixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);

                    if (imagePixels != null) {
                        // Dynamically overlay ore clumps on top of the stone texture
                        if (name.endsWith("_ore")) {
                            String bgPath = textureFolder + "/stone.png";
                            try {
                                ByteBuffer bgBuffer = AssetManager.loadResource(bgPath);
                                int origW = w.get(0);
                                int origH = h.get(0);
                                int origComp = comp.get(0);

                                ByteBuffer bgPixels = STBImage.stbi_load_from_memory(bgBuffer, w, h, comp, 4);
                                if (bgPixels != null) {
                                    for (int pixelIndex = 0; pixelIndex < TEXTURE_SIZE * TEXTURE_SIZE; pixelIndex++) {
                                        int offset = pixelIndex * 4;
                                        int alpha = imagePixels.get(offset + 3) & 0xFF;
                                        if (alpha == 0) {
                                            imagePixels.put(offset, bgPixels.get(offset));
                                            imagePixels.put(offset + 1, bgPixels.get(offset + 1));
                                            imagePixels.put(offset + 2, bgPixels.get(offset + 2));
                                            imagePixels.put(offset + 3, bgPixels.get(offset + 3));
                                        }
                                    }
                                    STBImage.stbi_image_free(bgPixels);
                                }
                                w.put(0, origW);
                                h.put(0, origH);
                                comp.put(0, origComp);
                            } catch (Exception e) {
                                System.err.println("Could not load background stone texture for ore: " + bgPath);
                            }
                        }
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

                        // --- NEU: UV-Koordinaten je nach Atlas-Typ berechnen ---
                        float u0, v0, u1, v1, layer;
                        if (is2DAtlas) {
                            // 2D Atlas: Echte UV-Koordinaten im Raster, Layer ist immer 0
                            float epsilon = 0.0005f;
                            u0 = (float) gridX / gridSize + epsilon;
                            v0 = (float) gridY / gridSize + epsilon;
                            u1 = (float) (gridX + 1) / gridSize - epsilon;
                            v1 = (float) (gridY + 1) / gridSize - epsilon;
                            layer = 0.0f;
                        } else {
                            // Texture Array (Blöcke): Volle Textur, Layer ist der Index
                            u0 = 0.0f;
                            v0 = 0.0f;
                            u1 = 1.0f;
                            v1 = 1.0f;
                            layer = (float) i;
                        }

                        regions.put(name, new AtlasRegion(u0, v0, u1, v1, layer));
                    } else {
                        System.err.println("Fehler beim Entpacken von: " + assetPath);
                    }
                } catch (Exception e) {
                    System.err.println("Textur nicht gefunden: " + assetPath);
                }
                i++;
            }
        }

        if (Constants.IS_DEV) {
            if (debugOutputPath != null) {
                try {
                    STBImageWrite.stbi_write_png(debugOutputPath, atlasWidth, atlasHeight, 4, atlasPixels,
                            atlasWidth * 4);
                } catch (Exception ignored) {
                }
            }
        }

        return new AtlasResult(atlasPixels, atlasWidth, atlasHeight, regions);
    }
}
