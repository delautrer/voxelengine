package de.delautrer.game.world.generation.structure;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.NamespacedKey;

import java.util.List;

public class StructureTemplate {

    public static class StructureBlock {
        public final int dx, dy, dz;
        public final Block block;
        public final byte state;
        public final CompoundTag nbt;

        public StructureBlock(int dx, int dy, int dz, Block block, byte state, CompoundTag nbt) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.block = block;
            this.state = state;
            this.nbt = nbt;
        }
    }

    private final NamespacedKey key;
    private final int sizeX, sizeY, sizeZ;
    private final List<StructureBlock> blocks;

    public StructureTemplate(NamespacedKey key, int sizeX, int sizeY, int sizeZ, List<StructureBlock> blocks) {
        this.key = key;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = blocks;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public List<StructureBlock> getBlocks() {
        return blocks;
    }
}
