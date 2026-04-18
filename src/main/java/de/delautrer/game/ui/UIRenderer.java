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

    private VulkanMesh guiMesh;   // Für HUD (gui.png)
    private VulkanMesh menuMesh;  // Für Pause/Menü (menu_gui.png)
    private VulkanMesh textMesh;
    private final VulkanContext context;

    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
    }

    public void rebuildMesh(int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font, MenuScreen pauseScreen) {
        // Aufräumen der alten Meshes
        VK10.vkDeviceWaitIdle(context.getDevice());
        if (guiMesh != null) guiMesh.cleanup();
        if (menuMesh != null) menuMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();

        guiMesh = null;
        menuMesh = null;
        textMesh = null;

        // --- SCHRITT 1: HUD BAUEN (gui.png) ---
        meshBuilder.clear();
        uiManager.update(input, interaction);
        uiManager.buildMeshes(meshBuilder, width, height, input, interaction, mouseX, mouseY, debugOverlay, chatOverlay, font);

        if (!meshBuilder.guiVerts.isEmpty()) {
            guiMesh = new VulkanMesh(context, toArray(meshBuilder.guiVerts), toIntArray(meshBuilder.guiInds));
        }

        // --- SCHRITT 2: MENÜ BAUEN (menu_gui.png) ---
        meshBuilder.guiVerts.clear();
        meshBuilder.guiInds.clear();

        if (pauseScreen != null) {
            float uiMouseY = height - mouseY;
            pauseScreen.render(meshBuilder, mouseX, uiMouseY);

            if (!meshBuilder.guiVerts.isEmpty()) {
                menuMesh = new VulkanMesh(context, toArray(meshBuilder.guiVerts), toIntArray(meshBuilder.guiInds));
            }
        }

        // --- SCHRITT 3: TEXT BAUEN (bleibt wie es ist) ---
        if (!meshBuilder.textVerts.isEmpty()) {
            textMesh = new VulkanMesh(context, toArray(meshBuilder.textVerts), toIntArray(meshBuilder.textInds));
        }
    }

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

    public VulkanMesh getGuiMesh() { return guiMesh; }
    public VulkanMesh getMenuMesh() { return menuMesh; } // NEU
    public VulkanMesh getTextMesh() { return textMesh; }

    public void cleanup() {
        if (guiMesh != null) guiMesh.cleanup();
        if (menuMesh != null) menuMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
    }
}