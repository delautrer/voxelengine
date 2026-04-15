package de.delautrer.engine.states;

import de.delautrer.engine.Engine;

public abstract class Scene {
    protected final Engine engine;

    public Scene(Engine engine) {
        this.engine = engine;
    }

    public abstract void init();
    public abstract void update(float deltaTime);
    public abstract void render();
    public abstract void cleanup();
    public abstract void onResize();
}