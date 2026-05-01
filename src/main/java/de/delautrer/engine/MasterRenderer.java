package de.delautrer.engine;

import de.delautrer.Constants;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.window.Window;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.UIRenderer;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.sky.SkyManager;
import de.delautrer.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.vulkan.VK10;

public class MasterRenderer {
    private final VulkanContext vulkanContext;
    private final Window window;

    private final VulkanRenderer renderer;
    private final UIRenderer uiRenderer;

    private VulkanTextureArray worldTexture;
    private VulkanTexture uiTexture;
    private VulkanTexture itemTexture;
    private VulkanTexture fontTexture;
    private VulkanTexture blockUITexture;
    private VulkanMesh highlightMesh;
    private VulkanMesh starMesh;
    private VulkanMesh cloudMesh;
    private VulkanMesh celestialMesh;
    private VulkanMesh dynamicHighlightMesh;
    private VulkanFont font;

    private final TextureStitcher.AtlasResult blockAtlas;
    private final TextureStitcher.AtlasResult itemAtlas;

    private int lastVisibleChunkCount = 0;

    public MasterRenderer(VulkanContext vulkanContext, Window window, TextureStitcher.AtlasResult blockAtlas, TextureStitcher.AtlasResult itemAtlas) {
        this.vulkanContext = vulkanContext;
        this.window = window;
        this.blockAtlas = blockAtlas;
        this.itemAtlas = itemAtlas;
        this.renderer = new VulkanRenderer(vulkanContext, window);
        this.uiRenderer = new UIRenderer(vulkanContext, renderer.getWidth(), renderer.getHeight());

        initResources();
    }

    private void initResources() {
        worldTexture = new VulkanTextureArray(vulkanContext, renderer.getCommandBuffers(), renderer.getGraphicsLayout(), blockAtlas);
        uiTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), "menu_gui.png");
        itemTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), itemAtlas);
        blockUITexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), blockAtlas);

        font = new VulkanFont("MinecraftRegular-Bmg3.otf", 24.0f);
        if (font.getRgbaPixels() != null) {
            fontTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), font.getRgbaPixels(), font.BITMAP_SIZE, font.BITMAP_SIZE);
        }

        highlightMesh = new VulkanMesh(vulkanContext, Chunk.getHighlightVertices(), Chunk.getHighlightIndices());
    }

    public void initClouds(MeshData data) {
        this.cloudMesh = new VulkanMesh(vulkanContext, data.vertices, data.indices);
    }
    public void initStars(MeshData starData) {
        if (this.starMesh != null) this.starMesh.cleanup();
        this.starMesh = new VulkanMesh(vulkanContext, starData);
    }

    public void initCelestial(MeshData data) {
        if (this.celestialMesh != null) this.celestialMesh.cleanup();
        this.celestialMesh = new VulkanMesh(vulkanContext, data);
    }

    public void rebuildUI(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay, MenuScreen pauseScreen, ChatOverlay chatOverlay) {
        uiRenderer.rebuildMesh(
                renderer.getWidth(), renderer.getHeight(),
                input, interaction, input.getMouseX(), input.getMouseY(),
                debugOverlay, chatOverlay, font, pauseScreen, blockAtlas.atlasWidth
        );
    }

    public boolean drawFrame(Camera camera, World world, PlayerInteraction interaction, boolean hideUI, int isoFramesToWait, boolean isTakingIsometric) {
        SkyManager skyManager = world.getSkyManager();
        float aspect = (float) renderer.getWidth() / (float) renderer.getHeight();
        Matrix4f view = camera.getViewMatrix();

        // --- 1. NEU: View-Matrix ohne Translation (nur Rotation) ---
        Matrix4f viewRotOnly = new Matrix4f(view).setTranslation(0, 0, 0);

        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0f), aspect, 0.01f, 1000.0f);
        proj.m11(proj.m11() * -1); // Vulkan Y-Flip

        // Die Camera-Relative MVP (für den Shader ohne Ruckeln!)
        Matrix4f mvpCameraRelative = new Matrix4f(proj).mul(viewRotOnly);

        RenderPacket packet = new RenderPacket();
        packet.mvp = mvpCameraRelative;
        packet.proj = proj;
        packet.view = viewRotOnly;
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);
        packet.cameraPos = camera.getPosition();

        boolean isIsoFrame = (isTakingIsometric && isoFramesToWait == -1);

        CullingUtils.buildVisibleLists(world.getChunkManager(), mvpCameraRelative, packet, isIsoFrame, packet.cameraPos);
        lastVisibleChunkCount = packet.opaqueMeshes.size() + packet.waterMeshes.size();

        packet.cloudMesh = this.cloudMesh;
        packet.cloudOffset = world.getCloudSystem().getRenderOffset(
                (float) camera.getPosition().x,
                (float) camera.getPosition().z
        );
        packet.starMesh = this.starMesh;
        packet.starAlpha = skyManager.getStarAlpha();
        packet.timeOfDay = skyManager.getTimeOfDay();
        packet.celestialMesh = this.celestialMesh;
        // ---------------------------

        packet.isUnderwater = interaction.getPlayer().isHeadInWater();
        packet.cameraPos = camera.getPosition();
        packet.renderDistance = Constants.RENDERDISTANCE * 16.0f;

        Vector3f skyColor = skyManager.getCurrentSkyColor();
        float intensity = skyManager.getGlobalLightIntensity();
        if(packet.isUnderwater)
            renderer.setClearColor(0.02f*intensity, 0.1f*intensity, 0.3f*intensity);
        else
            renderer.setClearColor(skyColor.x, skyColor.y, skyColor.z);

        packet.blockUITexture = blockUITexture;
        packet.uiCombinedMesh = uiRenderer.getCombinedMesh();
        packet.uiDrawCalls = uiRenderer.getDrawCalls();

        packet.uiTexture = uiTexture;
        packet.itemTexture = itemTexture;
        packet.fontTexture = fontTexture;
        packet.worldTexture = worldTexture;

        Vector3i selectedBlockPos = interaction.getSelectedBlockPos();
        packet.selectedBlockPos = selectedBlockPos;

        // --- HIGHLIGHT & CRACKING MESH LÖSCHEN ---
        if (dynamicHighlightMesh != null) {
            VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
            dynamicHighlightMesh.cleanup();
            dynamicHighlightMesh = null;
        }
        if (packet.overlayMesh != null) {
            VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
            packet.overlayMesh.cleanup();
            packet.overlayMesh = null;
        }

        if(selectedBlockPos != null) {
            byte selectedBlockId = world.getBlockAt(selectedBlockPos);
            Block block = BlockRegistry.get(selectedBlockId);
            BlockState state = world.getBlockState(selectedBlockPos);

            dynamicHighlightMesh = new VulkanMesh(vulkanContext, block.getHighlightVertices(state), block.getHighlightIndices(state));
            packet.highlightMesh = dynamicHighlightMesh;

            // ==========================================
            // NEU: MINING CRACKING OVERLAY BERECHNEN
            // ==========================================
            float miningProgress = interaction.getMiningProgressPercent();
            if (miningProgress > 0.0f) {
                // Berechne Stage 0 bis 9
                int stage = (int) Math.min(9, Math.floor(miningProgress * 10.0f));
                String textureName = "destroy_stage_" + stage;

                // Prüfen, ob die Textur im Atlas ist
                if (blockAtlas.regions.containsKey(textureName)) {
                    float layer = blockAtlas.regions.get(textureName).layer;
                    packet.overlayMesh = buildCrackingMesh(selectedBlockPos, layer);
                }
            }
        } else {
            packet.highlightMesh = highlightMesh;
        }

        packet.entities = world.getEntities();

        packet.sunDirection = skyManager.getSunDirection();
        packet.globalLight = skyManager.getGlobalLightIntensity();
        packet.skyR = skyColor.x;
        packet.skyG = skyColor.y;
        packet.skyB = skyColor.z;

        packet.hideUI = hideUI; // F1 Schalter weitergeben

        if (isIsoFrame) {
            packet.hideUI = true;
            packet.clipY = (float) Math.min(54f, interaction.getPlayer().getEyePosition().y - 15.0f );
            packet.renderDistance = 10000.0f;
            float zoom = 80.0f;

            packet.proj = new org.joml.Matrix4f().ortho(-zoom * aspect, zoom * aspect, -zoom, zoom, -2000.0f, 2000.0f);
            packet.proj.m11(packet.proj.m11() * -1.0f);

            org.joml.Vector3d target = interaction.getPlayer().position;
            org.joml.Vector3d eye = new org.joml.Vector3d(target.x + 100.0, target.y + 60.0, target.z + 100.0);

            packet.view = new org.joml.Matrix4f().lookAt(new Vector3f((float)eye.x, (float)eye.y, (float)eye.z), new Vector3f((float)target.x, (float)target.y, (float)target.z), new org.joml.Vector3f(0, 1, 0));

            // --- 3. NEU: Auch Iso-Ansicht relativ machen ---
            Matrix4f isoViewRotOnly = new Matrix4f(packet.view).setTranslation(0, 0, 0);
            packet.mvp = new org.joml.Matrix4f(packet.proj).mul(isoViewRotOnly);
            packet.cameraPos = eye; // Sicherstellen, dass die Kamera-Pos für die Offset-Berechnung im Shader stimmt
        } else {
            packet.clipY = -999.0f;
            packet.renderDistance = Constants.RENDERDISTANCE * 16.0f;
            packet.proj = proj;

            // --- 4. NEU: Hier ebenfalls die rot-only Matrizen zuweisen ---
            packet.view = viewRotOnly;
            packet.mvp = mvpCameraRelative;
        }

        boolean success = renderer.render(packet);

        if (packet.overlayMesh != null) {
            VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
            packet.overlayMesh.cleanup();
        }

        return success;
    }

    // Hilfsmethode, um den Crack-Würfel zu bauen
    private VulkanMesh buildCrackingMesh(Vector3i pos, float layer) {
        float x = pos.x, y = pos.y, z = pos.z;
        float e = -0.005f; // Epsilon (leicht ausdehnen, damit es nicht z-fightet)
        float s = 1.0f + 0.005f; // Size

        // Format: x, y, z, r, g, b, a, u, v, layer, skyLight, blockLight (12 Floats)
        float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f; // Keine Verdunkelung
        float sl = 1.0f, bl = 1.0f; // Volles Licht auf den Rissen

        float[] verts = {
                // Front (-Z)
                x+e, y+e, z+e, r,g,b,a, 0,1, layer, sl,bl,
                x+s, y+e, z+e, r,g,b,a, 1,1, layer, sl,bl,
                x+s, y+s, z+e, r,g,b,a, 1,0, layer, sl,bl,
                x+e, y+s, z+e, r,g,b,a, 0,0, layer, sl,bl,
                // Back (+Z)
                x+e, y+e, z+s, r,g,b,a, 1,1, layer, sl,bl,
                x+s, y+e, z+s, r,g,b,a, 0,1, layer, sl,bl,
                x+s, y+s, z+s, r,g,b,a, 0,0, layer, sl,bl,
                x+e, y+s, z+s, r,g,b,a, 1,0, layer, sl,bl,
                // Left (-X)
                x+e, y+e, z+e, r,g,b,a, 1,1, layer, sl,bl,
                x+e, y+e, z+s, r,g,b,a, 0,1, layer, sl,bl,
                x+e, y+s, z+s, r,g,b,a, 0,0, layer, sl,bl,
                x+e, y+s, z+e, r,g,b,a, 1,0, layer, sl,bl,
                // Right (+X)
                x+s, y+e, z+e, r,g,b,a, 0,1, layer, sl,bl,
                x+s, y+e, z+s, r,g,b,a, 1,1, layer, sl,bl,
                x+s, y+s, z+s, r,g,b,a, 1,0, layer, sl,bl,
                x+s, y+s, z+e, r,g,b,a, 0,0, layer, sl,bl,
                // Top (+Y)
                x+e, y+s, z+e, r,g,b,a, 0,1, layer, sl,bl,
                x+s, y+s, z+e, r,g,b,a, 1,1, layer, sl,bl,
                x+s, y+s, z+s, r,g,b,a, 1,0, layer, sl,bl,
                x+e, y+s, z+s, r,g,b,a, 0,0, layer, sl,bl,
                // Bottom (-Y)
                x+e, y+e, z+e, r,g,b,a, 0,0, layer, sl,bl,
                x+s, y+e, z+e, r,g,b,a, 1,0, layer, sl,bl,
                x+s, y+e, z+s, r,g,b,a, 1,1, layer, sl,bl,
                x+e, y+e, z+s, r,g,b,a, 0,1, layer, sl,bl,
        };

        int[] inds = {
                2,1,0, 0,3,2,       // Front
                6,7,4, 4,5,6,       // Back
                10,11,8, 8,9,10,    // Left
                14,13,12, 12,15,14, // Right
                18,17,16, 16,19,18, // Top
                22,23,20, 20,21,22  // Bottom
        };

        return new VulkanMesh(vulkanContext, verts, inds);
    }

    public void recreate(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay, MenuScreen pauseScreen, ChatOverlay chatOverlay) {
        renderer.recreate(window);
        rebuildUI(interaction, input, debugOverlay, pauseScreen, chatOverlay);
    }

    public int getLastVisibleChunkCount() { return lastVisibleChunkCount; }

    public void cleanup() {
        if (celestialMesh != null) celestialMesh.cleanup();
        if (starMesh != null) starMesh.cleanup();
        if (cloudMesh != null) cloudMesh.cleanup();
        if (worldTexture != null) worldTexture.cleanup();
        if (blockUITexture != null) blockUITexture.cleanup();
        if (uiTexture != null) uiTexture.cleanup();
        if (itemTexture != null) itemTexture.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();
        if (font != null) font.cleanup();
        if (renderer != null) renderer.cleanup();
    }

    public VulkanFont getFont() {
        return font;
    }

    public void requestScreenshot(String path) {
        if (this.renderer != null) {
            this.renderer.requestScreenshot(path);
        }
    }
}