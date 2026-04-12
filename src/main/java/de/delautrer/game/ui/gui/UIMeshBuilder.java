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

    private static final float GRID_SIZE = 9.0f;

    public void clear() {
        guiVerts.clear(); guiInds.clear();
        textVerts.clear(); textInds.clear();
    }

    public void drawItem(ItemStack stack, float x, float y, float z, float size) {
        if (stack == null) return;
        int idx = stack.type.iconIndex;
        int gridX = idx % 9;
        int gridY = 3 + (idx / 9);
        addAtlasQuad(x, y, z, size, size, gridX, gridY, 1, 1, true);
    }

    public void addAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH, boolean flipV) {
        int offset = guiVerts.size() / 8;
        float epsilon = 0.0005f;

        float u0 = (float) gridX / GRID_SIZE + epsilon;
        float v0 = (float) gridY / GRID_SIZE + epsilon;
        float u1 = (float) (gridX + gridW) / GRID_SIZE - epsilon;
        float v1 = (float) (gridY + gridH) / GRID_SIZE - epsilon;

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
}