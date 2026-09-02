package de.delautrer.game.world.generation.structure.processor;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.ChestBlock;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;

import java.util.Random;

public class RuleProcessor extends StructureProcessor {
    private final Block inputBlock;
    private final Block outputBlock;
    private final float probability;

    public RuleProcessor(Block inputBlock, Block outputBlock, float probability) {
        this.inputBlock = inputBlock;
        this.outputBlock = outputBlock;
        this.probability = Math.max(0.0f, Math.min(1.0f, probability));
    }

    @Override
    public ProcessedBlock process(ProcessedBlock input, int worldX, int worldY, int worldZ, Random rand) {
        if (input == null || input.block == null) return input;

        // Kisten/Truhen und NBT-Blöcke schützen
        if (input.nbt != null || input.block instanceof ChestBlock) return input;
        NamespacedKey bKey = Registries.BLOCKS.getKey(input.block);
        if (bKey != null && bKey.getKey().contains("chest")) return input;

        if (inputBlock != null && input.block.equals(inputBlock)) {
            if (rand.nextFloat() <= probability) {
                return new ProcessedBlock(outputBlock, input.state, input.nbt);
            }
        }
        return input;
    }
}
