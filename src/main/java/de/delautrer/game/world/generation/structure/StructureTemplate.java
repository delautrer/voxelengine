package de.delautrer.game.world.generation.structure;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.NamespacedKey;

import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;

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

    public void place(World world, int originX, int originY, int originZ) {
        if (world == null || blocks == null) return;
        Block airBlock = Registries.BLOCKS.get("veinstride:air");

        for (StructureBlock sb : blocks) {
            int wx = originX + sb.dx;
            int wy = originY + sb.dy;
            int wz = originZ + sb.dz;
            Block b = sb.block;
            byte st = sb.state;
            CompoundTag nbt = sb.nbt;

            if (b != null && b.isStructureVoid()) {
                b = airBlock;
                st = 0;
                nbt = null;
            }

            world.setBlockWithState(wx, wy, wz, b, st, false);
            if (nbt != null) {
                int cx = wx >> 4;
                int cz = wz >> 4;
                Chunk chunk = world.getChunkManager().getChunk(cx, cz);
                if (chunk != null) {
                    chunk.setBlockEntityTag(wx & 15, wy, wz & 15, nbt);
                }
                world.setBlockEntity(new org.joml.Vector3i(wx, wy, wz), null);
            }
        }
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
