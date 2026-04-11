package de.delautrer.game.items;

public class ItemStack {
    public final ItemType type;
    public int amount;

    public ItemStack(ItemType type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}