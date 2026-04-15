package de.delautrer.engine.states;

public class SceneManager {
    private Scene currentScene;
    private Scene nextScene;

    public void changeScene(Scene newScene) {
        this.nextScene = newScene;
    }

    public void update(float deltaTime) {
        // Szenenwechsel sicher durchführen
        if (nextScene != null) {
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