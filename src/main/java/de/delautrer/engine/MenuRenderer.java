package de.delautrer.engine;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.window.Window;
import de.delautrer.game.ui.gui.MenuScreen;
import de.delautrer.game.ui.gui.UIMeshBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VK10;
import java.util.ArrayList;

public class MenuRenderer {
    private final VulkanContext context;
    private final Window window;
    private final VulkanRenderer renderer;

    private VulkanTexture guiTexture;
    private VulkanTexture fontTexture;
    private VulkanFont font;

    private VulkanMesh uiMesh;
    private VulkanMesh textMesh;
    private final UIMeshBuilder meshBuilder;

    public MenuRenderer(VulkanContext context, Window window) {
        this.context = context;
        this.window = window;
        this.renderer = new VulkanRenderer(context, window);
        this.meshBuilder = new UIMeshBuilder();
        initResources();
    }

    private void initResources() {
        guiTexture = new VulkanTexture(context, renderer.getCommandBuffers(), renderer.getUiLayout(), "menu_gui.png");
        font = new VulkanFont("MinecraftRegular-Bmg3.otf", 24.0f);
        if (font.getRgbaPixels() != null) {
            fontTexture = new VulkanTexture(context, renderer.getCommandBuffers(), renderer.getUiLayout(), font.getRgbaPixels(), font.BITMAP_SIZE, font.BITMAP_SIZE);
        }
    }

    // ZWINGEND: Nimmt jetzt die invertierte MausY von außen!
    public void draw(MenuScreen screen, float uiMouseX, float uiMouseY) {
        if (uiMesh != null) { VK10.vkDeviceWaitIdle(context.getDevice()); uiMesh.cleanup(); uiMesh = null; }
        if (textMesh != null) { VK10.vkDeviceWaitIdle(context.getDevice()); textMesh.cleanup(); textMesh = null; }

        meshBuilder.clear();
        screen.render(meshBuilder, uiMouseX, uiMouseY); // Für Hover

        if (!meshBuilder.guiVerts.isEmpty()) {
            float[] vArr = new float[meshBuilder.guiVerts.size()];
            for (int i = 0; i < vArr.length; i++) vArr[i] = meshBuilder.guiVerts.get(i);
            int[] iArr = new int[meshBuilder.guiInds.size()];
            for (int i = 0; i < iArr.length; i++) iArr[i] = meshBuilder.guiInds.get(i);
            uiMesh = new VulkanMesh(context, vArr, iArr);
        }

        if (!meshBuilder.textVerts.isEmpty()) {
            float[] vArr = new float[meshBuilder.textVerts.size()];
            for (int i = 0; i < vArr.length; i++) vArr[i] = meshBuilder.textVerts.get(i);
            int[] iArr = new int[meshBuilder.textInds.size()];
            for (int i = 0; i < iArr.length; i++) iArr[i] = meshBuilder.textInds.get(i);
            textMesh = new VulkanMesh(context, vArr, iArr);
        }

        RenderPacket packet = new RenderPacket();
        // Standard-Vulkan-Matrix: Y=0 ist Unten.
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);
        packet.uiMesh = uiMesh;
        packet.guiTexture = guiTexture;
        packet.textMesh = textMesh;
        packet.fontTexture = fontTexture;

        packet.mvp = new Matrix4f(); packet.proj = new Matrix4f(); packet.view = new Matrix4f();
        packet.visibleMeshes = new ArrayList<>();
        packet.skyR = 0.1f; packet.skyG = 0.1f; packet.skyB = 0.15f;
        packet.sunDirection = new Vector3f(0, 1, 0);

        if (!renderer.render(packet)) { renderer.recreate(window); }
    }

    public void recreate() { renderer.recreate(window); }
    public VulkanFont getFont() { return font; }

    public void cleanup() {
        if (guiTexture != null) guiTexture.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
        if (uiMesh != null) uiMesh.cleanup();
        if (textMesh != null) textMesh.cleanup();
        if (font != null) font.cleanup();
        if (renderer != null) renderer.cleanup();
    }
}