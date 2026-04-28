package de.delautrer.game.inventory;

import de.delautrer.game.inventory.BaseInventory;

public class CraftingInventory extends BaseInventory {
    private final int width;
    private final int height;

    public CraftingInventory(int width, int height) {
        super(width * height);
        this.width = width;
        this.height = height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}