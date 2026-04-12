package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.gui.UIManager;
import de.delautrer.game.ui.gui.UIMeshBuilder;
import org.lwjgl.vulkan.VK10;

public class UIRenderer {

    private VulkanMesh guiMesh;
    private VulkanMesh textMesh;
    private final VulkanContext context;

    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(VulkanContext context, int width, int height) {
        this.context = context;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
    }

    public void rebuildMesh(int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, VulkanFont font) {
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

        uiManager.update(input, interaction);
        uiManager.buildMeshes(meshBuilder, width, height, input, interaction, mouseX, mouseY, debugOverlay, font);

        if (!meshBuilder.guiVerts.isEmpty()) {
            float[] vArr = new float[meshBuilder.guiVerts.size()];
            for (int i = 0; i < meshBuilder.guiVerts.size(); i++) vArr[i] = meshBuilder.guiVerts.get(i);
            int[] iArr = new int[meshBuilder.guiInds.size()];
            for (int i = 0; i < meshBuilder.guiInds.size(); i++) iArr[i] = meshBuilder.guiInds.get(i);
            guiMesh = new VulkanMesh(context, vArr, iArr);
        }

        if (!meshBuilder.textVerts.isEmpty()) {
            float[] vArr = new float[meshBuilder.textVerts.size()];
            for (int i = 0; i < meshBuilder.textVerts.size(); i++) vArr[i] = meshBuilder.textVerts.get(i);
            int[] iArr = new int[meshBuilder.textInds.size()];
            for (int i = 0; i < meshBuilder.textInds.size(); i++) iArr[i] = meshBuilder.textInds.get(i);
            textMesh = new VulkanMesh(context, vArr, iArr);
        }
    }

    public VulkanMesh getGuiMesh() { return guiMesh; }
    public VulkanMesh getTextMesh() { return textMesh; }

    public void cleanup() {
        if (guiMesh != null) guiMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
    }
}