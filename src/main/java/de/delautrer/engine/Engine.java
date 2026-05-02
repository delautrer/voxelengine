package de.delautrer.engine;

import de.delautrer.Constants;
import de.delautrer.engine.audio.AudioEngine;
import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.SceneManager;
import de.delautrer.engine.window.Window;
import de.delautrer.game.blocks.models.BlockModelManager;
import de.delautrer.game.items.ItemModelManager;
import de.delautrer.game.states.MainMenuScene;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;

public class Engine {

    private Window window;
    private VulkanContext vulkanContext;
    private InputManager inputManager;
    private AudioEngine audioEngine;

    private SceneManager sceneManager;

    private TextureStitcher.AtlasResult blockAtlas;
    private TextureStitcher.AtlasResult itemAtlas;

    private float lastFrame = 0.0f;
    private int currentFps = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window(1280, 720, "Voxel Engine - " + Constants.VERSION);
        window.disableCursor();

        audioEngine = new AudioEngine();
        audioEngine.init();
        SoundManager.init(audioEngine);

        vulkanContext = new VulkanContext(window);
        inputManager = new InputManager(window.getHandle());

        System.out.println("[Engine] Building block atlas...");
        try {
            java.util.Set<String> reqBlocks = BlockModelManager.getRequiredTextures();
            // is2DAtlas = false, Ordner = "assets/textures/block"
            blockAtlas = TextureStitcher.buildAtlas(reqBlocks, "atlas_blocks_debug.png", "assets/textures/block", false);
            BlockModelManager.loadAllModels(blockAtlas);
        } catch (Exception e) { e.printStackTrace(); }

        System.out.println("[Engine] Building item atlas...");
        try {
            java.util.Set<String> reqItems = ItemModelManager.getRequiredTextures();
            // is2DAtlas = true, Ordner = "assets/textures/item"
            itemAtlas = TextureStitcher.buildAtlas(reqItems, "atlas_items_debug.png", "assets/textures/item", true);
            ItemModelManager.loadAllModels(itemAtlas);
        } catch (Exception e) { e.printStackTrace(); }

        BlockModelManager.loadAllModels(blockAtlas);

        sceneManager = new SceneManager(this);
        sceneManager.changeScene(new MainMenuScene(this));
    }

    private void loop() {
        while (!window.shouldClose()) {
            float currentFrameTime = (float) GLFW.glfwGetTime();
            float deltaTime = currentFrameTime - lastFrame;
            lastFrame = currentFrameTime;

            if (deltaTime > 0) currentFps = (int)(1.0f / deltaTime);
            if (deltaTime > 0.1f) {
                deltaTime = 0.1f;
            }

            window.pollEvents();

            if (window.isFramebufferResized()) {
                window.setFramebufferResized(false);
                sceneManager.onResize();
            }

            sceneManager.update(deltaTime);
            sceneManager.render();
            inputManager.update();
        }
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());
    }

    private void cleanup() {
        System.out.println("[Engine] Shutting down...");
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());

        if (audioEngine != null) audioEngine.cleanup();
        sceneManager.cleanup();
        if (blockAtlas != null) blockAtlas.cleanup();
        if (itemAtlas != null) itemAtlas.cleanup();
        if (vulkanContext != null) vulkanContext.cleanup();
        if (window != null) window.cleanup();

        System.out.println("[Engine] Gone. RIP lovely engine. See you soon my fren.");
    }

    public AudioEngine getAudioEngine() { return audioEngine; }
    public SceneManager getSceneManager() { return sceneManager; }
    public Window getWindow() { return window; }
    public VulkanContext getVulkanContext() { return vulkanContext; }
    public InputManager getInputManager() { return inputManager; }
    public int getCurrentFps() { return currentFps; }
    public TextureStitcher.AtlasResult getBlockAtlas() { return blockAtlas; }
    public TextureStitcher.AtlasResult getItemAtlas() {
        return itemAtlas;
    }
}