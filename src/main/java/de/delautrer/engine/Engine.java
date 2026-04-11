package de.delautrer.engine;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.engine.window.Window;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.UIRenderer;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.Environment;
import de.delautrer.game.world.World;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;

public class Engine {

    private Window window;
    private VulkanContext vulkanContext;
    private VulkanRenderer renderer;

    private World world;
    private Camera camera;
    private Player player;
    private PlayerInteraction interaction;

    private EventBus eventBus;
    private InputManager inputManager;
    private Environment environment;

    private VulkanTextureArray worldTexture;
    private VulkanTexture guiTexture;
    private VulkanTexture fontTexture;

    private VulkanMesh highlightMesh;
    private UIRenderer uiRenderer;

    private DebugOverlay debugOverlay;
    private VulkanFont font;
    private int lastVisibleChunkCount = 0;

    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;
    private int lastSelectedSlot = -1;
    private boolean lastInventoryState = false;

    // NEU: Damit die Engine merkt, wenn das F3-Menü geschlossen wird!
    private boolean lastDebugState = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window(1280, 720, "Voxel Engine");
        window.disableCursor();
        vulkanContext = new VulkanContext(window);

        eventBus = new EventBus();
        inputManager = new InputManager(window.getHandle());
        environment = new Environment();

        player = new Player();
        world = new World(vulkanContext, player);
        renderer = new VulkanRenderer(vulkanContext, window);

        worldTexture = new VulkanTextureArray(vulkanContext, renderer.getCommandBuffers(), renderer.getGraphicsLayout(), "src/main/resources/texture.png");
        guiTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), "src/main/resources/gui.png");

        font = new VulkanFont("src/main/resources/MinecraftRegular-Bmg3.otf", 24.0f);
        if (font.getRgbaPixels() != null) {
            fontTexture = new VulkanTexture(vulkanContext, renderer.getCommandBuffers(), renderer.getUiLayout(), font.getRgbaPixels(), font.BITMAP_SIZE, font.BITMAP_SIZE);
        }

        highlightMesh = new VulkanMesh(vulkanContext, Chunk.getHighlightVertices(), Chunk.getHighlightIndices());
        uiRenderer = new UIRenderer(vulkanContext, renderer.getWidth(), renderer.getHeight());
        camera = new Camera();

        debugOverlay = new DebugOverlay();

        debugOverlay.addLine("Version", () -> "0.1-Alpha");
        debugOverlay.addLine("FPS", () -> {
            if (deltaTime <= 0) return "0";
            return String.format("%d", (int)(1.0f / deltaTime));
        });
        debugOverlay.addLine("Chunks (Geladen/Sichtbar)", () ->
                world.getChunkManager().getMeshes().size() + " / " + lastVisibleChunkCount
        );
        debugOverlay.addLine("Player XYZ", () -> String.format("%.2f / %.2f / %.2f",
                player.position.x, player.position.y, player.position.z));
        debugOverlay.addLine("Player Yaw/Pitch", () -> String.format("%.1f / %.1f",
                camera.getYaw(), camera.getPitch()));
        debugOverlay.addLine("Chunk Pos", () -> String.format("%d / %d",
                (int)Math.floor(player.position.x / Chunk.SIZE),
                (int)Math.floor(player.position.z / Chunk.SIZE)));
        debugOverlay.addLine("Daytime", () -> String.format("%.2f", environment.getTimeOfDay()));

        eventBus.subscribe(InventoryToggleEvent.class, event -> {
            if (event.isOpen) window.enableCursor();
            else {
                window.disableCursor();
                camera.resetMouseTracking();
            }
        });

        interaction = new PlayerInteraction(world, camera, player, vulkanContext, eventBus);
    }

    private void loop() {
        while (!window.shouldClose()) {
            float currentFrameTime = (float) GLFW.glfwGetTime();
            deltaTime = currentFrameTime - lastFrame;
            lastFrame = currentFrameTime;

            window.pollEvents();

            if (inputManager.isActionJustPressed("DEBUG_MENU")) {
                debugOverlay.toggle();
            }

            environment.update(deltaTime);
            world.update(inputManager, camera.getFront(), deltaTime);

            if (!interaction.getInventory().isOpen()) {
                camera.update(window.getHandle(), deltaTime, player.getEyePosition());
            } else {
                camera.setPosition(player.getEyePosition());
            }

            interaction.update(inputManager);

            if (window.isFramebufferResized()) {
                window.setFramebufferResized(false);
                recreateRenderer();
            } else {
                if (!drawFrame()) {
                    recreateRenderer();
                }
            }

            inputManager.update();
        }
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
    }

    private void recreateRenderer() {
        renderer.recreate(window);
        uiRenderer.rebuildMesh(renderer.getWidth(), renderer.getHeight(), interaction.getInventory(), inputManager.getMouseX(), inputManager.getMouseY(), interaction.getHoveredSlot(), debugOverlay, font);
    }

    private boolean drawFrame() {
        boolean invOpen = interaction.getInventory().isOpen();
        int currentSlot = interaction.getInventory().getSelectedSlot();
        boolean currentDebugState = debugOverlay.isVisible();

        // HIER IST DER LOGIK-FIX: currentDebugState != lastDebugState prüft, ob das Menü gerade geschlossen wurde!
        if (invOpen || currentSlot != lastSelectedSlot || invOpen != lastInventoryState || currentDebugState || currentDebugState != lastDebugState) {
            uiRenderer.rebuildMesh(renderer.getWidth(), renderer.getHeight(), interaction.getInventory(), inputManager.getMouseX(), inputManager.getMouseY(), interaction.getHoveredSlot(), debugOverlay, font);
            lastSelectedSlot = currentSlot;
            lastInventoryState = invOpen;
            lastDebugState = currentDebugState; // Status für den nächsten Frame merken
        }

        float aspect = (float) renderer.getWidth() / (float) renderer.getHeight();
        Matrix4f view = camera.getViewMatrix();
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0f), aspect, 0.1f, 1000.0f);
        proj.m11(proj.m11() * -1);
        Matrix4f mvp = new Matrix4f(proj).mul(view);

        RenderPacket packet = new RenderPacket();
        packet.mvp = mvp;
        packet.proj = proj;
        packet.view = view;
        packet.ortho = new Matrix4f().ortho(0.0f, renderer.getWidth(), renderer.getHeight(), 0.0f, -1.0f, 1.0f);

        java.util.List<de.delautrer.engine.graphics.VulkanMesh> visible = world.getVisibleMeshes(mvp);
        packet.visibleMeshes = visible;
        lastVisibleChunkCount = visible.size();

        packet.highlightMesh = highlightMesh;

        packet.uiMesh = uiRenderer.getGuiMesh();
        packet.guiTexture = guiTexture;
        packet.textMesh = uiRenderer.getTextMesh();
        packet.fontTexture = fontTexture;

        packet.worldTexture = worldTexture;
        packet.selectedBlockPos = interaction.getSelectedBlockPos();

        packet.globalLight = environment.getGlobalLight();
        packet.skyR = environment.getSkyR();
        packet.skyG = environment.getSkyG();
        packet.skyB = environment.getSkyB();

        return renderer.render(packet);
    }

    private void cleanup() {
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());

        if (worldTexture != null) worldTexture.cleanup();
        if (guiTexture != null) guiTexture.cleanup();
        if (fontTexture != null) fontTexture.cleanup();

        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();
        if (font != null) font.cleanup();

        if (renderer != null) renderer.cleanup();
        if (world != null && world.getChunkManager() != null) world.getChunkManager().cleanup();

        if (vulkanContext != null) vulkanContext.cleanup();
        if (window != null) window.cleanup();
    }
}