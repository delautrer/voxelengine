package de.delautrer.engine;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.window.Window;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIDrawCall;
import de.delautrer.game.ui.elements.UITexture;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuRenderer {
    private final IGraphicsContext graphicsContext;
    private final Window window;
    private final VulkanRenderer renderer;
    private final IGraphicsFactory factory;

    private ITexture guiTexture;
    private ITexture fontTexture;
    private IFont font;

    // --- NEUES UI SYSTEM ---
    private IMesh uiCombinedMesh;
    private final List<UIDrawCall> drawCalls = new ArrayList<>();
    // -----------------------

    private final UIMeshBuilder meshBuilder;

    public MenuRenderer(IGraphicsContext graphicsContext, Window window) {
        this.graphicsContext = graphicsContext;
        this.window = window;
        VulkanContext context = (VulkanContext) graphicsContext;
        this.renderer = new VulkanRenderer(context, window);
        this.meshBuilder = new UIMeshBuilder();
        this.factory = new VulkanGraphicsFactory(context, renderer.getCommandBuffers(), renderer.getGraphicsLayout(), renderer.getUiLayout());
        initResources();
    }

    private void initResources() {
        guiTexture = factory.createTexture("menu_gui.png");
        font = factory.createFont("MinecraftRegular-Bmg3.otf", 24.0f);
        if (font.getRgbaPixels() != null) {
            fontTexture = factory.createTexture(font.getRgbaPixels(), font.getBitmapSize(), font.getBitmapSize());
        }
    }

    public void draw(MenuScreen screen, float uiMouseX, float uiMouseY) {
        if (uiCombinedMesh != null) {
            graphicsContext.waitIdle();
            uiCombinedMesh.cleanup();
            uiCombinedMesh = null;
        }

        drawCalls.clear();
        meshBuilder.clear();
        screen.render(meshBuilder, uiMouseX, uiMouseY);

        // --- FLATTENING LOGIK (Wie im UIRenderer) ---
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

        if (!allVerts.isEmpty()) {
            float[] vArr = new float[allVerts.size()];
            for (int i = 0; i < vArr.length; i++) vArr[i] = allVerts.get(i);
            int[] iArr = new int[allInds.size()];
            for (int i = 0; i < iArr.length; i++) iArr[i] = allInds.get(i);
            uiCombinedMesh = factory.createMesh(vArr, iArr);
        }
        // ----------------------------------------------

        RenderPacket packet = new RenderPacket();
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);

        // --- NEUES PAKET ---
        packet.uiCombinedMesh = uiCombinedMesh;
        packet.uiDrawCalls = drawCalls;
        packet.uiTexture = guiTexture;
        packet.fontTexture = fontTexture;
        // -------------------

        packet.mvp = new Matrix4f(); packet.proj = new Matrix4f(); packet.view = new Matrix4f();

        // --- FIX FÜR NPE ---
        packet.cameraPos = new Vector3d(0, 0, 0); // Dummy-Position für das Menü
        packet.renderDistance = 128.0f;           // Dummy-Sichtweite
        packet.opaqueMeshes = new ArrayList<>();   // Leere Liste statt null
        packet.waterMeshes = new ArrayList<>();    // Leere Liste statt null
        // -------------------

        packet.skyR = 0.1f; packet.skyG = 0.1f; packet.skyB = 0.15f;
        packet.sunDirection = new Vector3f(0, 1, 0);

        if (!renderer.render(packet)) { renderer.recreate(window); }
    }

    public void recreate() { renderer.recreate(window); }
    public IFont getFont() { return font; }

    public void cleanup() {
        if (guiTexture != null) guiTexture.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
        if (uiCombinedMesh != null) uiCombinedMesh.cleanup();
        if (font != null) font.cleanup();
        if (renderer != null) renderer.cleanup();
    }
}
