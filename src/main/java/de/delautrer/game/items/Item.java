package de.delautrer.game.items;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.entity.player.PlayerInteraction;
import org.joml.Vector3i;

public abstract class Item {
    public final String name;
    public final int iconIndex;

    public Item(String name, int iconIndex) {
        this.name = name;
        this.iconIndex = iconIndex;
    }

    // targetBlock: Der Block, auf den man schaut
    // adjacentBlock: Die leere Position direkt vor dem anvisierten Block (wichtig fürs Platzieren)
    public abstract void onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction);
}