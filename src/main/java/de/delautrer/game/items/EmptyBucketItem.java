package de.delautrer.game.items;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

public class EmptyBucketItem extends Item {

    public EmptyBucketItem(String name, String textureName) {
        super(name, textureName);
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (targetBlock == null) return false;

        // Prüfen, ob der Block, den wir anvisieren (oder der davor), Wasser ist
        Vector3i waterPos = null;
        if (world.getBlockAt(targetBlock) == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water").getId()) {
            waterPos = targetBlock;
        } else if (adjacentBlock != null && world.getBlockAt(adjacentBlock) == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water").getId()) {
            waterPos = adjacentBlock;
        }

        if (waterPos != null) {
            // Wasser aufsaugen (durch Luft / 0 ersetzen)
            world.setBlock(waterPos, (byte) 0);

            // NEU: Dem Spieler den vollen Eimer in die Hand drücken!
            interaction.getInventory().setStack(
                    interaction.getInventory().getSelectedSlot(),
                    new ItemStack(Registries.ITEMS.get(Constants.NAMESPACE + ":" + "water_bucket"), 1)
            );

            return true; // Erfolgreich aufgesammelt!
        }

        return false; // Nichts passiert
    }
}
