package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.Block;

public class BlockSelectedEvent implements Event {
    private final Block block;

    public BlockSelectedEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}