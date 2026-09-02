package de.delautrer.game.blocks.entities;

import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.TagIo;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

    public void writeTag(CompoundTag tag) {
        if (pos != null) {
            tag.putInt("x", pos.x);
            tag.putInt("y", pos.y);
            tag.putInt("z", pos.z);
        }
    }

    public void readTag(CompoundTag tag) {
        // Base position is initialized via constructor in BlockEntityType.read
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(pos.x);
        dos.writeInt(pos.y);
        dos.writeInt(pos.z);

        CompoundTag tag = new CompoundTag();
        writeTag(tag);
        TagIo.writeCompound(tag, dos);
    }

    public void read(DataInputStream dis) throws IOException {
        CompoundTag tag = TagIo.readCompound(dis);
        readTag(tag);
    }

    public void onRemove() {}

    public void tick() {}
}