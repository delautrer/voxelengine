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

    private IMesh combinedMesh;
    private final List<UIDrawCall> drawCalls = new ArrayList<>();

    private final IGraphicsFactory factory;
    private final IGraphicsContext graphicsContext;
    private final UIManager uiManager;
    private final UIMeshBuilder meshBuilder;

    public UIRenderer(IGraphicsFactory factory, IGraphicsContext graphicsContext) {
        this.factory = factory;
        this.graphicsContext = graphicsContext;
        this.uiManager = new UIManager();
        this.meshBuilder = new UIMeshBuilder();
    }

    public void rebuildMesh(int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, IFont font, MenuScreen pauseScreen, int blockAtlasWidth) {

        // --- HIER WURDE AUFGERÄUMT: Kein WaitIdle und kein cleanup mehr am Anfang! ---
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

        for (Map<UITexture, UIMeshBuilder.Batch> layer : meshBuilder.getLayers().values()) {
            for (Map.Entry<UITexture, UIMeshBuilder.Batch> entry : layer.entrySet()) {
                UITexture tex = entry.getKey();
                UIMeshBuilder.Batch batch = entry.getValue();

                if (batch.inds.isEmpty()) continue;

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

        // --- MESH UPDATE ---
        if (!allVerts.isEmpty()) {
            MeshData newMeshData = new MeshData(toArray(allVerts), toIntArray(allInds));

            // GPU kurz anhalten, damit wir die laufenden Daten SICHER aktualisieren können
            graphicsContext.waitIdle();

            if (combinedMesh == null) {
                combinedMesh = factory.createMesh(newMeshData);
            } else {
                combinedMesh.updateMesh(newMeshData.vertices(), newMeshData.indices());
            }
        } else {
            if (combinedMesh != null) {
                graphicsContext.waitIdle();
                combinedMesh.cleanup();
                combinedMesh = null;
            }
        }
    }

    public IMesh getCombinedMesh() { return combinedMesh; }
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
