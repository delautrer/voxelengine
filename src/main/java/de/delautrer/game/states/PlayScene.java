package de.delautrer.game.states;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.Constants;
import de.delautrer.engine.MasterRenderer;
import de.delautrer.engine.audio.AudioEngine;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.engine.graphics.MeshData;
import de.delautrer.engine.states.Scene;
import de.delautrer.engine.Engine;
import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import de.delautrer.engine.utils.GamePaths;
import de.delautrer.game.commands.CommandManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.*;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.gui.screens.*;
import de.delautrer.game.world.*;
import de.delautrer.game.world.sky.Weather;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.events.InventoryClosedEvent;
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
    private DeathScreen deathScreen;

    private boolean wasDead = false;
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

    private boolean hideUI = false;
    private float screenshotCooldown = 0.0f;

    private boolean isTakingIsometric = false;
    private int isoFramesToWait = 0;

    private EventListener<InventoryToggleEvent> inventoryToggleListener;
    private EventListener<HotbarSlotChangeEvent> hotbarSlotChangeListener;
    private EventListener<DebugToggleEvent> debugToggleListener;
    private EventListener<InventoryChangeEvent> inventoryChangeEvent;
    private EventListener<InventoryOpenedEvent> openListener;
    private EventListener<InventoryClosedEvent> closeListener;
    private EventListener<PlayerDamageEvent> playerDamageEventListener;

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
        masterRenderer = new MasterRenderer((VulkanContext)engine.getGraphicsContext(), engine.getWindow(), engine.getBlockAtlas(), engine.getItemAtlas());
        debugOverlay = new DebugOverlay();
        chatOverlay = new ChatOverlay(eventBus);

        pauseScreen = new PauseScreen(this);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        pauseScreen.setFont(masterRenderer.getFont());

        loadingScreen = new LoadingScreen();
        loadingScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        loadingScreen.setFont(masterRenderer.getFont());

        localPlayer = new LocalPlayer(new Vector3d(8.0, 20.0, 8.0));
        engine.getWindow().disableCursor();
        localPlayer.getCamera().resetMouseTracking();


        deathScreen = new DeathScreen(localPlayer, this);
        deathScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        deathScreen.setFont(masterRenderer.getFont());

        // Mein schönie seedie : 1337l
        world = new World((VulkanContext)engine.getGraphicsContext(), localPlayer, eventBus, seed, worldName, worldSave);
        worldEventHandler = new WorldEventHandler(world, (VulkanContext)engine.getGraphicsContext(), eventBus);
        localPlayer.initInteraction(world, (VulkanContext)engine.getGraphicsContext(), eventBus);

        float cloudLayer;
        if(engine.getBlockAtlas().regions.containsKey("just_white")) {
            cloudLayer = engine.getBlockAtlas().regions.get("just_white").layer;
        } else {
            cloudLayer = 0.0f;
        }
        MeshData cloudData = world.getSkyManager().getCloudSystem().generateCloudMesh(world.getSeed(), cloudLayer, world.getSkyManager().getCurrentWeather());
        masterRenderer.initClouds(cloudData);
        world.getSkyManager().setWeatherCallback(() -> {
            MeshData newCloudData = world.getSkyManager().getCloudSystem().generateCloudMesh(world.getSeed(), cloudLayer, world.getSkyManager().getCurrentWeather());
            masterRenderer.initClouds(newCloudData);
        });
        world.getSkyManager().forceWeather(world.getSkyManager().getCurrentWeather());

        masterRenderer.initStars(world.getSkyManager().getStarSystem().generateStarMesh());
        masterRenderer.initCelestial(world.getSkyManager().getCelestialSystem().generateCelestialMesh());

        setupDebugOverlay();
        setupEvents();

        CommandManager cmdManager = new CommandManager(eventBus);
        chatScreen = new ChatScreen(eventBus, localPlayer, world, cmdManager, chatOverlay, this::closeChat);
        chatScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        chatScreen.setFont(masterRenderer.getFont());
    }

    private void setupDebugOverlay() {
        debugOverlay.addLine("Version", () -> Constants.VERSION);
        debugOverlay.addLine("FPS", () -> String.format("%d", engine.getCurrentFps()));
        debugOverlay.addLine("Chunks (Loaded/Visible)", () ->
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
                BlockState state = world.getBlockState(target.x, target.y, target.z);
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
            float time = world.getSkyManager().getTimeOfDay();

            int hours = (int) time;
            int minutes = (int) ((time - hours) * 60);
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
                engine.getInputManager().setTypingMode(false);
            }
        };
        hotbarSlotChangeListener = event -> uiNeedsRebuild = true;
        debugToggleListener = event -> uiNeedsRebuild = true;
        inventoryChangeEvent = event -> uiNeedsRebuild = true;
        openListener = event -> uiNeedsRebuild = true;
        closeListener = event -> uiNeedsRebuild = true;
        playerDamageEventListener = event -> uiNeedsRebuild = true;

        eventBus.subscribe(InventoryToggleEvent.class, inventoryToggleListener);
        eventBus.subscribe(HotbarSlotChangeEvent.class, hotbarSlotChangeListener);
        eventBus.subscribe(DebugToggleEvent.class, debugToggleListener);
        eventBus.subscribe(InventoryChangeEvent.class, inventoryChangeEvent);
        eventBus.subscribe(InventoryOpenedEvent.class, openListener);
        eventBus.subscribe(InventoryClosedEvent.class, closeListener);
        eventBus.subscribe(PlayerDamageEvent.class, playerDamageEventListener);
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

        // --- 2. TODES LOGIK ---
        if (localPlayer.isDead()) {
            if (!wasDead) {
                engine.getWindow().enableCursor();
                localPlayer.getCamera().resetMouseTracking();

                if (isChatOpen) closeChat();
                if (localPlayer.getOpenedInventory() != null) {
                    eventBus.publish(new InventoryClosedEvent(localPlayer, localPlayer.getOpenedInventory()));
                    localPlayer.closeInventory();
                }
                if (localPlayer.getInventory().isOpen()) {
                    localPlayer.getInventory().setOpen(false);
                    eventBus.publish(new InventoryToggleEvent(false));
                }

                wasDead = true;
            }

            float uiMouseY = engine.getWindow().getHeight() - engine.getInputManager().getMouseY();
            deathScreen.handleMenuInput(engine.getInputManager(), engine.getInputManager().getMouseX(), uiMouseY);

            uiNeedsRebuild = true;
        } else {
            wasDead = false;
        }


        // --- 3. PAUSE LOGIK ---
        if (engine.getInputManager().isActionJustPressed("PAUSE")) {
            if (isChatOpen) {
                closeChat();
            }
            else if (localPlayer.getOpenedInventory() != null) {
                eventBus.publish(new InventoryClosedEvent(localPlayer, localPlayer.getOpenedInventory()));
                localPlayer.closeInventory();

                engine.getWindow().disableCursor();
                localPlayer.getCamera().resetMouseTracking();
                uiNeedsRebuild = true;
            }
            else if (localPlayer.getInventory().isOpen()) {
                localPlayer.getInventory().setOpen(false);
                eventBus.publish(new InventoryToggleEvent(false));
                engine.getWindow().disableCursor();
                localPlayer.getCamera().resetMouseTracking();
                uiNeedsRebuild = true;
            } else if (isPaused) {
                resumeGame();
            } else {
                pauseGame();
            }
        }

        if (isPaused) {
            float uiMouseY = engine.getWindow().getHeight() - engine.getInputManager().getMouseY();
            pauseScreen.handleMenuInput(engine.getInputManager(), engine.getInputManager().getMouseX(), uiMouseY);
            uiNeedsRebuild = true;
            return;
        }

        // --- 4. NORMALES SPIEL ---
        // Cooldown ticken lassen
        if (screenshotCooldown > 0) screenshotCooldown -= deltaTime;

        // F1 - UI Toggle
        if (engine.getInputManager().isActionJustPressed("TOGGLE_UI")) { // Stell sicher, dass du F1 als "TOGGLE_UI" bindest!
            hideUI = !hideUI;
            uiNeedsRebuild = true;
        }

        // F2 - Screenshots (Standard & Isometrisch)
        if (engine.getInputManager().isActionJustPressed("SCREENSHOT") && screenshotCooldown <= 0) { // F2 binden!
            String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            if (engine.getInputManager().isActionActive("MOD_ALT")) { // Alt gedrückt?
                // Isometrisch Starten!
                isTakingIsometric = true;
                isoFramesToWait = 2; // Wir warten 2 Frames, damit der LoadingScreen aufpoppt!
                //pauseGame(); // Spiel anhalten
                uiNeedsRebuild = true;
            } else {
                // Normaler Screenshot
                String path = GamePaths.SCREENSHOTS_DIR.resolve(date + ".png").toString();
                masterRenderer.requestScreenshot(path);
                screenshotCooldown = 2.0f;
                chatOverlay.getMessages().add(new ChatOverlay.ChatMessage("Saved screenshot: " + path));
            }
        }

        // Isometrische Screenshot Warteschleife
        if (isTakingIsometric) {
            if (isoFramesToWait > 0) {
                isoFramesToWait--; // Warten, bis das UI/Ladebildschirm bereit ist...
            } else if (isoFramesToWait == 0) {
                // JETZT den Screenshot an Vulkan in Auftrag geben
                String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String path = GamePaths.SCREENSHOTS_DIR.resolve(date + "_isometric.png").toString();

                masterRenderer.requestScreenshot(path);
                screenshotCooldown = 2.0f;
                chatOverlay.getMessages().add(new ChatOverlay.ChatMessage("Saved Isometric screenshot: " + path));

                isoFramesToWait = -1;
            } else if (isoFramesToWait == -1) {
                isTakingIsometric = false;
                //resumeGame();
            }
        }

        if (!isChatOpen && engine.getInputManager().isActionJustPressed("DEBUG_MENU")) {
            debugOverlay.toggle();
            eventBus.publish(new DebugToggleEvent(debugOverlay.isVisible()));
        }

        world.getSkyManager().update(deltaTime);
        world.update(deltaTime, localPlayer);
        if (chatOverlay.update(deltaTime)) {
            uiNeedsRebuild = true;
        }

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

        if (localPlayer.getInventory().isOpen() || localPlayer.getOpenedInventory() != null || debugOverlay.isVisible()) {
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
            } else if (localPlayer.isDead()) {
                activeScreen = deathScreen;
            } else if (isPaused || isSavingAndQuitting) {
                activeScreen = pauseScreen;
            } else if (isChatOpen) {
                activeScreen = chatScreen;
            }

            if (localPlayer.isDead()) {
                masterRenderer.rebuildUI(null, engine.getInputManager(), null, activeScreen, null);
            } else {
                masterRenderer.rebuildUI(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen, chatOverlay);
            }

            uiNeedsRebuild = false;
        }

        if (!masterRenderer.drawFrame(localPlayer.getCamera(), world, localPlayer.getInteraction(), hideUI, isoFramesToWait, isTakingIsometric)) {
            uiNeedsRebuild = true;
        }
    }

    @Override
    public void onResize() {
        MenuScreen activeScreen = null;
        if (!world.getChunkManager().isInitialLoadComplete()) {
            activeScreen = loadingScreen;
        } else if (localPlayer.isDead()) {
            activeScreen = deathScreen;
        } else if (isPaused || isSavingAndQuitting) {
            activeScreen = pauseScreen;
        }

        masterRenderer.recreate(localPlayer.getInteraction(), engine.getInputManager(), debugOverlay, activeScreen, chatOverlay);
        pauseScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        loadingScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        chatScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());

        if(deathScreen != null) deathScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());

        uiNeedsRebuild = true;
    }

    @Override
    public void cleanup() {
        if (eventBus != null) {
            eventBus.unsubscribe(InventoryToggleEvent.class, inventoryToggleListener);
            eventBus.unsubscribe(HotbarSlotChangeEvent.class, hotbarSlotChangeListener);
            eventBus.unsubscribe(DebugToggleEvent.class, debugToggleListener);

            eventBus.unsubscribe(InventoryChangeEvent.class, inventoryChangeEvent);
            eventBus.unsubscribe(InventoryOpenedEvent.class, openListener);
            eventBus.unsubscribe(InventoryClosedEvent.class, closeListener);
            eventBus.unsubscribe(PlayerDamageEvent.class, playerDamageEventListener);

            eventBus.cleanup();
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
        chatOverlay.resetScroll();
        localPlayer.setChatOpen(true);
        chatScreen.open(startWithSlash);

        engine.getInputManager().setTypingMode(true);
        engine.getInputManager().consumeTypedChars();

        engine.getWindow().enableCursor();
        localPlayer.getCamera().resetMouseTracking();
        uiNeedsRebuild = true;
    }

    public void closeChat() {
        isChatOpen = false;
        localPlayer.setChatOpen(false);
        engine.getWindow().disableCursor();
        engine.getInputManager().setTypingMode(false);
        localPlayer.getCamera().resetMouseTracking();
        uiNeedsRebuild = true;
    }


    public AudioEngine getAudioEngine() { return this.engine.getAudioEngine(); }

    public World getWorld() {
        return world;
    }
}
