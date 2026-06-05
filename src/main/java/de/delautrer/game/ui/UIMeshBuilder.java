package de.delautrer.game.ui;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.ToolItem;
import de.delautrer.game.ui.elements.UITexture;
import org.lwjgl.stb.STBTTBakedChar;
import java.util.*;

public class UIMeshBuilder {
    public static final float UI_GRID = 16.0f;
    public static final float ITEM_GRID = 9.0f;

    private boolean clippingEnabled = false;
    private float clipX, clipY, clipW, clipH;

    public static class Batch {
        public final List<Float> verts = new ArrayList<>();
        public final List<Integer> inds = new ArrayList<>();
    }

    // Die Magie: Sortiert automatisch nach Z-Index!
    private final TreeMap<Float, Map<UITexture, Batch>> layers = new TreeMap<>();

    public void clear() {
        layers.clear();
        clippingEnabled = false;
    }

    public TreeMap<Float, Map<UITexture, Batch>> getLayers() {
        return layers;
    }

    public void setClipRect(float x, float y, float w, float h) {
        this.clippingEnabled = true;
        this.clipX = x;
        this.clipY = y;
        this.clipW = w;
        this.clipH = h;
    }

    public void clearClipRect() {
        this.clippingEnabled = false;
    }

    private void addClippedQuad(UITexture texture, float x0, float y0, float x1, float y1, float z, float r, float g,
            float b, float u0, float v0, float u1, float v1) {
        float minX = Math.min(x0, x1);
        float maxX = Math.max(x0, x1);
        float minY = Math.min(y0, y1);
        float maxY = Math.max(y0, y1);
        float minU = (x0 < x1) ? u0 : u1;
        float maxU = (x0 < x1) ? u1 : u0;
        float minV = (y0 < y1) ? v0 : v1;
        float maxV = (y0 < y1) ? v1 : v0;

        if (clippingEnabled) {
            if (maxX <= clipX || minX >= clipX + clipW || maxY <= clipY || minY >= clipY + clipH)
                return;
            float origW = maxX - minX;
            float origH = maxY - minY;
            if (minX < clipX) {
                float cut = clipX - minX;
                minU += (cut / origW) * (maxU - minU);
                minX = clipX;
            }
            if (maxX > clipX + clipW) {
                float cut = maxX - (clipX + clipW);
                maxU -= (cut / origW) * (maxU - minU);
                maxX = clipX + clipW;
            }
            if (minY < clipY) {
                float cut = clipY - minY;
                minV += (cut / origH) * (maxV - minV);
                minY = clipY;
            }
            if (maxY > clipY + clipH) {
                float cut = maxY - (clipY + clipH);
                maxV -= (cut / origH) * (maxV - minV);
                maxY = clipY + clipH;
            }
            if (minX >= maxX || minY >= maxY)
                return;
        }

        // Finde den Z-Layer oder erstelle ihn
        Map<UITexture, Batch> layer = layers.computeIfAbsent(z, k -> new EnumMap<>(UITexture.class));
        Batch batch = layer.computeIfAbsent(texture, k -> new Batch());

        int offset = batch.verts.size() / 8;
        batch.verts.addAll(List.of(
                minX, minY, z, r, g, b, minU, minV, maxX, minY, z, r, g, b, maxU, minV,
                maxX, maxY, z, r, g, b, maxU, maxV, minX, maxY, z, r, g, b, minU, maxV));
        batch.inds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    public void drawItem(ItemStack stack, float x, float y, float z, float size) {
        if (stack == null)
            return;
        TextureStitcher.AtlasRegion reg = stack.type.getIconRegion();
        if (reg == null)
            return;
        addClippedQuad(UITexture.ITEM, x, y, x + size, y + size, z, 1f, 1f, 1f, reg.u0, reg.v1, reg.u1, reg.v0);

        if (stack.type instanceof ToolItem) {
            ToolItem tool = (ToolItem) stack.type;
            int maxDur = tool.getMaxDurability();
            int curDur = stack.durability;
            if (curDur < maxDur) {
                float pct = Math.max(0.0f, Math.min(1.0f, (float) curDur / maxDur));
                float barW = size * 0.8f;
                float barX = x + (size - barW) / 2.0f;
                float barY = y + size * 0.1f;
                float barH = size * 0.08f;

                // Dark background
                addRect(barX, barY, z + 0.01f, barW, barH, 0.0f, 0.0f, 0.0f, 1.0f);

                // Color: green for high durability, yellow for mid, red for low
                float r = 1.0f - pct;
                float g = pct;
                float b = 0.0f;
                addRect(barX, barY, z + 0.02f, barW * pct, barH, r, g, b, 1.0f);
            }
        }
    }

    public void addAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH,
            boolean flipV) {
        float epsilon = 0.0005f;
        float u0 = (float) gridX / UI_GRID + epsilon;
        float v0 = (float) gridY / UI_GRID + epsilon;
        float u1 = (float) (gridX + gridW) / UI_GRID - epsilon;
        float v1 = (float) (gridY + gridH) / UI_GRID - epsilon;

        addClippedQuad(UITexture.UI, x, y, x + w, y + h, z, 1f, 1f, 1f, u0, flipV ? v1 : v0, u1, flipV ? v0 : v1);
    }

    public void addRect(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        addClippedQuad(UITexture.UI, x, y, x + w, y + h, z, r, g, b, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void addCroppedAtlasQuad(float x, float y, float z, float w, float h, int gridX, int gridY, float cropRatio,
            float gridSize) {
        float epsilon = 0.0005f;
        float uCellWidth = (1.0f / gridSize) - (2 * epsilon);
        addClippedQuad(UITexture.UI, x, y, x + w, y + h, z, 1f, 1f, 1f,
                (float) gridX / gridSize + epsilon, (float) gridY / gridSize + epsilon,
                ((float) gridX / gridSize + epsilon) + (uCellWidth * cropRatio),
                (float) (gridY + 1) / gridSize - epsilon);
    }

    public void addOverlayQuad(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        addClippedQuad(UITexture.BLOCK, x, y, x + w, y + h, 0.0f, 0.2f, 0.2f, 0.2f, u0, v0, u1, v1);
    }

    public void add9Slice(float x, float y, float z, float w, float h, int gridX, int gridY, float cornerRenderSize) {
        float epsilon = 0.0005f;
        float uvStep = 1.0f / UI_GRID;
        float u0 = (float) gridX * uvStep + epsilon;
        float v0 = (float) gridY * uvStep + epsilon;
        float u3 = u0 + uvStep - 2 * epsilon;
        float v3 = v0 + uvStep - 2 * epsilon;
        float cornerUV = (uvStep - 2 * epsilon) * 0.25f;

        float[] u = { u0, u0 + cornerUV, u3 - cornerUV, u3 };
        float[] v = { v0, v0 + cornerUV, v3 - cornerUV, v3 };
        float[] posX = { x, x + cornerRenderSize, x + w - cornerRenderSize, x + w };
        float[] posY = { y, y + cornerRenderSize, y + h - cornerRenderSize, y + h };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float px0 = posX[col];
                float px1 = Math.max(posX[col + 1], px0);
                float py0 = posY[row];
                float py1 = Math.max(posY[row + 1], py0);
                addClippedQuad(UITexture.UI, px0, py0, px1, py1, z, 1f, 1f, 1f, u[col], v[row], u[col + 1], v[row + 1]);
            }
        }
    }

    public void drawText(String text, float startX, float startY, float z, IFont font) {
        if (font == null || font.getCharData() == null)
            return;
        float currentX = startX;
        float currentY = startY;
        STBTTBakedChar.Buffer charData = font.getCharData();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c < 256) {
                STBTTBakedChar bakedChar = charData.get(c - 32);
                float x0 = currentX + bakedChar.xoff();
                float yTop = currentY - bakedChar.yoff();

                addClippedQuad(UITexture.FONT, x0, yTop - (bakedChar.y1() - bakedChar.y0()),
                        x0 + (bakedChar.x1() - bakedChar.x0()), yTop, z, 1f, 1f, 1f,
                        bakedChar.x0() / (float) font.getBitmapSize(), bakedChar.y1() / (float) font.getBitmapSize(),
                        bakedChar.x1() / (float) font.getBitmapSize(), bakedChar.y0() / (float) font.getBitmapSize());
                currentX += bakedChar.xadvance();
            }
        }
    }

    public float getTextWidth(String text, IFont font) {
        if (font == null || font.getCharData() == null || text == null || text.isEmpty())
            return 0.0f;
        float textWidth = 0.0f;
        STBTTBakedChar.Buffer charData = font.getCharData();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c < 256)
                textWidth += charData.get(c - 32).xadvance();
        }
        return textWidth;
    }

    public void addTooltipBackground(float x, float y, float z, float w, float h, int gridX, int gridY,
            float cornerRenderSize) {
        add9Slice(x, y, z, w, h, gridX, gridY, cornerRenderSize);
    }
}
