package de.delautrer.game.world.generation.structure.processor;

import java.util.Random;

public class IntegrityProcessor extends StructureProcessor {
    private final float integrity;

    public IntegrityProcessor(float integrity) {
        this.integrity = Math.max(0.0f, Math.min(1.0f, integrity));
    }

    public float getIntegrity() {
        return integrity;
    }

    @Override
    public ProcessedBlock process(ProcessedBlock input, int worldX, int worldY, int worldZ, Random rand) {
        if (input == null) return null;
        if (input.block != null && input.block.isStructureVoid()) return input;
        if (integrity >= 1.0f) return input;
        if (rand.nextFloat() > integrity) {
            return null; // Decayed / discarded block
        }
        return input;
    }
}
