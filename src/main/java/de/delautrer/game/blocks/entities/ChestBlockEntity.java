package de.delautrer.game.blocks.entities;

import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.inventory.ChestInventory;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.ListTag;
import de.delautrer.game.nbt.VsnbtTag;
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
    public void writeTag(CompoundTag tag) {
        super.writeTag(tag);
        ListTag itemsList = new ListTag();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.type != null && stack.amount > 0) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemTag.putString("id", de.delautrer.game.registry.Registries.ITEMS.getKey(stack.type).toString());
                itemTag.putInt("Count", stack.amount);
                if (stack.tag != null) {
                    itemTag.put("tag", stack.tag.copy());
                }
                itemsList.add(itemTag);
            }
        }
        tag.put("Items", itemsList);
    }

    @Override
    public void readTag(CompoundTag tag) {
        super.readTag(tag);
        inventory.clear();
        if (tag.contains("Items", VsnbtTag.TYPE_LIST)) {
            ListTag itemsList = tag.getList("Items");
            for (int i = 0; i < itemsList.size(); i++) {
                VsnbtTag elem = itemsList.get(i);
                if (elem instanceof CompoundTag itemTag) {
                    int slot = itemTag.getByte("Slot") & 0xFF;
                    String itemKey = itemTag.getString("id");
                    int amount = itemTag.getInt("Count");
                    de.delautrer.game.items.Item item = de.delautrer.game.registry.Registries.ITEMS.get(itemKey);
                    if (item != null && amount > 0 && slot < inventory.getSize()) {
                        ItemStack stack = new ItemStack(item, amount);
                        if (itemTag.contains("tag", VsnbtTag.TYPE_COMPOUND)) {
                            stack.tag = itemTag.getCompound("tag");
                        }
                        inventory.setStack(slot, stack);
                    }
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