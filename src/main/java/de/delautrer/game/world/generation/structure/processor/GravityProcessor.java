package de.delautrer.game.world.generation.structure.processor;

import java.util.Random;

public class GravityProcessor extends StructureProcessor {
    private final String heightmap;

    public GravityProcessor(String heightmap) {
        this.heightmap = (heightmap != null) ? heightmap : "WORLD_SURFACE";
    }

    public String getHeightmap() {
        return heightmap;
    }

    @Override
    public ProcessedBlock process(ProcessedBlock input, int worldX, int worldY, int worldZ, Random rand) {
        return input;
    }
}
