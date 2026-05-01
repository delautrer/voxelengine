package de.delautrer.game.blocks.entities;

import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.inventory.ChestInventory;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ChestBlockEntity extends BlockEntity {
    private final ChestInventory inventory;

    public ChestBlockEntity(World world, Vector3i pos) {
        super(world, pos);
        this.inventory = new ChestInventory();
    }

    public ChestInventory getInventory() {
        return inventory;
    }

    @Override
    public void onRemove() {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.amount > 0) {
                Vector3d spawnPos = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
                Vector3f vel = new Vector3f((float)Math.random() * 2 - 1, 2.0f, (float)Math.random() * 2 - 1).normalize().mul(3.0f);

                ItemEntity drop = new ItemEntity(stack, spawnPos, vel);
                world.spawnEntity(drop);
            }
        }
        inventory.clear();
    }
}