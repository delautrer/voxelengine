package de.delautrer.game.blocks.entities;

import de.delautrer.game.world.World;
import org.joml.Vector3i;

public abstract class BlockEntity {
    protected final Vector3i pos;
    protected final World world;

    public BlockEntity(World world, Vector3i pos) {
        this.world = world;
        this.pos = pos;
    }

    public Vector3i getPos() { return pos; }
    public World getWorld() { return world; }

    public void onRemove() {}

    public void tick() {}
}