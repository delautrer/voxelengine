package de.delautrer.game.items;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.blocks.Block;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

class EmptyBucketItem extends Item {

    public EmptyBucketItem(String name, String textureName) {
        super(name, textureName);
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (targetBlock == null) return false;

        // Prüfen, ob der Block, den wir anvisieren (oder der davor), Wasser ist
        Vector3i waterPos = null;
        Block waterBlock = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water");
        if (world.getBlock(targetBlock) == waterBlock) {
            waterPos = targetBlock;
        } else if (adjacentBlock != null && world.getBlock(adjacentBlock) == waterBlock) {
            waterPos = adjacentBlock;
        }

        if (waterPos != null) {
            // Wasser aufsaugen (durch Luft ersetzen)
            world.setBlock(waterPos, Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air"));
            return true; // Erfolgreich aufgesammelt!
        }

        return false; // Nichts passiert
    }
}
