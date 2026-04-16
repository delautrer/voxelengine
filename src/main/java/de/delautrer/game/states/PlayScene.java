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
import de.delautrer.game.ui.gui.MenuScreen;
import de.delautrer.game.ui.gui.PauseScreen;
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

    private PauseScreen pauseScreen;
    private boolean isPaused = false;
    private boolean isSavingAndQuitting = false;
    private int saveWaitFrames = 18;

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

        pauseScreen = new PauseScreen(this);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        pauseScreen.setFont(masterRenderer.getFont());

        localPlayer = new LocalPlayer(new Vector3f(8.0f, 20.0f, 8.0f));
        engine.getWindow().disableCursor();
        localPlayer.getCamera().resetMouseTracking();

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

        debugOverlay.addLine("Target Block", () -> {
            org.joml.Vector3i target = localPlayer.getInteraction().getSelectedBlockPos();
            if (target != null) {
                byte blockId = world.getBlockAt(target);
                de.delautrer.game.blocks.state.BlockState state = world.getBlockState(target.x, target.y, target.z);
                return String.format("[%d %d %d] ID: %d, State: %d",
                        target.x, target.y, target.z, blockId, state.getStateId());
            }
            return "-";
        });

        debugOverlay.addLine("Target Light", () -> {
            org.joml.Vector3i target = localPlayer.getInteraction().getSelectedBlockPos();
            if (target != null) {
                Chunk c = world.getChunkManager().getChunkAtBlock(target.x, target.y, target.z);
                if (c != null) {
                    int lx = Math.floorMod(target.x, Chunk.SIZE);
                    int lz = Math.floorMod(target.z, Chunk.SIZE);
                    int sky = c.getSkyLight(lx, target.y, lz);
                    int block = c.getBlockLight(lx, target.y, lz);
                    return String.format("Sky: %d, Block: %d", sky, block);
                }
            }
            return "-";
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
        // 1. Speichern & Beenden Sequenz
        if (isSavingAndQuitting) {
            saveWaitFrames--;
            if (saveWaitFrames <= 0) {
                // Wirft uns ins Hauptmenü. Der SceneManager ruft automatisch
                // PlayScene.cleanup() auf, was die Welt sicher auf der Festplatte speichert!
                engine.getSceneManager().changeScene(new MainMenuScene(engine));
            }
            uiNeedsRebuild = true;
            return; // Nichts anderes mehr updaten
        }

        // 2. Escape-Taste prüft Pause (Vorausgesetzt du hast "PAUSE" in deinem InputManager registriert!)
        if (engine.getInputManager().isActionJustPressed("PAUSE")) {
            if (isPaused) resumeGame();
            else pauseGame();
        }

        // 3. Pausen-Status
        if (isPaused) {
            float uiMouseY = engine.getWindow().getHeight() - engine.getInputManager().getMouseY();
            pauseScreen.handleMenuInput(engine.getInputManager(), engine.getInputManager().getMouseX(), uiMouseY);
            uiNeedsRebuild = true; // Permanent neu bauen für Hover-Effekte
            return; // WICHTIG: Überspringt das World-Update! Das Spiel friert ein.
        }

        // --- NORMALES SPIEL ---
        if (engine.getInputManager().isActionJustPressed("DEBUG_MENU")) {
            debugOverlay.toggle();
            engine.getEventBus().publish(new DebugToggleEvent(debugOverlay.isVisible()));
        }

        world.getEnvironment().update(deltaTime);
        world.update(deltaTime, localPlayer);

        localPlayer.updateLocal(engine.getInputManager(), world.getChunkManager(), deltaTime);
        localPlayer.updateCamera(engine.getWindow().getHandle(), deltaTime);

        if (localPlayer.getInventory().isOpen() || debugOverlay.isVisible()) {
            uiNeedsRebuild = true;
        }

        autosaveTimer += deltaTime;
        if (autosaveTimer >= AUTOSAVE_INTERVAL) {
            world.saveWorld(localPlayer);
            autosaveTimer = 0;
        }
    }

    @Override
    public void render() {
        if (uiNeedsRebuild) {
            MenuScreen activeScreen = (isPaused || isSavingAndQuitting) ? pauseScreen : null;
            masterRenderer.rebuildUI(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen);
            uiNeedsRebuild = false;
        }

        if (!masterRenderer.drawFrame(localPlayer.getCamera(), world, world.getEnvironment(), localPlayer.getInteraction())) {
            uiNeedsRebuild = true;
        }
    }

    @Override
    public void onResize() {
        MenuScreen activeScreen = (isPaused || isSavingAndQuitting) ? pauseScreen : null;
        masterRenderer.recreate(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        uiNeedsRebuild = true;
    }

    @Override
    public void cleanup() {
        if (world != null) world.cleanup(localPlayer);
        if (masterRenderer != null) masterRenderer.cleanup();
    }

    public void pauseGame() {
        isPaused = true;
        engine.getWindow().enableCursor();
        localPlayer.getCamera().resetMouseTracking();
        uiNeedsRebuild = true;
    }

    public void resumeGame() {
        isPaused = false;
        if (!localPlayer.getInventory().isOpen()) {
            engine.getWindow().disableCursor();
        }
        uiNeedsRebuild = true;
    }

    public void saveAndQuit() {
        isSavingAndQuitting = true;
        uiNeedsRebuild = true;
    }
}