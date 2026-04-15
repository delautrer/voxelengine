package de.delautrer.game.world.generation;

import de.delautrer.game.world.Chunk;

public interface IGenerationPass {
    /**
     * Führt diesen Generierungsschritt für den übergebenen Chunk aus.
     * @param chunk Der Chunk, der bearbeitet wird.
     * @param seed Der Welt-Seed (für Randomness).
     * @param heightMap Ein Array [Chunk.SIZE][Chunk.SIZE], das Pässe untereinander teilen können (z.B. für die Oberflächenhöhe).
     */
    void process(Chunk chunk, long seed, int[][] heightMap);
}