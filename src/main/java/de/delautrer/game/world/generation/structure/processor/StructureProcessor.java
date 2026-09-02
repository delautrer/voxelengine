package de.delautrer.game.world.generation.structure.processor;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.nbt.CompoundTag;
import java.util.Random;

public abstract class StructureProcessor {

    public static class ProcessedBlock {
        public Block block;
        public byte state;
        public CompoundTag nbt;

        public ProcessedBlock(Block block, byte state, CompoundTag nbt) {
            this.block = block;
            this.state = state;
            this.nbt = nbt;
        }
    }

    public abstract ProcessedBlock process(ProcessedBlock input, int worldX, int worldY, int worldZ, Random rand);
}
