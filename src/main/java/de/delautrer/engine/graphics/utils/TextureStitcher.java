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

        if (is2DAtlas) {
            return buildItemAtlas(textureNames, debugOutputPath, textureFolder);
        }

        return buildBlockAtlas(textureNames, debugOutputPath, textureFolder);
    }

    private static AtlasResult buildBlockAtlas(Set<String> textureNames, String debugOutputPath, String textureFolder) {
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

                ByteBuffer imagePixels = null;
                try {
                    ByteBuffer fileBuffer = AssetManager.loadResource(assetPath);
                    imagePixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);
                } catch (Exception e) {
                }

                if (imagePixels == null) {
                    System.err.println("Fehler beim Entpacken von: " + assetPath);
                    throw new IllegalStateException("Failed to load block texture: " + assetPath);
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

                // Texture Array (Blöcke): Volle Textur, Layer ist der Index
                float u0 = 0.0f;
                float v0 = 0.0f;
                float u1 = 1.0f;
                float v1 = 1.0f;
                float layer = (float) i;

                regions.put(name, new AtlasRegion(u0, v0, u1, v1, layer));
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

    private static class ItemTextureData {
        final String name;
        final ByteBuffer pixels;
        final int width;
        final int height;
        int packedX;
        int packedY;

        ItemTextureData(String name, ByteBuffer pixels, int width, int height) {
            this.name = name;
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }
    }

    private static AtlasResult buildItemAtlas(Set<String> textureNames, String debugOutputPath, String textureFolder) {
        java.util.List<ItemTextureData> items = new java.util.ArrayList<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            for (String name : textureNames) {
                String assetPath = textureFolder + "/" + name + ".png";
                ByteBuffer imagePixels = null;
                try {
                    ByteBuffer fileBuffer = AssetManager.loadResource(assetPath);
                    imagePixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);
                } catch (Exception e) {
                }

                if (imagePixels == null) {
                    System.err.println("Fehler beim Entpacken von: " + assetPath);
                    for (ItemTextureData item : items) {
                        STBImage.stbi_image_free(item.pixels);
                    }
                    throw new IllegalStateException("Failed to load item texture: " + assetPath);
                }

                int imgW = w.get(0);
                int imgH = h.get(0);

                if (!((imgW == 16 && imgH == 16) || (imgW == 64 && imgH == 64))) {
                    STBImage.stbi_image_free(imagePixels);
                    for (ItemTextureData item : items) {
                        STBImage.stbi_image_free(item.pixels);
                    }
                    throw new IllegalStateException("item texture '" + name + "' must be 16x16 or 64x64, got " + imgW + "x" + imgH);
                }

                items.add(new ItemTextureData(name, imagePixels, imgW, imgH));
            }
        }

        // Sort items by height descending, then width descending, then name
        items.sort((a, b) -> {
            if (b.height != a.height) {
                return Integer.compare(b.height, a.height);
            }
            if (b.width != a.width) {
                return Integer.compare(b.width, a.width);
            }
            return a.name.compareTo(b.name);
        });

        int maxDim = 16;
        for (ItemTextureData item : items) {
            maxDim = Math.max(maxDim, Math.max(item.width, item.height));
        }

        int candidateSize = 16;
        while (candidateSize < maxDim) {
            candidateSize *= 2;
        }

        int chosenSize = -1;
        while (candidateSize <= 4096) {
            if (tryPackItems(items, candidateSize)) {
                chosenSize = candidateSize;
                break;
            }
            candidateSize *= 2;
        }

        if (chosenSize == -1) {
            for (ItemTextureData item : items) {
                STBImage.stbi_image_free(item.pixels);
            }
            throw new IllegalStateException("Item atlas size exceeded 4096x4096 limit");
        }

        int atlasWidth = chosenSize;
        int atlasHeight = chosenSize;
        ByteBuffer atlasPixels = MemoryUtil.memCalloc(atlasWidth * atlasHeight * 4);
        Map<String, AtlasRegion> regions = new HashMap<>();

        for (ItemTextureData item : items) {
            for (int row = 0; row < item.height; row++) {
                for (int col = 0; col < item.width; col++) {
                    int srcIndex = (row * item.width + col) * 4;
                    int dstIndex = ((item.packedY + row) * atlasWidth + (item.packedX + col)) * 4;

                    atlasPixels.put(dstIndex, item.pixels.get(srcIndex));
                    atlasPixels.put(dstIndex + 1, item.pixels.get(srcIndex + 1));
                    atlasPixels.put(dstIndex + 2, item.pixels.get(srcIndex + 2));
                    atlasPixels.put(dstIndex + 3, item.pixels.get(srcIndex + 3));
                }
            }
            STBImage.stbi_image_free(item.pixels);

            float u0 = (float) item.packedX / atlasWidth;
            float v0 = (float) item.packedY / atlasHeight;
            float u1 = (float) (item.packedX + item.width) / atlasWidth;
            float v1 = (float) (item.packedY + item.height) / atlasHeight;
            float layer = 0.0f;

            regions.put(item.name, new AtlasRegion(u0, v0, u1, v1, layer));
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

    private static boolean tryPackItems(java.util.List<ItemTextureData> items, int atlasSize) {
        int currentX = 0;
        int currentY = 0;
        int shelfHeight = 0;

        for (ItemTextureData item : items) {
            int cellW = item.width + 1;
            int cellH = item.height + 1;

            if (currentX + cellW > atlasSize) {
                currentY += shelfHeight;
                currentX = 0;
                shelfHeight = 0;
            }

            if (currentY + cellH > atlasSize) {
                return false;
            }

            item.packedX = currentX;
            item.packedY = currentY;

            currentX += cellW;
            shelfHeight = Math.max(shelfHeight, cellH);
        }

        return true;
    }
}
