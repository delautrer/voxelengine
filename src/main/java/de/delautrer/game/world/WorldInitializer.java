package de.delautrer.game.world;

import org.joml.Vector3f;

public class WorldInitializer {

    public static Vector3f findSpawnPoint(long seed) {
        WorldGenerator generator = new WorldGenerator(seed);
        Chunk spawnChunk = new Chunk(0, 0);

        // 1. Chunk (0,0) synchron generieren (nur Datenstruktur, kein VRAM Upload)
        generator.generate(spawnChunk);

        // 2. Den höchsten Block in der Mitte (8, 8) finden
        int spawnX = 8;
        int spawnZ = 8;

        for (int y = Chunk.HEIGHT - 2; y > 0; y--) {
            if (spawnChunk.getBlock(spawnX, y, spawnZ) != 0) { // 0 = Air
                // Wir spawnen den Spieler mittig auf dem Block (X+0.5, Z+0.5)
                // y+1 ist der erste leere Block, y+1.5f gibt etwas Puffer, damit er nicht im Boden steckt
                return new Vector3f(spawnX + 0.5f, y + 1.5f, spawnZ + 0.5f);
            }
        }

        // Fallback, falls der Chunk komplett leer sein sollte (sollte nie passieren)
        return new Vector3f(8.5f, 100.0f, 8.5f);
    }
}