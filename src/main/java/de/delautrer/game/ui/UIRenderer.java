package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.items.ItemStack;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.List;

public class UIRenderer {
    private VulkanMesh uiMesh;
    private final VulkanContext context;

    private static final float GRID_SIZE = 9.0f;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
    }

    public void rebuildMesh(int width, int height, Inventory inventory, float mouseX, float mouseY, int hoveredSlot) {
        if (uiMesh != null) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            uiMesh.cleanup();
        }

        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float pixelScale = 2.0f;
        if (height >= 1080) pixelScale = 3.0f;
        if (height >= 1440) pixelScale = 4.0f;

        float hotbarWidth = 207.0f * pixelScale;
        float hotbarHeight = 23.0f * pixelScale;

        // --- 1. FADENKREUZ ---
        if (!inventory.isOpen()) {
            float crosshairSize = 23.0f * pixelScale;
            float cx = (float) Math.floor((width - crosshairSize) / 2.0f);
            float cy = (float) Math.floor((height - crosshairSize) / 2.0f);
            addAtlasQuad(vertices, indices, cx, cy, 0.0f, crosshairSize, crosshairSize, 0, 0, 1, 1, false);
        }

        // --- 2. HOTBAR ---
        float hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        //float hy = (float) Math.floor(height - hotbarHeight - (10.0f * pixelScale));
        float hotbarY = (float) Math.floor(10.0f * pixelScale);

        // Hintergrund (Z = 0.2f, ganz hinten)
        addAtlasQuad(vertices, indices, hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);

        for (int col = 0; col < 9; col++) {
            // Exakt identische Formel zur Inventory.java!
            float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
            float selectorW = 23.0f * pixelScale;

            // Selektor
            if (!inventory.isOpen() && inventory.getSelectedSlot() == col) {
                addAtlasQuad(vertices, indices, slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            } else if (inventory.isOpen() && hoveredSlot == col) {
                addAtlasQuad(vertices, indices, slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            }

            // Item
            if (inventory.getStack(col) != null) {
                drawItem(vertices, indices, inventory.getStack(col), slotX, hotbarY, 0.0f, selectorW);
            }
        }

        // --- 3. INVENTAR GRID ---
        if (inventory.isOpen()) {
            float invY = (float) Math.floor((height - (3 * hotbarHeight)) / 2.0f);

            // Hintergrund-Reihen zeichnen
            for (int visualRow = 0; visualRow < 3; visualRow++) {
                float y = invY + (visualRow * hotbarHeight);
                addAtlasQuad(vertices, indices, hx, y, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);
            }

            // Items & Selektor im Grid
            for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
                // Exakt dieselbe Y-Zuordnung wie in der Inventory.java Hitbox!
                // logicalRow 0 (Slots 9-17) = Unten (invY)
                // logicalRow 1 (Slots 18-26) = Mitte (invY + 1h)
                // logicalRow 2 (Slots 27-35) = Oben (invY + 2h)
                float rowY = invY + (logicalRow * hotbarHeight);

                for (int col = 0; col < 9; col++) {
                    int slot = 9 + (logicalRow * 9) + col;
                    float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                    float selectorW = 23.0f * pixelScale;

                    if (hoveredSlot == slot) {
                        addAtlasQuad(vertices, indices, slotX, rowY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
                    }

                    if (inventory.getStack(slot) != null) {
                        drawItem(vertices, indices, inventory.getStack(slot), slotX, rowY, 0.0f, selectorW);
                    }
                }
            }

            // Drag'n'Drop Item an der Maus
            if (inventory.getMouseStack() != null) {
                float itemSize = 23.0f * pixelScale;
                // Auch hier muss die Maus gedreht werden, damit es passt!
                float invertedMouseY = height - mouseY;
                drawItem(vertices, indices, inventory.getMouseStack(), mouseX - itemSize / 2.0f, invertedMouseY - itemSize / 2.0f, -0.1f, itemSize);
            }
        }

        if (vertices.isEmpty()) {
            uiMesh = null;
            return;
        }

        float[] vArr = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) vArr[i] = vertices.get(i);
        int[] iArr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) iArr[i] = indices.get(i);

        uiMesh = new VulkanMesh(context, vArr, iArr);
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

    public VulkanMesh getMesh() { return uiMesh; }
    public void cleanup() { if (uiMesh != null) uiMesh.cleanup(); }
}