package de.delautrer.game.world.systems;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;

public interface WorldSystem {
    void update(World world, float deltaTime, LocalPlayer localPlayer);
}
