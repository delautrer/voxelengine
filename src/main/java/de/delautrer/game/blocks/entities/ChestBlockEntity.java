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
    public BlockEntityType<?> getType() {
        return BlockEntityTypeRegistry.CHEST;
    }

    @Override
    public void write(java.io.DataOutputStream dos) throws java.io.IOException {
        super.write(dos);
        dos.writeInt(inventory.getSize());
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.type != null && stack.amount > 0) {
                dos.writeInt(i);
                dos.writeUTF(de.delautrer.game.registry.Registries.ITEMS.getKey(stack.type).toString());
                dos.writeInt(stack.amount);
            } else {
                dos.writeInt(-1);
            }
        }
    }

    @Override
    public void read(java.io.DataInputStream dis) throws java.io.IOException {
        super.read(dis);
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            int slot = dis.readInt();
            if (slot != -1) {
                String itemKey = dis.readUTF();
                int amount = dis.readInt();
                de.delautrer.game.items.Item item = de.delautrer.game.registry.Registries.ITEMS.get(itemKey);
                if (item != null && amount > 0) {
                    inventory.setStack(slot, new ItemStack(item, amount));
                }
            }
        }
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