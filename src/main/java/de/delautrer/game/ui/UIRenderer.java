package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.gui.*;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIRenderer {

    private VulkanMesh combinedMesh;
    private final List<UIDrawCall> drawCalls = new ArrayList<>();

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
        if (combinedMesh != null) combinedMesh.cleanup();
        combinedMesh = null;
        drawCalls.clear();
        meshBuilder.clear();

        boolean renderHUD = true;
        if (pauseScreen != null) {
            String screenName = pauseScreen.getClass().getSimpleName();
            if (!screenName.equals("ChatScreen")) renderHUD = false;
        }

        if (renderHUD) {
            uiManager.update(input, interaction);
            uiManager.buildMeshes(meshBuilder, width, height, input, interaction, mouseX, mouseY, debugOverlay, chatOverlay, font, blockAtlasWidth);
        }

        if (pauseScreen != null) {
            float uiMouseY = height - mouseY;
            pauseScreen.render(meshBuilder, mouseX, uiMouseY);
        }

        // --- FLATTENING: Alles in EIN Mesh packen ---
        List<Float> allVerts = new ArrayList<>();
        List<Integer> allInds = new ArrayList<>();
        int globalVertexOffset = 0;

        UITexture currentTex = null;
        int currentStartIndex = 0;
        int currentIndexCount = 0;

        // Durchläuft die Layer automatisch nach Z-Index sortiert (dank TreeMap)
        for (Map<UITexture, UIMeshBuilder.Batch> layer : meshBuilder.getLayers().values()) {
            for (Map.Entry<UITexture, UIMeshBuilder.Batch> entry : layer.entrySet()) {
                UITexture tex = entry.getKey();
                UIMeshBuilder.Batch batch = entry.getValue();

                if (batch.inds.isEmpty()) continue;

                // Benachbarte Draw-Calls mit gleicher Textur verschmelzen!
                if (currentTex == tex) {
                    currentIndexCount += batch.inds.size();
                } else {
                    if (currentTex != null) {
                        drawCalls.add(new UIDrawCall(currentTex, currentStartIndex, currentIndexCount));
                    }
                    currentTex = tex;
                    currentStartIndex = allInds.size();
                    currentIndexCount = batch.inds.size();
                }

                for (int ind : batch.inds) {
                    allInds.add(ind + globalVertexOffset);
                }
                allVerts.addAll(batch.verts);
                globalVertexOffset += batch.verts.size() / 8;
            }
        }

        if (currentTex != null) {
            drawCalls.add(new UIDrawCall(currentTex, currentStartIndex, currentIndexCount));
        }

        if (!allVerts.isEmpty()) {
            combinedMesh = new VulkanMesh(context, toArray(allVerts), toIntArray(allInds));
        }
    }

    public VulkanMesh getCombinedMesh() { return combinedMesh; }
    public List<UIDrawCall> getDrawCalls() { return drawCalls; }

    private float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public void cleanup() {
        if (combinedMesh != null) combinedMesh.cleanup();
    }
}