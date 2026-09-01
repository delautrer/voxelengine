package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.world.Chunk;
import org.joml.Vector3i;

public class BlockChangeEvent implements Event {
    public final Vector3i pos;
    public final Block oldBlock;
    public final Block newBlock;
    public final Chunk chunk;

    public BlockChangeEvent(Vector3i pos, Block oldBlock, Block newBlock, Chunk chunk) {
        this.pos = pos;
        this.oldBlock = oldBlock;
        this.newBlock = newBlock;
        this.chunk = chunk;
    }
}