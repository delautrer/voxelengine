package de.delautrer.engine;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.window.Window;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.UIRenderer;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.Environment;
import de.delautrer.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class MasterRenderer {
    private final VulkanContext vulkanContext;
    private final Window window;

    private final VulkanRenderer renderer;
    private final UIRenderer uiRenderer;

    private VulkanTextureArray worldTexture;
    private VulkanTexture guiTexture;
    private VulkanTexture fontTexture;
    private VulkanMesh highlightMesh;
    private VulkanMesh cloudMesh;
    private VulkanFont font;

    private int lastVisibleChunkCount = 0;

    public MasterRenderer(VulkanContext vulkanContext, Window window) {
        this.vulkanContext = vulkanContext;
        this.window = window;
        this.renderer = new VulkanRenderer(vulkanContext, window);
        this.uiRenderer = new UIRenderer(vulkanContext, renderer.getWidth(), renderer.getHeight());

        initResources();
    }

    private void initResources() {
        worldTexture = new VulkanTextureArray(vulkanContext, renderer.getCommandBuffers(), renderer.getGraphicsLayout(), "texture.png");
        guiTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), "gui.png");

        font = new VulkanFont("MinecraftRegular-Bmg3.otf", 24.0f);
        if (font.getRgbaPixels() != null) {
            fontTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), font.getRgbaPixels(), font.BITMAP_SIZE, font.BITMAP_SIZE);
        }

        highlightMesh = new VulkanMesh(vulkanContext, Chunk.getHighlightVertices(), Chunk.getHighlightIndices());
    }

    public void initClouds(MeshData data) {
        this.cloudMesh = new VulkanMesh(vulkanContext, data.vertices, data.indices);
    }

    public void rebuildUI(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay) {
        uiRenderer.rebuildMesh(
                renderer.getWidth(), renderer.getHeight(),
                input, interaction, input.getMouseX(), input.getMouseY(),
                debugOverlay, font
        );
    }

    public boolean drawFrame(Camera camera, World world, Environment environment, PlayerInteraction interaction) {
        float aspect = (float) renderer.getWidth() / (float) renderer.getHeight();
        Matrix4f view = camera.getViewMatrix();
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0f), aspect, 0.1f, 1000.0f);
        proj.m11(proj.m11() * -1); // Vulkan Y-Flip
        Matrix4f mvp = new Matrix4f(proj).mul(view);

        RenderPacket packet = new RenderPacket();
        packet.mvp = mvp;
        packet.proj = proj;
        packet.view = view;
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);

        java.util.List<VulkanMesh> visible = world.getVisibleMeshes(mvp);
        packet.visibleMeshes = visible;
        lastVisibleChunkCount = visible.size();

        packet.cloudMesh = this.cloudMesh;
        packet.cloudOffset = world.getCloudSystem().getRenderOffset(
                world.getPlayer().position.x,
                world.getPlayer().position.z
        );

        packet.uiMesh = uiRenderer.getGuiMesh();
        packet.guiTexture = guiTexture;
        packet.textMesh = uiRenderer.getTextMesh();
        packet.fontTexture = fontTexture;
        packet.worldTexture = worldTexture;
        Vector3i selectedBlockPos = interaction.getSelectedBlockPos();
        packet.selectedBlockPos = selectedBlockPos;
        if(selectedBlockPos != null) {
            byte selectedBlockId = world.getBlockAt(selectedBlockPos);
            Block block = BlockRegistry.get(selectedBlockId);
            packet.highlightMesh = new VulkanMesh(vulkanContext, block.getHighlightVertices(), block.getHighlightIndices());
        } else {
            packet.highlightMesh = highlightMesh;
        }
        Vector3f skyColor = environment.getCurrentSkyColor();
        packet.sunDirection = environment.getSunDirection();
        packet.globalLight = environment.getGlobalLightIntensity();
        packet.skyR = skyColor.x;
        packet.skyG = skyColor.y;
        packet.skyB = skyColor.z;

        return renderer.render(packet);
    }

    public void recreate(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay) {
        renderer.recreate(window);
        rebuildUI(interaction, input, debugOverlay);
    }

    public int getLastVisibleChunkCount() { return lastVisibleChunkCount; }

    public void cleanup() {
        if (cloudMesh != null) cloudMesh.cleanup();
        if (worldTexture != null) worldTexture.cleanup();
        if (guiTexture != null) guiTexture.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();
        if (font != null) font.cleanup();
        if (renderer != null) renderer.cleanup();
    }
}