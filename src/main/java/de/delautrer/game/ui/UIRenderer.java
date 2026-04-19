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
    private final VulkanContext context;

    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
    }

    public void rebuildMesh(int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font, MenuScreen pauseScreen) {
        VK10.vkDeviceWaitIdle(context.getDevice());
        if (uiMesh != null) uiMesh.cleanup();
        if (itemMesh != null) itemMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
        uiMesh = null; itemMesh = null; textMesh = null;

        meshBuilder.clear();

        // --- FIX: HUD und Inventar nur rendern, wenn wir im normalen Spiel oder im Chat sind! ---
        boolean renderHUD = true;
        if (pauseScreen != null) {
            String screenName = pauseScreen.getClass().getSimpleName();
            // Pause- oder Ladescreen sollen das HUD komplett verstecken!
            if (!screenName.equals("ChatScreen")) {
                renderHUD = false;
            }
        }

        // 1. Alles für HUD & Inventare sammeln (nur wenn renderHUD true ist)
        if (renderHUD) {
            uiManager.update(input, interaction);
            uiManager.buildMeshes(meshBuilder, width, height, input, interaction, mouseX, mouseY, debugOverlay, chatOverlay, font);
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
    }

    // Und passe die Getter an:
    public VulkanMesh getUiMesh() { return uiMesh; }
    public VulkanMesh getItemMesh() { return itemMesh; }
    public VulkanMesh getTextMesh() { return textMesh; }

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
        if (uiMesh != null) uiMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
        if (itemMesh != null) itemMesh.cleanup();
    }
}