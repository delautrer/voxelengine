package de.delautrer.game.world.systems;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.sky.CloudSystem;
import de.delautrer.game.world.sky.SkyManager;

public class WeatherSystem implements WorldSystem {

    private final CloudSystem cloudSystem;
    private final SkyManager skyManager;

    public WeatherSystem(CloudSystem cloudSystem, SkyManager skyManager) {
        this.cloudSystem = cloudSystem;
        this.skyManager = skyManager;
    }

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        cloudSystem.update(deltaTime);
        // SkyManager update could go here as well if it needs per-tick updates
    }

    public CloudSystem getCloudSystem() {
        return cloudSystem;
    }

    public SkyManager getSkyManager() {
        return skyManager;
    }
}
