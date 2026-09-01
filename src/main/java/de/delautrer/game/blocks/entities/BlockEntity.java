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
    public abstract BlockEntityType<?> getType();

    public void write(java.io.DataOutputStream dos) throws java.io.IOException {
        dos.writeInt(pos.x);
        dos.writeInt(pos.y);
        dos.writeInt(pos.z);
    }

    public void read(java.io.DataInputStream dis) throws java.io.IOException {}

    public void onRemove() {}

    public void tick() {}
}