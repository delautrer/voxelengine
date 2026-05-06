package de.delautrer.game.blocks;

import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.events.InventoryOpenedEvent;

public class ChestBlock extends CubeBlock implements IInteractable {

    public ChestBlock() {
        super(true, false);
    }

    @Override
    public boolean hasBlockEntity() {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(World world, Vector3i pos) {
        return new ChestBlockEntity(world, pos);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof ChestBlockEntity chestEntity) {
            player.openInventory(chestEntity.getInventory());
            player.getInteraction().getEventBus().publish(new InventoryOpenedEvent(player, chestEntity.getInventory()));
            return true;
        }
        return false;
    }
}
