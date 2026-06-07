package de.delautrer.game.items;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class SimpleItem extends Item {
    @SuppressWarnings("this-escape")
    public SimpleItem(String name, String textureName) {
        super(name, textureName);
        this.setCategory("misc");
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock,
            Vector3i adjacentBlock, PlayerInteraction interaction) {
        return false;
    }
}
