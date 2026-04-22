package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.gui.UIManager;
import de.delautrer.game.ui.gui.UIMeshBuilder;
import de.delautrer.game.ui.gui.screens.MenuScreen; // NEU
import org.lwjgl.vulkan.VK10;

public class UIRenderer {

    private VulkanMesh uiMesh;
    private VulkanMesh itemMesh;
    private VulkanMesh textMesh;
    private VulkanMesh overlayMesh;
    private VulkanMesh topUiMesh;
    private final VulkanContext context;

    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
    }

    public void rebuildMesh(int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font, MenuScreen pauseScreen, int blockAtlasWidth) {
        VK10.vkDeviceWaitIdle(context.getDevice());
        if (topUiMesh != null) topUiMesh.cleanup();
        topUiMesh = null;
        if (uiMesh != null) uiMesh.cleanup();
        if (itemMesh != null) itemMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
        if (overlayMesh != null) overlayMesh.cleanup();
        uiMesh = null; itemMesh = null; textMesh = null; overlayMesh = null;

        meshBuilder.clear();

        boolean renderHUD = true;
        if (pauseScreen != null) {
            String screenName = pauseScreen.getClass().getSimpleName();
            if (!screenName.equals("ChatScreen")) {
                renderHUD = false;
            }
        }

        // 1. Alles für HUD & Inventare sammeln (nur wenn renderHUD true ist)
        if (renderHUD) {
            uiManager.update(input, interaction);
            uiManager.buildMeshes(meshBuilder, width, height, input, interaction, mouseX, mouseY, debugOverlay, chatOverlay, font, blockAtlasWidth);
        }

        // 2. Pause/Menu Screen obendrauf rendern
        if (pauseScreen != null) {
            float uiMouseY = height - mouseY;
            pauseScreen.render(meshBuilder, mouseX, uiMouseY);
        }

        // 3. Meshes bauen!
        if (!meshBuilder.uiVerts.isEmpty()) {
            uiMesh = new VulkanMesh(context, toArray(meshBuilder.uiVerts), toIntArray(meshBuilder.uiInds));
        }
        if (!meshBuilder.itemVerts.isEmpty()) {
            itemMesh = new VulkanMesh(context, toArray(meshBuilder.itemVerts), toIntArray(meshBuilder.itemInds));
        }
        if (!meshBuilder.textVerts.isEmpty()) {
            textMesh = new VulkanMesh(context, toArray(meshBuilder.textVerts), toIntArray(meshBuilder.textInds));
        }
        if (!meshBuilder.overlayVerts.isEmpty()) {
            overlayMesh = new VulkanMesh(context, toArray(meshBuilder.overlayVerts), toIntArray(meshBuilder.overlayInds));
        }
        if (!meshBuilder.topUiVerts.isEmpty()) {
            topUiMesh = new VulkanMesh(context, toArray(meshBuilder.topUiVerts), toIntArray(meshBuilder.topUiInds));
        }
    }

    public VulkanMesh getUiMesh() { return uiMesh; }
    public VulkanMesh getItemMesh() { return itemMesh; }
    public VulkanMesh getTextMesh() { return textMesh; }
    public VulkanMesh getOverlayMesh() {
        return overlayMesh;
    }
    public VulkanMesh getTopUiMesh() { return topUiMesh; }

    private float[] toArray(java.util.List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
    private int[] toIntArray(java.util.List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public void cleanup() {
        if (topUiMesh != null) topUiMesh.cleanup();
        if (uiMesh != null) uiMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
        if (itemMesh != null) itemMesh.cleanup();
        if (overlayMesh != null) overlayMesh.cleanup();
    }
}