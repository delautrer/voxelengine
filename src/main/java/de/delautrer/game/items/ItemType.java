package de.delautrer.game.items;

import de.delautrer.game.world.BlockType;

public class ItemType {
    public final String name;
    public final int iconIndex;
    public final boolean isPlaceable;
    public final BlockType associatedBlock;

    public ItemType(String name, int iconIndex, BlockType associatedBlock) {
        this.name = name;
        this.iconIndex = iconIndex;
        this.associatedBlock = associatedBlock;
        this.isPlaceable = (associatedBlock != BlockType.AIR);
    }
}