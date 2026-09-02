package de.delautrer.game.items;

import de.delautrer.game.nbt.CompoundTag;

public class ItemStack {
    public final Item type;
    public int amount;
    public int durability;
    public CompoundTag tag;

    public ItemStack(Item type, int amount) {
        this.type = type;
        this.amount = amount;
        if (type instanceof ToolItem) {
            this.durability = ((ToolItem) type).getMaxDurability();
        } else {
            this.durability = -1;
        }
    }

    public void damage(int amount) {
        if (type instanceof ToolItem) {
            this.durability -= amount;
        }
    }
}