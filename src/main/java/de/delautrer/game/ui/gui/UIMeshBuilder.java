package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.items.ItemStack;
import org.lwjgl.stb.STBTTBakedChar;

import java.util.ArrayList;
import java.util.List;

public class UIMeshBuilder {
    public final List<Float> guiVerts = new ArrayList<>();
    public final List<Integer> guiInds = new ArrayList<>();
    public final List<Float> textVerts = new ArrayList<>();
    public final List<Integer> textInds = new ArrayList<>();

    private static final float DEFAULT_GRID = 9.0f;

    public void clear() {
        guiVerts.clear(); guiInds.clear();
        textVerts.clear(); textInds.clear();
    }

    public void drawItem(ItemStack stack, float x, float y, float z, float size) {
        if (stack == null) return;
        int idx = stack.type.iconIndex;
        int gridX = idx % 9;
        int gridY = 3 + (idx / 9);
        addAtlasQuad(x, y, z, size, size, gridX, gridY, 1, 1, DEFAULT_GRID, true);
    }

    public void addAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH, float gridSize, boolean flipV) {
        int offset = guiVerts.size() / 8;
        float epsilon = 0.0005f;

        float u0 = (float) gridX / gridSize + epsilon;
        float v0 = (float) gridY / gridSize + epsilon;
        float u1 = (float) (gridX + gridW) / gridSize - epsilon;
        float v1 = (float) (gridY + gridH) / gridSize - epsilon;

        float finalV0 = flipV ? v1 : v0;
        float finalV1 = flipV ? v0 : v1;

        guiVerts.addAll(List.of(
                x, y, z, 1.0f, 1.0f, 1.0f, u0, finalV0,
                x + w, y, z, 1.0f, 1.0f, 1.0f, u1, finalV0,
                x + w, y + h, z, 1.0f, 1.0f, 1.0f, u1, finalV1,
                x, y + h, z, 1.0f, 1.0f, 1.0f, u0, finalV1
        ));
        guiInds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    public void addAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH, boolean flipV) {
        addAtlasQuad(x, y, z, w, h, gridX, gridY, gridW, gridH, DEFAULT_GRID, flipV);
    }

    public void drawText(String text, float startX, float startY, float z, VulkanFont font) {
        if (font == null || font.getCharData() == null) return;
        float currentX = startX;
        float currentY = startY;
        STBTTBakedChar.Buffer charData = font.getCharData();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c < 128) {
                STBTTBakedChar bakedChar = charData.get(c - 32);

                float glyphWidth = bakedChar.x1() - bakedChar.x0();
                float glyphHeight = bakedChar.y1() - bakedChar.y0();

                float x0 = currentX + bakedChar.xoff();
                float x1 = x0 + glyphWidth;
                float yTop = currentY - bakedChar.yoff();
                float yBottom = yTop - glyphHeight;

                float atlasSize = (float) font.BITMAP_SIZE;
                float u0 = bakedChar.x0() / atlasSize;
                float v0 = bakedChar.y0() / atlasSize;
                float u1 = bakedChar.x1() / atlasSize;
                float v1 = bakedChar.y1() / atlasSize;

                int offset = textVerts.size() / 8;
                textVerts.addAll(List.of(
                        x0, yBottom, z, 1.0f, 1.0f, 1.0f, u0, v1,
                        x1, yBottom, z, 1.0f, 1.0f, 1.0f, u1, v1,
                        x1, yTop,    z, 1.0f, 1.0f, 1.0f, u1, v0,
                        x0, yTop,    z, 1.0f, 1.0f, 1.0f, u0, v0
                ));
                textInds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));

                currentX += bakedChar.xadvance();
            }
        }
    }

    public void addRect(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        int offset = guiVerts.size() / 8;

        float u = 0.0f;
        float v = 0.0f;

        guiVerts.addAll(List.of(
                x,     y,     z, r, g, b, u, v,
                x + w, y,     z, r, g, b, u, v,
                x + w, y + h, z, r, g, b, u, v,
                x,     y + h, z, r, g, b, u, v
        ));

        guiInds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    public void addCroppedAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, float cropRatio, float gridSize) {
        int offset = guiVerts.size() / 8;
        float epsilon = 0.0005f;

        float u0 = (float) gridX / gridSize + epsilon;
        float v0 = (float) gridY / gridSize + epsilon;

        float uCellWidth = (1.0f / gridSize) - (2 * epsilon);
        float u1 = u0 + (uCellWidth * cropRatio);

        float v1 = (float) (gridY + 1) / gridSize - epsilon;

        guiVerts.addAll(List.of(
                x,     y,     z, 1.0f, 1.0f, 1.0f, u0, v0,
                x + w, y,     z, 1.0f, 1.0f, 1.0f, u1, v0,
                x + w, y + h, z, 1.0f, 1.0f, 1.0f, u1, v1,
                x,     y + h, z, 1.0f, 1.0f, 1.0f, u0, v1
        ));
        guiInds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    public float getTextWidth(String text, VulkanFont font) {
        if (font == null || font.getCharData() == null || text == null || text.isEmpty()) {
            return 0.0f;
        }

        float textWidth = 0.0f;
        STBTTBakedChar.Buffer charData = font.getCharData();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c < 128) {
                textWidth += charData.get(c - 32).xadvance();
            }
        }
        return textWidth;
    }

    // Die neue magische 9-Slice Methode!
    public void add9Slice(float x, float y, float z, float w, float h, int gridX, int gridY, float cornerRenderSize) {
        float epsilon = 0.0005f;
        float uvStep = 1.0f / UIElement.MENU_GRID_SIZE;

        // Die exakten UV-Grenzen deiner Textur-Zelle
        float u0 = (float) gridX * uvStep + epsilon;
        float v0 = (float) gridY * uvStep + epsilon;
        float u3 = u0 + uvStep - 2 * epsilon;
        float v3 = v0 + uvStep - 2 * epsilon;

        // Wir nehmen an, dass die Ecken in deiner Textur 4 Pixel dick sind (bei 16x16 Grid = 25% = 0.25f)
        float cornerUV = (uvStep - 2 * epsilon) * 0.25f;

        // Die 4 Schnittkanten auf der Textur (UVs)
        float[] u = {u0, u0 + cornerUV, u3 - cornerUV, u3};
        float[] v = {v0, v0 + cornerUV, v3 - cornerUV, v3};

        // Die 4 Schnittkanten auf dem Bildschirm (Pixel)
        float[] posX = {x, x + cornerRenderSize, x + w - cornerRenderSize, x + w};
        float[] posY = {y, y + cornerRenderSize, y + h - cornerRenderSize, y + h};

        // Zeichne alle 9 Kacheln
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float px0 = posX[col];
                float px1 = posX[col+1];
                float py0 = posY[row];
                float py1 = posY[row+1];

                // Verhindert Fehler, wenn das UI-Element kleiner als die Ecken ist
                if (px1 < px0) px1 = px0;
                if (py1 < py0) py1 = py0;

                addRawQuad(px0, py0, px1, py1, z, u[col], v[row], u[col+1], v[row+1]);
            }
        }
    }

    private void addRawQuad(float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1) {
        int offset = guiVerts.size() / 8;
        guiVerts.addAll(List.of(
                x0, y0, z, 1.0f, 1.0f, 1.0f, u0, v0,
                x1, y0, z, 1.0f, 1.0f, 1.0f, u1, v0,
                x1, y1, z, 1.0f, 1.0f, 1.0f, u1, v1,
                x0, y1, z, 1.0f, 1.0f, 1.0f, u0, v1
        ));
        guiInds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }
}