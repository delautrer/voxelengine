package de.delautrer.engine.states;

import de.delautrer.engine.Engine;


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
            engine.getGraphicsContext().waitIdle();

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