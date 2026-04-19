package de.delautrer.engine;

import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.SceneManager;
import de.delautrer.engine.window.Window;
import de.delautrer.game.blocks.models.BlockModelManager;
import de.delautrer.game.states.MainMenuScene;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;

import java.util.Set;

public class Engine {

    private Window window;
    private VulkanContext vulkanContext;
    private InputManager inputManager;

    private SceneManager sceneManager;

    private TextureStitcher.AtlasResult blockAtlas;

    private float lastFrame = 0.0f;
    private int currentFps = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window(1280, 720, "Voxel Engine");
        window.disableCursor();
        vulkanContext = new VulkanContext(window);
        inputManager = new InputManager(window.getHandle());

        System.out.println("Reading BlockModels...");
        Set<String> requiredTextures = BlockModelManager.getRequiredTextures();

        System.out.println("Building texture atlas with " + requiredTextures.size() + " textures...");
        try {
            blockAtlas = TextureStitcher.buildAtlas(requiredTextures, "atlas_debug.png");
            System.out.println("Texture atlas built with " + blockAtlas.atlasWidth + "x" + blockAtlas.atlasHeight);
            de.delautrer.game.blocks.models.BlockModelManager.loadAllModels(blockAtlas);

        } catch (Exception e) {
            System.err.println("Error whilst building texture atlas:");
            e.printStackTrace();
        }

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
        System.out.println("--- ENGINE SHUTDOWN START ---");
        VK10.vkDeviceWaitIdle(vulkanContext.getDevice());

        sceneManager.cleanup();
        if (blockAtlas != null) blockAtlas.cleanup();
        if (vulkanContext != null) vulkanContext.cleanup();
        if (window != null) window.cleanup();

        System.out.println("--- ENGINE SHUTDOWN BEENDET ---");
    }

    public SceneManager getSceneManager() { return sceneManager; }
    public Window getWindow() { return window; }
    public VulkanContext getVulkanContext() { return vulkanContext; }
    public InputManager getInputManager() { return inputManager; }
    public int getCurrentFps() { return currentFps; }
    public TextureStitcher.AtlasResult getBlockAtlas() { return blockAtlas; }
}