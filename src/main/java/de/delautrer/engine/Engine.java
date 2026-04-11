package de.delautrer.engine;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.engine.window.Window;
import de.delautrer.game.events.InventoryToggleEvent; // NEUES EVENT
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.UIRenderer;
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
    private VulkanMesh highlightMesh;
    private UIRenderer uiRenderer;

    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;
    private int lastSelectedSlot = -1;
    private boolean lastInventoryState = false;

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

        highlightMesh = new VulkanMesh(vulkanContext, Chunk.getHighlightVertices(), Chunk.getHighlightIndices());
        uiRenderer = new UIRenderer(vulkanContext, renderer.getWidth(), renderer.getHeight());
        camera = new Camera();

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
        uiRenderer.rebuildMesh(renderer.getWidth(), renderer.getHeight(), interaction.getInventory(), inputManager.getMouseX(), inputManager.getMouseY(), interaction.getHoveredSlot());
    }

    private boolean drawFrame() {
        boolean invOpen = interaction.getInventory().isOpen();
        int currentSlot = interaction.getInventory().getSelectedSlot();

        if (invOpen || currentSlot != lastSelectedSlot || invOpen != lastInventoryState) {
            uiRenderer.rebuildMesh(renderer.getWidth(), renderer.getHeight(), interaction.getInventory(), inputManager.getMouseX(), inputManager.getMouseY(), interaction.getHoveredSlot());
            lastSelectedSlot = currentSlot;
            lastInventoryState = invOpen;
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

        packet.visibleMeshes = world.getVisibleMeshes(mvp);
        packet.highlightMesh = highlightMesh;
        packet.uiMesh = uiRenderer.getMesh();

        packet.worldTexture = worldTexture;
        packet.guiTexture = guiTexture;

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
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();

        if (renderer != null) renderer.cleanup();
        if (world != null && world.getChunkManager() != null) world.getChunkManager().cleanup();

        if (vulkanContext != null) vulkanContext.cleanup();
        if (window != null) window.cleanup();
    }
}