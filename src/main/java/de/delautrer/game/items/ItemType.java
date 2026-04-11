package de.delautrer.game.items;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;

public class ItemType {
    public final String name;
    public final int iconIndex;
    public final boolean isPlaceable;
    public final Block associatedBlock;

    public ItemType(String name, int iconIndex, Block associatedBlock) {
        this.name = name;
        this.iconIndex = iconIndex;
        this.associatedBlock = associatedBlock;
        this.isPlaceable = (associatedBlock != BlockRegistry.AIR);
    }
}