package de.delautrer.engine.states;

import de.delautrer.engine.Engine;
import org.lwjgl.vulkan.VK10;

public class SceneManager {
    private Scene currentScene;
    private Scene nextScene;
    private final Engine engine;

    public SceneManager(Engine engine) {
        this.engine = engine;
    }

    public void changeScene(Scene newScene) {
        this.nextScene = newScene;
    }

    public void update(float deltaTime) {
        if (nextScene != null) {
            VK10.vkDeviceWaitIdle(engine.getVulkanContext().getDevice());

            if (currentScene != null) {
                currentScene.cleanup();
            }
            currentScene = nextScene;
            currentScene.init();
            nextScene = null;
        }

        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
    }

    public void render() {
        if (currentScene != null && nextScene == null) {
            currentScene.render();
        }
    }

    public void onResize() {
        if (currentScene != null) currentScene.onResize();
    }

    public void cleanup() {
        if (currentScene != null) currentScene.cleanup();
    }
}