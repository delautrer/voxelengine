package de.delautrer.game.blocks.entities;

import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class BlockEntityType<T extends BlockEntity> {
    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(World world, Vector3i pos);
    }

    private final NamespacedKey key;
    private final BlockEntityFactory<T> factory;

    public BlockEntityType(NamespacedKey key, BlockEntityFactory<T> factory) {
        this.key = key;
        this.factory = factory;
    }

    public NamespacedKey getKey() { return key; }
    public T create(World world, Vector3i pos) { return factory.create(world, pos); }

    public T read(java.io.DataInputStream dis, World world) throws java.io.IOException {
        int px = dis.readInt();
        int py = dis.readInt();
        int pz = dis.readInt();
        T be = create(world, new Vector3i(px, py, pz));
        be.read(dis);
        return be;
    }
}