package de.delautrer.game.inventory;

import de.delautrer.game.blocks.entities.JigsawBlockEntity;
import de.delautrer.game.items.ItemStack;

public class JigsawInventory implements IInventory {

    private final JigsawBlockEntity blockEntity;

    public JigsawInventory(JigsawBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public JigsawBlockEntity getBlockEntity() {
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
