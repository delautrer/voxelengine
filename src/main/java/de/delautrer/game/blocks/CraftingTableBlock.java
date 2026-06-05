package de.delautrer.game.blocks;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.inventory.CraftingTableInventory;

public class CraftingTableBlock extends CubeBlock implements IInteractable {
    public CraftingTableBlock() {
        super(true, false); // Solid und undurchsichtig
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        player.openInventory(new CraftingTableInventory());
        return true;
    }
}
