package de.delautrer.game.ui;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.MeshData;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.elements.UIDrawCall;
import de.delautrer.game.ui.elements.UITexture;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIRenderer {

    private IMesh[] combinedMeshes = new IMesh[de.delautrer.engine.graphics.vulkan.core.VulkanSync.MAX_FRAMES_IN_FLIGHT];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final List<UIDrawCall>[] drawCalls = new List[de.delautrer.engine.graphics.vulkan.core.VulkanSync.MAX_FRAMES_IN_FLIGHT];

    private IGraphicsFactory factory;
    private final IGraphicsContext graphicsContext;
    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(IGraphicsFactory factory, IGraphicsContext graphicsContext) {
        this.factory = factory;
        this.graphicsContext = graphicsContext;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
        for (int i = 0; i < drawCalls.length; i++) {
            drawCalls[i] = new ArrayList<>();
        }
    }

    public void setGraphicsFactory(IGraphicsFactory factory) {
        this.factory = factory;
    }

    public void rebuildMesh(int frameIndex, int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, IFont font, MenuScreen pauseScreen, int blockAtlasWidth) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[UIRenderer] rebuildMesh requested for frameIndex: " + frameIndex + ", size: " + width + "x" + height);
        }

        // --- HIER WURDE AUFGERÄUMT: Kein WaitIdle und kein cleanup mehr am Anfang! ---
        drawCalls[frameIndex].clear();
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

        Object currentTex = null;
        int currentStartIndex = 0;
        int currentIndexCount = 0;

        for (Map<Object, UIMeshBuilder.Batch> layer : meshBuilder.getLayers().values()) {
            for (Map.Entry<Object, UIMeshBuilder.Batch> entry : layer.entrySet()) {
                Object tex = entry.getKey();
                UIMeshBuilder.Batch batch = entry.getValue();

                if (batch.inds.isEmpty()) continue;

                if (currentTex == tex) {
                    currentIndexCount += batch.inds.size();
                } else {
                    if (currentTex != null) {
                        drawCalls[frameIndex].add(new UIDrawCall(currentTex, currentStartIndex, currentIndexCount));
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
            drawCalls[frameIndex].add(new UIDrawCall(currentTex, currentStartIndex, currentIndexCount));
        }

        // --- MESH UPDATE ---
        if (!allVerts.isEmpty()) {
            MeshData newMeshData = new MeshData(toArray(allVerts), toIntArray(allInds));
            if (de.delautrer.Constants.VULKAN_DEBUG) {
                System.out.println("[UIRenderer] Updating UI mesh for frameIndex " + frameIndex + ". Vertices: " + (allVerts.size() / 8) + ", Indices: " + allInds.size() + ", Draw calls: " + drawCalls[frameIndex].size());
            }

            if (combinedMeshes[frameIndex] == null) {
                combinedMeshes[frameIndex] = factory.createMesh(newMeshData);
            } else {
                combinedMeshes[frameIndex].updateMesh(newMeshData.vertices(), newMeshData.indices());
            }
        } else {
            if (de.delautrer.Constants.VULKAN_DEBUG) {
                System.out.println("[UIRenderer] UI mesh is empty for frameIndex " + frameIndex);
            }
            if (combinedMeshes[frameIndex] != null) {
                combinedMeshes[frameIndex].cleanup();
                combinedMeshes[frameIndex] = null;
            }
        }
    }

    public IMesh getCombinedMesh(int frameIndex) { return combinedMeshes[frameIndex]; }
    public List<UIDrawCall> getDrawCalls(int frameIndex) { return drawCalls[frameIndex]; }

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
        for(int i=0; i<combinedMeshes.length; i++) {
            if (combinedMeshes[i] != null) combinedMeshes[i].cleanup();
        }
    }
}
