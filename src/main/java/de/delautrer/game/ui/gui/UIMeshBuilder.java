package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.items.ItemStack;
import org.lwjgl.stb.STBTTBakedChar;

import java.util.ArrayList;
import java.util.List;

public class UIMeshBuilder {
    public final List<Float> uiVerts = new ArrayList<>();
    public final List<Integer> uiInds = new ArrayList<>();
    public final List<Float> itemVerts = new ArrayList<>();
    public final List<Integer> itemInds = new ArrayList<>();
    public final List<Float> textVerts = new ArrayList<>();
    public final List<Integer> textInds = new ArrayList<>();
    public final List<Float> overlayVerts = new ArrayList<>();
    public final List<Integer> overlayInds = new ArrayList<>();
    public final List<Float> topUiVerts = new ArrayList<>();
    public final List<Integer> topUiInds = new ArrayList<>();

    public static final float UI_GRID = 16.0f;
    public static final float ITEM_GRID = 9.0f;

    private boolean clippingEnabled = false;
    private float clipX, clipY, clipW, clipH;

    public void clear() {
        uiVerts.clear(); uiInds.clear();
        itemVerts.clear(); itemInds.clear();
        textVerts.clear(); textInds.clear();
        overlayVerts.clear(); overlayInds.clear();
        topUiVerts.clear(); topUiInds.clear();
        clippingEnabled = false;
    }

    public void setClipRect(float x, float y, float w, float h) {
        this.clippingEnabled = true;
        this.clipX = x; this.clipY = y; this.clipW = w; this.clipH = h;
    }

    public void clearClipRect() {
        this.clippingEnabled = false;
    }

    private void addClippedQuad(List<Float> verts, List<Integer> inds, float x0, float y0, float x1, float y1, float z, float r, float g, float b, float u0, float v0, float u1, float v1) {
        float minX = Math.min(x0, x1); float maxX = Math.max(x0, x1);
        float minY = Math.min(y0, y1); float maxY = Math.max(y0, y1);
        float minU = (x0 < x1) ? u0 : u1; float maxU = (x0 < x1) ? u1 : u0;
        float minV = (y0 < y1) ? v0 : v1; float maxV = (y0 < y1) ? v1 : v0;

        if (clippingEnabled) {
            if (maxX <= clipX || minX >= clipX + clipW || maxY <= clipY || minY >= clipY + clipH) return;
            float origW = maxX - minX; float origH = maxY - minY;
            if (minX < clipX) { float cut = clipX - minX; minU += (cut / origW) * (maxU - minU); minX = clipX; }
            if (maxX > clipX + clipW) { float cut = maxX - (clipX + clipW); maxU -= (cut / origW) * (maxU - minU); maxX = clipX + clipW; }
            if (minY < clipY) { float cut = clipY - minY; minV += (cut / origH) * (maxV - minV); minY = clipY; }
            if (maxY > clipY + clipH) { float cut = maxY - (clipY + clipH); maxV -= (cut / origH) * (maxV - minV); maxY = clipY + clipH; }
            if (minX >= maxX || minY >= maxY) return;
        }

        int offset = verts.size() / 8;
        verts.addAll(List.of(
                minX, minY, z, r, g, b, minU, minV,  maxX, minY, z, r, g, b, maxU, minV,
                maxX, maxY, z, r, g, b, maxU, maxV,  minX, maxY, z, r, g, b, minU, maxV
        ));
        inds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    public void drawItem(ItemStack stack, float x, float y, float z, float size) {
        if (stack == null) return;
        TextureStitcher.AtlasRegion reg = stack.type.getIconRegion();
        if(reg == null) return;
        addClippedQuad(itemVerts, itemInds, x, y, x + size, y + size, z, 1f, 1f, 1f, reg.u0, reg.v1, reg.u1, reg.v0);
    }

    public void addAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH, boolean flipV) {
        float epsilon = 0.0005f;
        float u0 = (float) gridX / UI_GRID + epsilon;
        float v0 = (float) gridY / UI_GRID + epsilon;
        float u1 = (float) (gridX + gridW) / UI_GRID - epsilon;
        float v1 = (float) (gridY + gridH) / UI_GRID - epsilon;

        float finalV0 = flipV ? v1 : v0;
        float finalV1 = flipV ? v0 : v1;

        addClippedQuad(uiVerts, uiInds, x, y, x + w, y + h, z, 1f, 1f, 1f, u0, finalV0, u1, finalV1);
    }

    public void addRect(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        addClippedQuad(uiVerts, uiInds, x, y, x + w, y + h, z, r, g, b, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void addCroppedAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, float cropRatio, float gridSize) {
        float epsilon = 0.0005f;
        float u0 = (float) gridX / gridSize + epsilon;
        float v0 = (float) gridY / gridSize + epsilon;
        float uCellWidth = (1.0f / gridSize) - (2 * epsilon);
        float u1 = u0 + (uCellWidth * cropRatio);
        float v1 = (float) (gridY + 1) / gridSize - epsilon;

        addClippedQuad(uiVerts, uiInds, x, y, x + w, y + h, z, 1f, 1f, 1f, u0, v0, u1, v1);
    }

    public void addOverlayQuad(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        addClippedQuad(overlayVerts, overlayInds, x, y, x + w, y + h, 0.0f, 0.2f, 0.2f, 0.2f, u0, v0, u1, v1);
    }

    public void add9Slice(float x, float y, float z, float w, float h, int gridX, int gridY, float cornerRenderSize) {
        float epsilon = 0.0005f;
        float uvStep = 1.0f / UI_GRID; // 16er Grid!

        float u0 = (float) gridX * uvStep + epsilon;
        float v0 = (float) gridY * uvStep + epsilon;
        float u3 = u0 + uvStep - 2 * epsilon;
        float v3 = v0 + uvStep - 2 * epsilon;

        float cornerUV = (uvStep - 2 * epsilon) * 0.25f; // Viertel eines Blocks

        float[] u = {u0, u0 + cornerUV, u3 - cornerUV, u3};
        float[] v = {v0, v0 + cornerUV, v3 - cornerUV, v3};
        float[] posX = {x, x + cornerRenderSize, x + w - cornerRenderSize, x + w};
        float[] posY = {y, y + cornerRenderSize, y + h - cornerRenderSize, y + h};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float px0 = posX[col]; float px1 = posX[col+1];
                float py0 = posY[row]; float py1 = posY[row+1];

                if (px1 < px0) px1 = px0;
                if (py1 < py0) py1 = py0;

                addClippedQuad(uiVerts, uiInds, px0, py0, px1, py1, z, 1f, 1f, 1f, u[col], v[row], u[col+1], v[row+1]);
            }
        }
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
                float u0 = bakedChar.x0() / atlasSize; float v0 = bakedChar.y0() / atlasSize;
                float u1 = bakedChar.x1() / atlasSize; float v1 = bakedChar.y1() / atlasSize;

                addClippedQuad(textVerts, textInds, x0, yBottom, x1, yTop, z, 1f, 1f, 1f, u0, v1, u1, v0);
                currentX += bakedChar.xadvance();
            }
        }
    }

    public float getTextWidth(String text, VulkanFont font) {
        if (font == null || font.getCharData() == null || text == null || text.isEmpty()) return 0.0f;
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

    public void addTooltipBackground(float x, float y, float z, float w, float h, int gridX, int gridY, float cornerRenderSize) {
        float epsilon = 0.0005f;
        float uvStep = 1.0f / UI_GRID;

        float u0 = (float) gridX * uvStep + epsilon;
        float v0 = (float) gridY * uvStep + epsilon;
        float u3 = u0 + uvStep - 2 * epsilon;
        float v3 = v0 + uvStep - 2 * epsilon;

        float cornerUV = (uvStep - 2 * epsilon) * 0.25f;

        float[] u = {u0, u0 + cornerUV, u3 - cornerUV, u3};
        float[] v = {v0, v0 + cornerUV, v3 - cornerUV, v3};
        float[] posX = {x, x + cornerRenderSize, x + w - cornerRenderSize, x + w};
        float[] posY = {y, y + cornerRenderSize, y + h - cornerRenderSize, y + h};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float px0 = posX[col]; float px1 = posX[col+1];
                float py0 = posY[row]; float py1 = posY[row+1];

                if (px1 < px0) px1 = px0;
                if (py1 < py0) py1 = py0;

                addClippedQuad(topUiVerts, topUiInds, px0, py0, px1, py1, z, 1f, 1f, 1f, u[col], v[row], u[col+1], v[row+1]);
            }
        }
    }
}