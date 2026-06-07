package de.delautrer.engine;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.VulkanRenderer;
import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import de.delautrer.engine.graphics.vulkan.core.VulkanGraphicsFactory;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.window.Window;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.settings.SettingsManager;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.UIRenderer;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.sky.SkyManager;
import de.delautrer.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class MasterRenderer {
    private final VulkanContext vulkanContext;
    private final Window window;

    private final VulkanRenderer renderer;
    private final UIRenderer uiRenderer;

    private IGraphicsFactory graphicsFactory;

    private ITextureArray worldTexture;
    private ITexture uiTexture;
    private ITexture itemTexture;
    private ITextureArray itemTextureArray;
    private ITexture fontTexture;
    private ITexture blockUITexture;
    private IMesh highlightMesh;
    private IMesh starMesh;
    private IMesh cloudMesh;
    private IMesh celestialMesh;

    // --- NEU: Beide dynamischen Meshes werden dauerhaft gespeichert ---
    private IMesh dynamicHighlightMesh;
    private IMesh dynamicOverlayMesh;
    private IMesh firstPersonMesh;
    private de.delautrer.game.items.Item lastFirstPersonItem;

    private IFont font;

    private final TextureStitcher.AtlasResult blockAtlas;
    private final TextureStitcher.AtlasResult itemAtlas;

    private int lastVisibleChunkCount = 0;

    public MasterRenderer(IGraphicsContext graphicsContext, Window window, TextureStitcher.AtlasResult blockAtlas,
            TextureStitcher.AtlasResult itemAtlas) {
        this.vulkanContext = (VulkanContext) graphicsContext;
        this.window = window;
        this.blockAtlas = blockAtlas;
        this.itemAtlas = itemAtlas;
        this.renderer = new VulkanRenderer(vulkanContext, window);
        this.graphicsFactory = new VulkanGraphicsFactory(vulkanContext, renderer.getCommandBuffers(),
                renderer.getGraphicsLayout(), renderer.getUiLayout());
        Engine.get().setGraphicsFactory(graphicsFactory);
        this.uiRenderer = new UIRenderer(graphicsFactory, graphicsContext);

        initResources();
    }

    private void initResources() {
        worldTexture = graphicsFactory.createTextureArray(blockAtlas);
        uiTexture = graphicsFactory.createTexture("menu_gui.png");
        itemTexture = graphicsFactory.createTexture(itemAtlas);
        itemTextureArray = graphicsFactory.createSingleLayerTextureArray(itemAtlas);
        blockUITexture = graphicsFactory.createTexture(blockAtlas);

        font = graphicsFactory.createFont(de.delautrer.Constants.GUI_FONT_NAME, de.delautrer.Constants.GUI_FONT_HEIGHT);
        if (font.getRgbaPixels() != null) {
            fontTexture = graphicsFactory.createTexture(font.getRgbaPixels(), font.getBitmapSize(),
                    font.getBitmapSize());
        }

        highlightMesh = graphicsFactory.createMesh(Chunk.getHighlightVertices(), Chunk.getHighlightIndices());
    }

    public void initClouds(MeshData data) {
        this.cloudMesh = graphicsFactory.createMesh(data);
    }

    public void initStars(MeshData starData) {
        if (this.starMesh != null)
            this.starMesh.cleanup();
        this.starMesh = graphicsFactory.createMesh(starData);
    }

    public void initCelestial(MeshData data) {
        if (this.celestialMesh != null)
            this.celestialMesh.cleanup();
        this.celestialMesh = graphicsFactory.createMesh(data);
    }

    public void rebuildUI(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay,
            MenuScreen pauseScreen, ChatOverlay chatOverlay) {
        uiRenderer.rebuildMesh(
                renderer.getWidth(), renderer.getHeight(),
                input, interaction, input.getMouseX(), input.getMouseY(),
                debugOverlay, chatOverlay, font, pauseScreen, blockAtlas.atlasWidth);
    }

    public boolean drawFrame(Camera camera, World world, PlayerInteraction interaction, boolean hideUI,
            int isoFramesToWait, boolean isTakingIsometric) {
        SkyManager skyManager = world.getSkyManager();
        float aspect = (float) renderer.getWidth() / (float) renderer.getHeight();
        Matrix4f view = camera.getViewMatrix();

        Matrix4f viewRotOnly = new Matrix4f(view).setTranslation(0, 0, 0);

        float fov = SettingsManager.get().fov;
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05f, 1000.0f);
        proj.m11(proj.m11() * -1); // Vulkan Y-Flip

        Matrix4f mvpCameraRelative = new Matrix4f(proj).mul(viewRotOnly);

        RenderPacket packet = new RenderPacket();
        packet.mvp = mvpCameraRelative;
        packet.proj = proj;
        packet.view = viewRotOnly;
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);
        packet.cameraPos = camera.getPosition();

        boolean isIsoFrame = (isTakingIsometric && isoFramesToWait == -1);

        CullingUtils.buildVisibleLists(world.getChunkManager(), mvpCameraRelative, packet, isIsoFrame,
                packet.cameraPos);
        lastVisibleChunkCount = packet.opaqueMeshes.size() + packet.waterMeshes.size();

        packet.cloudMesh = this.cloudMesh;
        packet.cloudOffset = world.getCloudSystem().getRenderOffset(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z);
        packet.cloudGridSize = world.getCloudSystem().getTotalSize();
        packet.starMesh = this.starMesh;
        packet.starAlpha = skyManager.getStarAlpha();
        packet.timeOfDay = skyManager.getTimeOfDay();
        packet.celestialMesh = this.celestialMesh;

        packet.isUnderwater = interaction.getPlayer().isHeadInWater();
        packet.cameraPos = camera.getPosition();
        packet.renderDistance = SettingsManager.get().renderDistance * 16.0f;

        packet.blockUITexture = blockUITexture;
        packet.uiCombinedMesh = uiRenderer.getCombinedMesh();
        packet.uiDrawCalls = uiRenderer.getDrawCalls();

        packet.uiTexture = uiTexture;
        packet.itemTexture = itemTexture;
        packet.itemTextureArray = itemTextureArray;
        packet.fontTexture = fontTexture;
        packet.worldTexture = worldTexture;

        Vector3i selectedBlockPos = interaction.getSelectedBlockPos();
        packet.selectedBlockPos = selectedBlockPos;

        if (selectedBlockPos != null) {
            byte selectedBlockId = world.getBlockAt(selectedBlockPos);
            Block block = BlockRegistry.get(selectedBlockId);
            BlockState state = world.getBlockState(selectedBlockPos);

            // HIGHLIGHT MESH UPDATE
            if (dynamicHighlightMesh == null) {
                dynamicHighlightMesh = graphicsFactory.createMesh(block.getHighlightVertices(state),
                        block.getHighlightIndices(state));
            } else {
                dynamicHighlightMesh.updateMesh(block.getHighlightVertices(state), block.getHighlightIndices(state));
            }
            packet.highlightMesh = dynamicHighlightMesh;

        } else {
            packet.highlightMesh = highlightMesh;
        }

        // MULTI-BLOCK CRACKING OVERLAY
        java.util.Map<Vector3i, Float> cracks = interaction.getAllMiningProgresses();
        if (!cracks.isEmpty()) {
            updateCrackingMesh(world, cracks);
            packet.overlayMesh = dynamicOverlayMesh;
        } else {
            if (dynamicOverlayMesh != null) {
                dynamicOverlayMesh.cleanup();
                dynamicOverlayMesh = null;
            }
            packet.overlayMesh = null;
        }

        Vector3f skyColor = skyManager.getCurrentSkyColor();
        packet.sunDirection = skyManager.getSunDirection();
        packet.globalLight = skyManager.getGlobalLightIntensity();
        packet.skyR = skyColor.x;
        packet.skyG = skyColor.y;
        packet.skyB = skyColor.z;

        int pX = (int) Math.floor(packet.cameraPos.x);
        int pY = (int) Math.floor(packet.cameraPos.y);
        int pZ = (int) Math.floor(packet.cameraPos.z);
        de.delautrer.game.world.Chunk pChunk = world.getChunkManager().getChunkAtBlock(pX, pY, pZ);
        if (pChunk != null) {
            packet.playerSkyLight = pChunk.getSkyLight(pX & 15, pY, pZ & 15) / 15.0f;
            packet.playerBlockLight = pChunk.getBlockLight(pX & 15, pY, pZ & 15) / 15.0f;
        } else {
            packet.playerSkyLight = 1.0f;
            packet.playerBlockLight = 0.0f;
        }

        packet.hideUI = hideUI;

        // --- FIRST PERSON HAND ---
        if (interaction.getPlayer().getGameMode() == de.delautrer.game.entity.player.GameMode.SPECTATOR) {
            packet.firstPersonMesh = null;
            packet.isEmptyHand = false;
            packet.firstPersonIsItem = false;
            packet.swingProgress = 0.0f;
        } else {
            de.delautrer.game.items.ItemStack handStack = interaction.getPlayer().getInventory().getStack(interaction.getPlayer().getInventory().getSelectedSlot());
            de.delautrer.game.items.Item currentItem = (handStack != null) ? handStack.type : null;
            
            if (currentItem != lastFirstPersonItem || firstPersonMesh == null) {
                if (firstPersonMesh != null) firstPersonMesh.cleanup();
                
                if (currentItem == null) {
                    // Empty hand -> Render block hand (4x12x4 pixels -> 0.25 x 0.75 x 0.25)
                    MeshData md = new MeshData(new float[] {
                        // Front (Z = 0)
                        0.0f, 0.0f, 0.0f, 1.0f, 0.8f, 0.6f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.0f, 1.0f, 0.8f, 0.6f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.75f, 0.0f, 1.0f, 0.8f, 0.6f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.0f, 0.0f, 1.0f, 0.8f, 0.6f, 1.0f, 0, 0, 1, 1, 1,
                        // Back (Z = 0.25)
                        0.25f, 0.0f, 0.25f, 0.8f, 0.6f, 0.4f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.75f, 0.25f, 0.8f, 0.6f, 0.4f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.25f, 0.8f, 0.6f, 0.4f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.0f, 0.25f, 0.8f, 0.6f, 0.4f, 1.0f, 0, 0, 1, 1, 1,
                        // Top (Y = 0.75)
                        0.25f, 0.75f, 0.25f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.75f, 0.0f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.0f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.25f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        // Right (X = 0.25)
                        0.25f, 0.0f, 0.0f, 0.7f, 0.5f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.75f, 0.0f, 0.7f, 0.5f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.75f, 0.25f, 0.7f, 0.5f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.0f, 0.25f, 0.7f, 0.5f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        // Bottom (Y = 0.0)
                        0.25f, 0.0f, 0.0f, 0.5f, 0.4f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.25f, 0.0f, 0.25f, 0.5f, 0.4f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.0f, 0.25f, 0.5f, 0.4f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.0f, 0.0f, 0.5f, 0.4f, 0.3f, 1.0f, 0, 0, 1, 1, 1,
                        // Left (X = 0.0)
                        0.0f, 0.0f, 0.25f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.25f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.75f, 0.0f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1,
                        0.0f, 0.0f, 0.0f, 0.9f, 0.7f, 0.5f, 1.0f, 0, 0, 1, 1, 1
                    }, new int[] {
                        0, 1, 2, 2, 3, 0,
                        4, 5, 6, 6, 7, 4,
                        8, 9, 10, 10, 11, 8,
                        12, 13, 14, 14, 15, 12,
                        16, 17, 18, 18, 19, 16,
                        20, 21, 22, 22, 23, 20
                    });
                    firstPersonMesh = graphicsFactory.createMesh(md);
                    packet.firstPersonIsItem = false;
                } else if (currentItem instanceof de.delautrer.game.items.BlockItem) {
                    de.delautrer.game.items.BlockItem blockItem = (de.delautrer.game.items.BlockItem) currentItem;
                    
                    boolean renderAsItem = currentItem.isRenderAsItem();
    
                    if (renderAsItem) {
                        de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = currentItem.getIconRegion();
                        if (reg != null) {
                            MeshData md = de.delautrer.engine.graphics.utils.ItemMeshGenerator.generateFromTexture(reg, itemAtlas);
                            firstPersonMesh = graphicsFactory.createMesh(md);
                        }
                    } else {
                        if (blockItem.block instanceof de.delautrer.game.blocks.CubeBlock cubeBlock) {
                            MeshData md = de.delautrer.engine.graphics.utils.ItemMeshGenerator.generateBlockMesh(cubeBlock);
                            firstPersonMesh = graphicsFactory.createMesh(md);
                        } else {
                            de.delautrer.game.blocks.models.BlockModelData model = blockItem.block.getModel();
                            if (model != null) {
                                MeshData md = de.delautrer.engine.graphics.utils.ItemMeshGenerator.generateBlockMesh(model);
                                firstPersonMesh = graphicsFactory.createMesh(md);
                            }
                        }
                    }
                } else {
                    de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = currentItem.getIconRegion();
                    if (reg != null) {
                        MeshData md = de.delautrer.engine.graphics.utils.ItemMeshGenerator.generateFromTexture(reg, itemAtlas);
                        firstPersonMesh = graphicsFactory.createMesh(md);
                    }
                }
                lastFirstPersonItem = currentItem;
            }
            
            packet.firstPersonMesh = firstPersonMesh;
            packet.isEmptyHand = (currentItem == null);
            
            packet.firstPersonIsItem = currentItem != null && currentItem.isRenderAsItem();
            packet.swingProgress = interaction.getSwingProgress();
        }

        if (isIsoFrame) {
            packet.hideUI = true;
            //packet.clipY = (float) Math.min(-10f, interaction.getPlayer().getEyePosition().y - 15.0f);
            packet.clipY = -22;
            packet.renderDistance = 10000.0f;
            float zoom = 80.0f;

            packet.proj = new org.joml.Matrix4f().ortho(-zoom * aspect, zoom * aspect, -zoom, zoom, -2000.0f, 2000.0f);
            packet.proj.m11(packet.proj.m11() * -1.0f);

            org.joml.Vector3d target = interaction.getPlayer().position;
            org.joml.Vector3d eye = new org.joml.Vector3d(target.x + 100.0, target.y + 60.0, target.z + 100.0);

            packet.view = new org.joml.Matrix4f().lookAt(new Vector3f((float) eye.x, (float) eye.y, (float) eye.z),
                    new Vector3f((float) target.x, (float) target.y, (float) target.z), new org.joml.Vector3f(0, 1, 0));

            Matrix4f isoViewRotOnly = new Matrix4f(packet.view).setTranslation(0, 0, 0);
            packet.mvp = new org.joml.Matrix4f(packet.proj).mul(isoViewRotOnly);
            packet.cameraPos = eye;
        } else {
            packet.clipY = -999.0f;
            packet.renderDistance = SettingsManager.get().renderDistance * 16.0f;
            packet.proj = proj;

            packet.view = viewRotOnly;
            packet.mvp = mvpCameraRelative;
        }

        return renderer.render(packet);
    }

    // --- NEU: Diese Methode baut keine neuen Meshes mehr, sondern updatet unser
    // gespeichertes Mesh ---
    private void updateCrackingMesh(World world, java.util.Map<Vector3i, Float> activeCracks) {
        int totalBoxes = 0;
        for (java.util.Map.Entry<Vector3i, Float> entry : activeCracks.entrySet()) {
            Vector3i pos = entry.getKey();
            byte id = world.getBlockAt(pos);
            if (id != 0) {
                de.delautrer.game.blocks.Block block = de.delautrer.game.blocks.BlockRegistry.get(id);
                de.delautrer.game.blocks.state.BlockState state = world.getBlockState(pos);
                totalBoxes += block.getHighlightBoxes(state).size();
            }
        }
        
        if (totalBoxes == 0) {
            if (dynamicOverlayMesh != null) {
                dynamicOverlayMesh.cleanup();
                dynamicOverlayMesh = null;
            }
            return;
        }

        float[] verts = new float[totalBoxes * 24 * 12];
        int[] inds = new int[totalBoxes * 36];

        int vOffset = 0;
        int iOffset = 0;
        int indexOffset = 0;

        for (java.util.Map.Entry<Vector3i, Float> entry : activeCracks.entrySet()) {
            Vector3i pos = entry.getKey();
            byte id = world.getBlockAt(pos);
            if (id == 0) continue;
            de.delautrer.game.blocks.Block block = de.delautrer.game.blocks.BlockRegistry.get(id);
            de.delautrer.game.blocks.state.BlockState state = world.getBlockState(pos);
            java.util.List<de.delautrer.engine.physics.AABB> boxes = block.getHighlightBoxes(state);

            float progress = entry.getValue();

            int stage = (int) Math.min(9, Math.floor(progress * 10.0f));
            String textureName = "destroy/destroy_stage_" + stage;
            if (!blockAtlas.regions.containsKey(textureName)) continue;
            float layer = blockAtlas.regions.get(textureName).layer;

            float x = pos.x, y = pos.y, z = pos.z;
            float eExpand = -0.001f;
            float sExpand = 0.001f;

            float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
            float sl = 1.0f, bl = 1.0f;

            for (de.delautrer.engine.physics.AABB box : boxes) {
                float minX = x + box.min.x + eExpand;
                float minY = y + box.min.y + eExpand;
                float minZ = z + box.min.z + eExpand;
                float maxX = x + box.max.x + sExpand;
                float maxY = y + box.max.y + sExpand;
                float maxZ = z + box.max.z + sExpand;

                float uMinX = box.min.x;
                float uMaxX = box.max.x;
                float uMinZ = box.min.z;
                float uMaxZ = box.max.z;

                float vMinY = 1.0f - box.max.y;
                float vMaxY = 1.0f - box.min.y;
                
                float vMinZ = 1.0f - box.max.z;
                float vMaxZ = 1.0f - box.min.z;

                float[] localVerts = {
                        // Front (-Z)
                        minX, minY, minZ, r, g, b, a, uMinX, vMaxY, layer, sl, bl,
                        maxX, minY, minZ, r, g, b, a, uMaxX, vMaxY, layer, sl, bl,
                        maxX, maxY, minZ, r, g, b, a, uMaxX, vMinY, layer, sl, bl,
                        minX, maxY, minZ, r, g, b, a, uMinX, vMinY, layer, sl, bl,
                        // Back (+Z)
                        minX, minY, maxZ, r, g, b, a, 1.0f - uMinX, vMaxY, layer, sl, bl,
                        maxX, minY, maxZ, r, g, b, a, 1.0f - uMaxX, vMaxY, layer, sl, bl,
                        maxX, maxY, maxZ, r, g, b, a, 1.0f - uMaxX, vMinY, layer, sl, bl,
                        minX, maxY, maxZ, r, g, b, a, 1.0f - uMinX, vMinY, layer, sl, bl,
                        // Left (-X)
                        minX, minY, minZ, r, g, b, a, 1.0f - uMinZ, vMaxY, layer, sl, bl,
                        minX, minY, maxZ, r, g, b, a, 1.0f - uMaxZ, vMaxY, layer, sl, bl,
                        minX, maxY, maxZ, r, g, b, a, 1.0f - uMaxZ, vMinY, layer, sl, bl,
                        minX, maxY, minZ, r, g, b, a, 1.0f - uMinZ, vMinY, layer, sl, bl,
                        // Right (+X)
                        maxX, minY, minZ, r, g, b, a, uMinZ, vMaxY, layer, sl, bl,
                        maxX, minY, maxZ, r, g, b, a, uMaxZ, vMaxY, layer, sl, bl,
                        maxX, maxY, maxZ, r, g, b, a, uMaxZ, vMinY, layer, sl, bl,
                        maxX, maxY, minZ, r, g, b, a, uMinZ, vMinY, layer, sl, bl,
                        // Top (+Y)
                        minX, maxY, minZ, r, g, b, a, uMinX, vMaxZ, layer, sl, bl,
                        maxX, maxY, minZ, r, g, b, a, uMaxX, vMaxZ, layer, sl, bl,
                        maxX, maxY, maxZ, r, g, b, a, uMaxX, vMinZ, layer, sl, bl,
                        minX, maxY, maxZ, r, g, b, a, uMinX, vMinZ, layer, sl, bl,
                        // Bottom (-Y)
                        minX, minY, minZ, r, g, b, a, uMinX, uMinZ, layer, sl, bl,
                        maxX, minY, minZ, r, g, b, a, uMaxX, uMinZ, layer, sl, bl,
                        maxX, minY, maxZ, r, g, b, a, uMaxX, uMaxZ, layer, sl, bl,
                        minX, minY, maxZ, r, g, b, a, uMinX, uMaxZ, layer, sl, bl,
                };
    
                int[] localInds = {
                        2, 1, 0, 0, 3, 2, // Front
                        6, 7, 4, 4, 5, 6, // Back
                        10, 11, 8, 8, 9, 10, // Left
                        14, 13, 12, 12, 15, 14, // Right
                        18, 17, 16, 16, 19, 18, // Top
                        22, 23, 20, 20, 21, 22 // Bottom
                };
    
                System.arraycopy(localVerts, 0, verts, vOffset, localVerts.length);
                vOffset += localVerts.length;
    
                for (int i = 0; i < localInds.length; i++) {
                    inds[iOffset++] = localInds[i] + indexOffset;
                }
                indexOffset += 24;
            }
        }

        if (vOffset == 0) {
            if (dynamicOverlayMesh != null) {
                dynamicOverlayMesh.cleanup();
                dynamicOverlayMesh = null;
            }
            return;
        }

        float[] finalVerts = new float[vOffset];
        System.arraycopy(verts, 0, finalVerts, 0, vOffset);
        int[] finalInds = new int[iOffset];
        System.arraycopy(inds, 0, finalInds, 0, iOffset);

        if (dynamicOverlayMesh == null) {
            dynamicOverlayMesh = graphicsFactory.createMesh(finalVerts, finalInds);
        } else {
            dynamicOverlayMesh.updateMesh(finalVerts, finalInds);
        }
    }

    public void recreate(PlayerInteraction interaction, InputManager input, DebugOverlay debugOverlay,
            MenuScreen pauseScreen, ChatOverlay chatOverlay) {
        renderer.recreate(window);
        this.graphicsFactory = new VulkanGraphicsFactory(vulkanContext, renderer.getCommandBuffers(),
                renderer.getGraphicsLayout(), renderer.getUiLayout());
        Engine.get().setGraphicsFactory(this.graphicsFactory);
        this.uiRenderer.setGraphicsFactory(this.graphicsFactory);
        rebuildUI(interaction, input, debugOverlay, pauseScreen, chatOverlay);
    }

    public int getLastVisibleChunkCount() {
        return lastVisibleChunkCount;
    }

    public void cleanup() {
        if (celestialMesh != null)
            celestialMesh.cleanup();
        if (starMesh != null)
            starMesh.cleanup();
        if (cloudMesh != null)
            cloudMesh.cleanup();
        if (worldTexture != null)
            worldTexture.cleanup();
        if (blockUITexture != null)
            blockUITexture.cleanup();
        if (uiTexture != null)
            uiTexture.cleanup();
        if (itemTexture != null)
            itemTexture.cleanup();
        if (itemTextureArray != null)
            itemTextureArray.cleanup();
        if (fontTexture != null)
            fontTexture.cleanup();
        if (uiRenderer != null)
            uiRenderer.cleanup();
        if (highlightMesh != null)
            highlightMesh.cleanup();
        if (dynamicHighlightMesh != null)
            dynamicHighlightMesh.cleanup();
        if (dynamicOverlayMesh != null)
            dynamicOverlayMesh.cleanup();
        if (font != null)
            font.cleanup();
        if (renderer != null)
            renderer.cleanup();
    }

    public IFont getFont() {
        return font;
    }

    public IGraphicsFactory getGraphicsFactory() {
        return graphicsFactory;
    }

    public void requestScreenshot(String path) {
        if (this.renderer != null) {
            this.renderer.requestScreenshot(path);
        }
    }

    public void requestThumbnail(String path) {
        if (this.renderer != null) {
            this.renderer.requestThumbnail(path);
        }
    }
}
