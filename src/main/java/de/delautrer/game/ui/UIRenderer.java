package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.items.ItemStack;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.List;

public class UIRenderer {

    private VulkanMesh guiMesh;
    private VulkanMesh textMesh;
    private final VulkanContext context;

    private static final float GRID_SIZE = 9.0f;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
    }

    public void rebuildMesh(int width, int height, Inventory inventory, float mouseX, float mouseY, int hoveredSlot, DebugOverlay debugOverlay, VulkanFont font) {
        if (guiMesh != null) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            guiMesh.cleanup();
            guiMesh = null;
        }
        if (textMesh != null) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            textMesh.cleanup();
            textMesh = null;
        }

        List<Float> guiVerts = new ArrayList<>();
        List<Integer> guiInds = new ArrayList<>();

        List<Float> textVerts = new ArrayList<>();
        List<Integer> textInds = new ArrayList<>();

        float pixelScale = 2.0f;
        if (height >= 1080) pixelScale = 3.0f;
        if (height >= 1440) pixelScale = 4.0f;

        // --- 1. DEBUG OVERLAY (F3) ---
        if (debugOverlay != null && debugOverlay.isVisible() && font != null && font.getCharData() != null) {
            // FIX 1: Da Y=0 unten ist, müssen wir oben am Bildschirm starten!
            float textY = height - 25.0f;
            float textX = 10.0f;

            for (String line : debugOverlay.getLinesToRender()) {
                drawText(textVerts, textInds, line, textX, textY, 0.5f, font);
                // FIX 2: Wir müssen Y abziehen, damit die nächste Zeile DARUNTER landet.
                textY -= 24.0f;
            }
        }

        // --- 2. GUI (Hotbar, Inventar, Fadenkreuz) ---
        float hotbarWidth = 207.0f * pixelScale;
        float hotbarHeight = 23.0f * pixelScale;

        if (!inventory.isOpen()) {
            float crosshairSize = 23.0f * pixelScale;
            float cx = (float) Math.floor((width - crosshairSize) / 2.0f);
            float cy = (float) Math.floor((height - crosshairSize) / 2.0f);
            addAtlasQuad(guiVerts, guiInds, cx, cy, 0.0f, crosshairSize, crosshairSize, 0, 0, 1, 1, false);
        }

        float hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        float hotbarY = (float) Math.floor(10.0f * pixelScale);

        addAtlasQuad(guiVerts, guiInds, hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);

        for (int col = 0; col < 9; col++) {
            float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
            float selectorW = 23.0f * pixelScale;

            if (!inventory.isOpen() && inventory.getSelectedSlot() == col) {
                addAtlasQuad(guiVerts, guiInds, slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            } else if (inventory.isOpen() && hoveredSlot == col) {
                addAtlasQuad(guiVerts, guiInds, slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            }

            if (inventory.getStack(col) != null) {
                drawItem(guiVerts, guiInds, inventory.getStack(col), slotX, hotbarY, 0.0f, selectorW);
            }
        }

        if (inventory.isOpen()) {
            float invY = (float) Math.floor((height - (3 * hotbarHeight)) / 2.0f);

            for (int visualRow = 0; visualRow < 3; visualRow++) {
                float y = invY + (visualRow * hotbarHeight);
                addAtlasQuad(guiVerts, guiInds, hx, y, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);
            }

            for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
                float rowY = invY + (logicalRow * hotbarHeight);

                for (int col = 0; col < 9; col++) {
                    int slot = 9 + (logicalRow * 9) + col;
                    float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                    float selectorW = 23.0f * pixelScale;

                    if (hoveredSlot == slot) {
                        addAtlasQuad(guiVerts, guiInds, slotX, rowY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
                    }

                    if (inventory.getStack(slot) != null) {
                        drawItem(guiVerts, guiInds, inventory.getStack(slot), slotX, rowY, 0.0f, selectorW);
                    }
                }
            }

            if (inventory.getMouseStack() != null) {
                float itemSize = 23.0f * pixelScale;
                float invertedMouseY = height - mouseY;
                drawItem(guiVerts, guiInds, inventory.getMouseStack(), mouseX - itemSize / 2.0f, invertedMouseY - itemSize / 2.0f, -0.1f, itemSize);
            }
        }

        if (!guiVerts.isEmpty()) {
            float[] vArr = new float[guiVerts.size()];
            for (int i = 0; i < guiVerts.size(); i++) vArr[i] = guiVerts.get(i);
            int[] iArr = new int[guiInds.size()];
            for (int i = 0; i < guiInds.size(); i++) iArr[i] = guiInds.get(i);
            guiMesh = new VulkanMesh(context, vArr, iArr);
        }

        if (!textVerts.isEmpty()) {
            float[] vArr = new float[textVerts.size()];
            for (int i = 0; i < textVerts.size(); i++) vArr[i] = textVerts.get(i);
            int[] iArr = new int[textInds.size()];
            for (int i = 0; i < textInds.size(); i++) iArr[i] = textInds.get(i);
            textMesh = new VulkanMesh(context, vArr, iArr);
        }
    }

    private void drawItem(List<Float> verts, List<Integer> inds, ItemStack stack, float x, float y, float z, float size) {
        int idx = stack.type.iconIndex;
        int gridX = idx % 9;
        int gridY = 3 + (idx / 9);
        addAtlasQuad(verts, inds, x, y, z, size, size, gridX, gridY, 1, 1, true);
    }

    private void addAtlasQuad(List<Float> verts, List<Integer> inds, float x, float y, float z, float w, float h, int gridX, int gridY, int gridW, int gridH, boolean flipV) {
        int offset = verts.size() / 8;
        float epsilon = 0.0005f;

        float u0 = (float) gridX / GRID_SIZE + epsilon;
        float v0 = (float) gridY / GRID_SIZE + epsilon;
        float u1 = (float) (gridX + gridW) / GRID_SIZE - epsilon;
        float v1 = (float) (gridY + gridH) / GRID_SIZE - epsilon;

        float finalV0 = flipV ? v1 : v0;
        float finalV1 = flipV ? v0 : v1;

        verts.addAll(List.of(
                x, y, z, 1.0f, 1.0f, 1.0f, u0, finalV0,
                x + w, y, z, 1.0f, 1.0f, 1.0f, u1, finalV0,
                x + w, y + h, z, 1.0f, 1.0f, 1.0f, u1, finalV1,
                x, y + h, z, 1.0f, 1.0f, 1.0f, u0, finalV1
        ));

        inds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));
    }

    private void drawText(List<Float> verts, List<Integer> inds, String text, float startX, float startY, float z, VulkanFont font) {
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

                // FIX 3: DIE MAGIE
                // STBTT yoff ist negativ (vom Boden zum Kopf).
                // Da Y bei dir nach OBEN wächst, MÜSSEN wir yoff abziehen, um an die Oberkante zu kommen!
                float yTop = currentY - bakedChar.yoff();
                float yBottom = yTop - glyphHeight;

                float atlasSize = (float) font.BITMAP_SIZE;
                float u0 = bakedChar.x0() / atlasSize;
                float v0 = bakedChar.y0() / atlasSize; // Top UV
                float u1 = bakedChar.x1() / atlasSize;
                float v1 = bakedChar.y1() / atlasSize; // Bottom UV

                int offset = verts.size() / 8;

                // yBottom bekommt v1, yTop bekommt v0. Genau wie in addAtlasQuad!
                verts.addAll(List.of(
                        x0, yBottom, z, 1.0f, 1.0f, 1.0f, u0, v1, // Bottom Left
                        x1, yBottom, z, 1.0f, 1.0f, 1.0f, u1, v1, // Bottom Right
                        x1, yTop,    z, 1.0f, 1.0f, 1.0f, u1, v0, // Top Right
                        x0, yTop,    z, 1.0f, 1.0f, 1.0f, u0, v0  // Top Left
                ));

                inds.addAll(List.of(offset, offset + 1, offset + 2, offset + 2, offset + 3, offset));

                currentX += bakedChar.xadvance();
            }
        }
    }

    public VulkanMesh getGuiMesh() { return guiMesh; }
    public VulkanMesh getTextMesh() { return textMesh; }

    public void cleanup() {
        if (guiMesh != null) guiMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
    }
}