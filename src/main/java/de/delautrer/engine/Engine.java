package de.delautrer.engine;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.MasterRenderer;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Camera;
import de.delautrer.engine.player.Player;
import de.delautrer.engine.window.Window;
import de.delautrer.game.events.DebugToggleEvent;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.Environment;
import de.delautrer.game.world.World;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;

public class Engine {

    private Window window;
    private VulkanContext vulkanContext;
    private MasterRenderer masterRenderer;

    private World world;
    private Camera camera;
    private Player player;
    private PlayerInteraction interaction;

    private EventBus eventBus;
    private InputManager inputManager;
    private Environment environment;
    private DebugOverlay debugOverlay;

    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;

    // Unser einziges UI-Flag!
    private boolean uiNeedsRebuild = true;

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
        camera = new Camera();

        // Der MasterRenderer übernimmt die gesamte Vulkan-Last
        masterRenderer = new MasterRenderer(vulkanContext, window);
        debugOverlay = new DebugOverlay();

        // Debug-Overlay konfigurieren
        debugOverlay.addLine("Version", () -> "0.1-Alpha");
        debugOverlay.addLine("FPS", () -> {
            if (deltaTime <= 0) return "0";
            return String.format("%d", (int)(1.0f / deltaTime));
        });
        debugOverlay.addLine("Chunks (Geladen/Sichtbar)", () ->
                world.getChunkManager().getMeshes().size() + " / " + masterRenderer.getLastVisibleChunkCount()
        );
        debugOverlay.addLine("Player XYZ", () -> String.format("%.2f / %.2f / %.2f",
                player.position.x, player.position.y, player.position.z));
        debugOverlay.addLine("Player Yaw/Pitch", () -> String.format("%.1f / %.1f",
                camera.getYaw(), camera.getPitch()));
        debugOverlay.addLine("Chunk Pos", () -> String.format("%d / %d",
                (int)Math.floor(player.position.x / Chunk.SIZE),
                (int)Math.floor(player.position.z / Chunk.SIZE)));
        debugOverlay.addLine("Daytime", () -> String.format("%.2f", environment.getTimeOfDay()));

        interaction = new PlayerInteraction(world, camera, player, vulkanContext, eventBus);

        // --- DIE NEUEN EVENT-SUBSCRIPTIONS ---
        eventBus.subscribe(InventoryToggleEvent.class, event -> {
            uiNeedsRebuild = true;
            if (event.isOpen) window.enableCursor();
            else {
                window.disableCursor();
                camera.resetMouseTracking();
            }
        });

        eventBus.subscribe(HotbarSlotChangeEvent.class, event -> {
            uiNeedsRebuild = true;
        });

        eventBus.subscribe(DebugToggleEvent.class, event -> {
            uiNeedsRebuild = true;
        });
    }

    private void loop() {
        while (!window.shouldClose()) {
            float currentFrameTime = (float) GLFW.glfwGetTime();
            deltaTime = currentFrameTime - lastFrame;
            lastFrame = currentFrameTime;

            window.pollEvents();

            if (inputManager.isActionJustPressed("DEBUG_MENU")) {
                debugOverlay.toggle();
                eventBus.publish(new DebugToggleEvent(debugOverlay.isVisible()));
            }

            environment.update(deltaTime);
            world.update(inputManager, camera.getFront(), interaction.getInventory().isOpen(), deltaTime);

            if (!interaction.getInventory().isOpen()) {
                camera.update(window.getHandle(), deltaTime, player.getEyePosition());
            } else {
                camera.setPosition(player.getEyePosition());
                uiNeedsRebuild = true; // Für das Item-Hovering
            }

            if (debugOverlay.isVisible()) {
                uiNeedsRebuild = true;
            }

            interaction.update(inputManager, deltaTime);

            // --- RENDER LOGIK ---
            if (window.isFramebufferResized()) {
                window.setFramebufferResized(false);
                masterRenderer.recreate(interaction, inputManager, debugOverlay);
                uiNeedsRebuild = true;
            } else {
                // Nur neu bauen, wenn jemand das Event gefeuert hat!
                if (uiNeedsRebuild) {
                    masterRenderer.rebuildUI(interaction, inputManager, debugOverlay);
                    uiNeedsRebuild = false;
                }

                // An MasterRenderer delegieren
                if (!masterRenderer.drawFrame(camera, world, environment, interaction)) {
                    masterRenderer.recreate(interaction, inputManager, debugOverlay);
                    uiNeedsRebuild = true;
                }
            }

            interaction.update(inputManager, deltaTime);
        }
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
    }

    private void cleanup() {
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());

        if (masterRenderer != null) masterRenderer.cleanup();
        if (world != null && world.getChunkManager() != null) world.getChunkManager().cleanup();
        if (vulkanContext != null) vulkanContext.cleanup();
        if (window != null) window.cleanup();
    }
}