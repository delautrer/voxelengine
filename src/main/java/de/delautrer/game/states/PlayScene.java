package de.delautrer.game.states;

import de.delautrer.engine.MasterRenderer;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.engine.graphics.MeshData;
import de.delautrer.engine.states.Scene;
import de.delautrer.engine.Engine;
import de.delautrer.game.commands.CommandManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.DebugToggleEvent;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryToggleEvent;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.gui.screens.ChatScreen;
import de.delautrer.game.ui.gui.screens.LoadingScreen;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.ui.gui.screens.PauseScreen;
import de.delautrer.game.world.*;
import org.joml.Vector3f;

public class PlayScene extends Scene {

    private MasterRenderer masterRenderer;
    private World world;
    private LocalPlayer localPlayer;
    private WorldEventHandler worldEventHandler;
    private DebugOverlay debugOverlay;
    private ChatOverlay chatOverlay;
    private EventBus eventBus;

    private PauseScreen pauseScreen;
    private LoadingScreen loadingScreen;
    private ChatScreen chatScreen;

    private boolean isChatOpen = false;
    private boolean isPaused = false;
    private boolean isSavingAndQuitting = false;
    private int saveWaitFrames = 2;

    private final String worldName;
    private final String worldSave;
    private long seed;
    private final boolean isNewWorld;

    private boolean uiNeedsRebuild = true;
    private boolean wasLoading = true;

    private float autosaveTimer = 0;
    private final float AUTOSAVE_INTERVAL = 300.0f;

    // Gespeicherte Listener-Variablen fürs Cleanup
    private EventListener<InventoryToggleEvent> inventoryToggleListener;
    private EventListener<HotbarSlotChangeEvent> hotbarSlotChangeListener;
    private EventListener<DebugToggleEvent> debugToggleListener;

    public PlayScene(Engine engine, String worldName, long seed) {
        super(engine);
        this.worldName = worldName;
        this.worldSave = WorldStorageManager.getUniqueValidFolderName(worldName);
        this.seed = seed;
        this.isNewWorld = true;
    }

    public PlayScene(Engine engine, String worldName, String worldSave) {
        super(engine);
        this.worldName = worldName;
        this.worldSave = worldSave;
        this.isNewWorld = false;
        this.seed = 0;
    }

    @Override
    public void init() {
        eventBus = new EventBus();
        masterRenderer = new MasterRenderer(engine.getVulkanContext(), engine.getWindow());
        debugOverlay = new DebugOverlay();
        chatOverlay = new ChatOverlay(eventBus);

        pauseScreen = new PauseScreen(this);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        pauseScreen.setFont(masterRenderer.getFont());

        loadingScreen = new LoadingScreen();
        loadingScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        loadingScreen.setFont(masterRenderer.getFont());

        localPlayer = new LocalPlayer(new Vector3f(8.0f, 20.0f, 8.0f));
        engine.getWindow().disableCursor();
        localPlayer.getCamera().resetMouseTracking();

        // Mein schönie seedie : 1337l
        world = new World(engine.getVulkanContext(), localPlayer, eventBus, seed, worldName, worldSave);
        worldEventHandler = new WorldEventHandler(world, engine.getVulkanContext(), eventBus);
        localPlayer.initInteraction(world, engine.getVulkanContext(), eventBus);

        MeshData cloudData = world.getCloudSystem().generateCloudMesh(world.getSeed());
        masterRenderer.initClouds(cloudData);

        setupDebugOverlay();
        setupEvents();

        CommandManager cmdManager = new CommandManager(eventBus);
        chatScreen = new ChatScreen(eventBus, localPlayer, world, cmdManager, this::closeChat);
        chatScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        chatScreen.setFont(masterRenderer.getFont());
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
        inventoryToggleListener = event -> {
            uiNeedsRebuild = true;
            if (event.isOpen) engine.getWindow().enableCursor();
            else {
                engine.getWindow().disableCursor();
                localPlayer.getCamera().resetMouseTracking();
                localPlayer.getInteraction().resetCooldown();
            }
        };
        hotbarSlotChangeListener = event -> uiNeedsRebuild = true;
        debugToggleListener = event -> uiNeedsRebuild = true;

        eventBus.subscribe(InventoryToggleEvent.class, inventoryToggleListener);
        eventBus.subscribe(HotbarSlotChangeEvent.class, hotbarSlotChangeListener);
        eventBus.subscribe(DebugToggleEvent.class, debugToggleListener);
    }

    @Override
    public void update(float deltaTime) {
        // --- 0. LADEBILDSCHIRM LOGIK ---
        if (!world.getChunkManager().isInitialLoadComplete()) {
            world.getChunkManager().update(localPlayer.position.x, localPlayer.position.z);
            float progress = world.getChunkManager().getLoadingProgress(localPlayer.position.x, localPlayer.position.z);

            loadingScreen.setProgress(progress);
            uiNeedsRebuild = true;
            return;
        } else if (wasLoading) {
            wasLoading = false;
            uiNeedsRebuild = true;
            if (isNewWorld) {
                world.calcWorldspawnAndTeleportPlayer(localPlayer);
            }
        }

        // --- 1. SPEICHERN & BEENDEN ---
        if (isSavingAndQuitting) {
            saveWaitFrames--;
            if (saveWaitFrames <= 0) {
                engine.getSceneManager().changeScene(new MainMenuScene(engine));
            }
            uiNeedsRebuild = true;
            return;
        }

        // --- 2. PAUSE LOGIK ---
        if (engine.getInputManager().isActionJustPressed("PAUSE")) {
            if (isPaused) resumeGame();
            else pauseGame();
        }

        if (isPaused) {
            float uiMouseY = engine.getWindow().getHeight() - engine.getInputManager().getMouseY();
            pauseScreen.handleMenuInput(engine.getInputManager(), engine.getInputManager().getMouseX(), uiMouseY);
            uiNeedsRebuild = true;
            return;
        }

        // --- 3. NORMALES SPIEL ---
        if (!isChatOpen && engine.getInputManager().isActionJustPressed("DEBUG_MENU")) {
            debugOverlay.toggle();
            eventBus.publish(new DebugToggleEvent(debugOverlay.isVisible()));
        }

        world.getEnvironment().update(deltaTime);
        world.update(deltaTime, localPlayer);

        if (!isChatOpen && !localPlayer.getInventory().isOpen() && !isPaused) {
            if (engine.getInputManager().isActionJustPressed("CHAT_OPEN_T")) {
                openChat(false);
            } else if (engine.getInputManager().isActionJustPressed("CHAT_OPEN_SLASH")) {
                openChat(true);
            }
        }

        if (isChatOpen) {
            chatScreen.handleMenuInput(engine.getInputManager(), engine.getInputManager().getMouseX(), engine.getInputManager().getMouseY());
            uiNeedsRebuild = true;
        }

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
            MenuScreen activeScreen = null;

            if (!world.getChunkManager().isInitialLoadComplete()) {
                activeScreen = loadingScreen;
            } else if (isPaused || isSavingAndQuitting) {
                activeScreen = pauseScreen;
            } else if (isChatOpen) { // NEU
                activeScreen = chatScreen;
            }

            masterRenderer.rebuildUI(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen, chatOverlay);
            uiNeedsRebuild = false;
        }

        if (!masterRenderer.drawFrame(localPlayer.getCamera(), world, world.getEnvironment(), localPlayer.getInteraction())) {
            uiNeedsRebuild = true;
        }
    }

    @Override
    public void onResize() {
        MenuScreen activeScreen = null;
        if (!world.getChunkManager().isInitialLoadComplete()) {
            activeScreen = loadingScreen;
        } else if (isPaused || isSavingAndQuitting) {
            activeScreen = pauseScreen;
        }

        masterRenderer.recreate(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen, chatOverlay);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        loadingScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        chatScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        uiNeedsRebuild = true;
    }

    @Override
    public void cleanup() {
        if (eventBus != null) {
            eventBus.unsubscribe(InventoryToggleEvent.class, inventoryToggleListener);
            eventBus.unsubscribe(HotbarSlotChangeEvent.class, hotbarSlotChangeListener);
            eventBus.unsubscribe(DebugToggleEvent.class, debugToggleListener);

            //eventBus.cleanup();
        }

        if (worldEventHandler != null) {
            worldEventHandler.cleanup();
        }

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

        localPlayer.getInteraction().resetCooldown();
    }

    public void saveAndQuit() {
        isSavingAndQuitting = true;
        uiNeedsRebuild = true;
    }

    private void openChat(boolean startWithSlash) {
        isChatOpen = true;
        localPlayer.setChatOpen(true);
        chatScreen.open(startWithSlash);

        engine.getInputManager().consumeTypedChars();

        engine.getWindow().enableCursor();
        localPlayer.getCamera().resetMouseTracking();
        uiNeedsRebuild = true;
    }

    public void closeChat() {
        isChatOpen = false;
        localPlayer.setChatOpen(false);
        engine.getWindow().disableCursor();
        localPlayer.getCamera().resetMouseTracking();
        uiNeedsRebuild = true;
    }
}