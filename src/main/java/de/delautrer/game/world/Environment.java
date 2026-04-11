package de.delautrer.game.world;

public class Environment {
    private float timeOfDay = 0.0f;
    private float globalLight;
    private float skyR, skyG, skyB;

    public void update(float deltaTime) {
        timeOfDay += deltaTime * 0.0052f; // 0.0052f -> MC.

        float sunHeight = (float) Math.sin(timeOfDay);
        globalLight = Math.max(0.1f, (sunHeight + 1.0f) / 2.0f);

        skyR = 0.02f + 0.51f * globalLight;
        skyG = 0.02f + 0.79f * globalLight;
        skyB = 0.08f + 0.84f * globalLight;
    }

    public float getGlobalLight() { return globalLight; }
    public float getSkyR() { return skyR; }
    public float getSkyG() { return skyG; }
    public float getSkyB() { return skyB; }
}