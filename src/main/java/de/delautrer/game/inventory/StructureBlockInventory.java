package de.delautrer.game.inventory;

import de.delautrer.game.blocks.entities.StructureBlockEntity;
import de.delautrer.game.items.ItemStack;

public class StructureBlockInventory implements IInventory {

    private final StructureBlockEntity blockEntity;

    public StructureBlockInventory(StructureBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public StructureBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override public int getSize() { return 0; }
    @Override public ItemStack getStack(int index) { return null; }
    @Override public void setStack(int index, ItemStack stack) {}
    @Override public int addItem(ItemStack stack) { return stack != null ? stack.amount : 0; }
    @Override public void clear() {}
    @Override public boolean isSortable() { return false; }
    @Override public void sort() {}
}
