package de.delautrer.game.blocks;

import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.StructureBlockEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.inventory.StructureBlockInventory;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class StructureBlock extends CubeBlock implements IInteractable {

    @SuppressWarnings("this-escape")
    public StructureBlock() {
        super(true, false);
        setHardness(-1.0f);
        setLootTable(null);
    }

    @Override
    public boolean hasBlockEntity() {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(World world, Vector3i pos) {
        return new StructureBlockEntity(world, pos);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StructureBlockEntity sbe) {
            player.openInventory(new StructureBlockInventory(sbe));
            return true;
        }
        return false;
    }
}
