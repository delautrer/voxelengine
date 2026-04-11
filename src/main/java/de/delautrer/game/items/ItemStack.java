package de.delautrer.game.items;

public class ItemStack {
    public final Item type;
    public int amount;

    public ItemStack(Item type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}