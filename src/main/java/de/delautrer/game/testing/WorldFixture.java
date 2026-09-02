package de.delautrer.game.testing;

import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;

public class WorldFixture {

    public static World create() {
        Registries.init();
        World world = new World(null, null, null, 12345L, "world_fixture", "world_fixture_save", "DEFAULT", "", null, false);

        // Pre-create chunks in a 5x5 chunk grid centered at (0,0) to accommodate tests at origin
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                chunk.setPalette(world.getBlockPalette());
                world.getChunkManager().addChunk(chunk);
            }
        }

        return world;
    }
}
