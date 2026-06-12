package de.delautrer.game.world.generation;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.WorldGenerator;

public interface IChunkGenerator {
    void generate(Chunk chunk, WorldGenerator worldGenerator);
}
