package de.delautrer.game.states;

import de.delautrer.engine.MasterRenderer;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.MeshData;
import de.delautrer.engine.states.Scene;
import de.delautrer.engine.Engine;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.DebugToggleEvent;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.world.WorldEventHandler;
import org.joml.Vector3f;

public class PlayScene extends Scene {

    private MasterRenderer masterRenderer;
    private World world;
    private LocalPlayer localPlayer;
    private WorldEventHandler worldEventHandler;
    private DebugOverlay debugOverlay;

    private boolean uiNeedsRebuild = true;

    private float autosaveTimer = 0;
    private final float AUTOSAVE_INTERVAL = 300.0f;

    public PlayScene(Engine engine) {
        super(engine);
    }

    @Override
    public void init() {
        masterRenderer = new MasterRenderer(engine.getVulkanContext(), engine.getWindow());
        debugOverlay = new DebugOverlay();

        localPlayer = new LocalPlayer(new Vector3f(8.0f, 20.0f, 8.0f));
        world = new World(engine.getVulkanContext(), localPlayer, engine.getEventBus(), 1337L);
        worldEventHandler = new WorldEventHandler(world, engine.getVulkanContext(), engine.getEventBus());
        localPlayer.initInteraction(world, engine.getVulkanContext(), engine.getEventBus());

        MeshData cloudData = world.getCloudSystem().generateCloudMesh(world.getSeed());
        masterRenderer.initClouds(cloudData);

        setupDebugOverlay();
        setupEvents();
    }

    private void setupDebugOverlay() {
        debugOverlay.addLine("Version", () -> "0.1-Alpha");
        debugOverlay.addLine("FPS", () -> String.format("%d", engine.getCurrentFps()));
        debugOverlay.addLine("Chunks (Geladen/Sichtbar)", () ->
                world.getChunkManager().getMeshes().size() + " / " + masterRenderer.getLastVisibleChunkCount()
        );
        debugOverlay.addLine("Player XYZ", () -> String.format("%.2f / %.2f / %.2f",
                localPlayer.position.x, localPlayer.position.y, localPlayer.position.z));
        debugOverlay.addLine("Player Yaw/Pitch", () -> String.format("%.1f / %.1f",
                localPlayer.getCamera().getYaw(), localPlayer.getCamera().getPitch()));
        debugOverlay.addLine("Chunk Pos", () -> String.format("%d / %d",
                (int)Math.floor(localPlayer.position.x / Chunk.SIZE),
                (int)Math.floor(localPlayer.position.z / Chunk.SIZE)));

        debugOverlay.addLine("Biome", () -> {
            int px = (int) Math.floor(localPlayer.position.x);
            int pz = (int) Math.floor(localPlayer.position.z);
            Chunk c = world.getChunkManager().getChunkAtBlock(px, (int) Math.max(0, localPlayer.position.y), pz);
            if (c != null) {
                Biome b = c.getBiome(Math.floorMod(px, Chunk.SIZE), Math.floorMod(pz, Chunk.SIZE));
                return b != null ? b.name() : "None";
            }
            return "Unloaded";
        });

        debugOverlay.addLine("Time", () -> {
            float time = world.getEnvironment().getTimeOfDay();
            float displayTime = (time + 12.0f) % 24.0f;
            if (displayTime < 0) displayTime += 24.0f;
            int hours = (int) displayTime;
            int minutes = (int) ((displayTime - hours) * 60);
            return String.format("%02d:%02d", hours, minutes);
        });
    }

    private void setupEvents() {
        EventBus eventBus = engine.getEventBus();
        eventBus.subscribe(InventoryToggleEvent.class, event -> {
            uiNeedsRebuild = true;
            if (event.isOpen) engine.getWindow().enableCursor();
            else {
                engine.getWindow().disableCursor();
                localPlayer.getCamera().resetMouseTracking();
            }
        });
        eventBus.subscribe(HotbarSlotChangeEvent.class, event -> uiNeedsRebuild = true);
        eventBus.subscribe(DebugToggleEvent.class, event -> uiNeedsRebuild = true);
    }

    @Override
    public void update(float deltaTime) {
        if (engine.getInputManager().isActionJustPressed("DEBUG_MENU")) {
            debugOverlay.toggle();
            engine.getEventBus().publish(new DebugToggleEvent(debugOverlay.isVisible()));
        }

        world.getEnvironment().update(deltaTime);
        world.update(deltaTime, localPlayer);

        autosaveTimer += deltaTime;
        if (autosaveTimer >= AUTOSAVE_INTERVAL) {
            System.out.println("Autosave wird ausgeführt...");
            world.saveWorld(localPlayer);
            autosaveTimer = 0;
        }

        localPlayer.updateLocal(engine.getInputManager(), world.getChunkManager(), deltaTime);
        localPlayer.updateCamera(engine.getWindow().getHandle(), deltaTime);

        if (localPlayer.getInventory().isOpen() || debugOverlay.isVisible()) {
            uiNeedsRebuild = true;
        }
    }

    @Override
    public void render() {
        if (uiNeedsRebuild) {
            masterRenderer.rebuildUI(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay);
            uiNeedsRebuild = false;
        }

        if (!masterRenderer.drawFrame(localPlayer.getCamera(), world, world.getEnvironment(), localPlayer.getInteraction())) {
            // Swapchain invalid -> recreate UI on next frame
            uiNeedsRebuild = true;
        }
    }

    @Override
    public void onResize() {
        masterRenderer.recreate(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay);
        uiNeedsRebuild = true;
    }

    @Override
    public void cleanup() {
        if (world != null) world.cleanup(localPlayer);
        if (masterRenderer != null) masterRenderer.cleanup();
    }
}