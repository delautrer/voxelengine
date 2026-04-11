package de.delautrer.game.events;

import de.delautrer.engine.events.Event;

public class BlockSelectedEvent implements Event {
    private final byte blockType;

    public BlockSelectedEvent(byte blockType) {
        this.blockType = blockType;
    }

    public byte getBlockType() {
        return blockType;
    }
}