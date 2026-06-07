package de.delautrer.game.inventory;

public class ChestInventory extends BaseInventory {
    @SuppressWarnings("this-escape")
    public ChestInventory() {
        super(27);
        this.setSortable(true);
    }
}