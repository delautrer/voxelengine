package de.delautrer.game.items;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class EmptyBucketItem extends Item {

    public EmptyBucketItem(String name, String textureName) {
        super(name, textureName);
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (targetBlock == null) return false;

        // Prüfen, ob der Block, den wir anvisieren (oder der davor), Wasser ist
        Vector3i waterPos = null;
        if (world.getBlockAt(targetBlock) == BlockRegistry.WATER.getId()) {
            waterPos = targetBlock;
        } else if (adjacentBlock != null && world.getBlockAt(adjacentBlock) == BlockRegistry.WATER.getId()) {
            waterPos = adjacentBlock;
        }

        if (waterPos != null) {
            // Wasser aufsaugen (durch Luft / 0 ersetzen)
            world.setBlock(waterPos, (byte) 0);

            // NEU: Dem Spieler den vollen Eimer in die Hand drücken!
            interaction.getInventory().setStack(
                    interaction.getInventory().getSelectedSlot(),
                    new de.delautrer.game.items.ItemStack(de.delautrer.game.items.ItemRegistry.WATER_BUCKET, 1)
            );

            return true; // Erfolgreich aufgesammelt!
        }

        return false; // Nichts passiert
    }
}