package de.delautrer.game.inventory;

import de.delautrer.game.blocks.entities.FurnaceBlockEntity;

public class FurnaceInventory extends BaseInventory {
    private final FurnaceBlockEntity furnace;

    public FurnaceInventory(FurnaceBlockEntity furnace) {
        super(3);
        this.furnace = furnace;
    }

    public FurnaceBlockEntity getFurnace() {
        return furnace;
    }
}
