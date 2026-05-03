package de.delautrer.game.world;

import de.delautrer.game.world.generation.*;

import java.util.ArrayList;
import java.util.List;

public class WorldGenerator {

    private final long seed;
    private final List<IGenerationPass> passes = new ArrayList<>();

    public WorldGenerator(long seed) {
        this.seed = seed;

        // Hier definieren wir die exakte Reihenfolge der Weltgenerierung!
        passes.add(new TerrainPass(seed));
        passes.add(new CavePass());
        passes.add(new SurfacePass(seed));
        passes.add(new DecoratorPass(seed));
    }

    public void generate(Chunk chunk) {
        // Dieses Array wandert von Pass zu Pass, damit z.B. der Höhlen-Pass
        // weiß, wo die Oberfläche ist, ohne den Chunk neu scannen zu müssen.
        int[][] heightMap = new int[Chunk.SIZE][Chunk.SIZE];

        for (IGenerationPass pass : passes) {
            pass.process(chunk, seed, heightMap);
        }
    }
}