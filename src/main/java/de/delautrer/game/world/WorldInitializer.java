package de.delautrer.game.world;

import org.joml.Vector3f;

public class WorldInitializer {

    public static Vector3f findSpawnPoint(long seed, String generatorType, String generatorOptions) {
        if ("FLAT".equalsIgnoreCase(generatorType)) {
            if (generatorOptions == null || generatorOptions.trim().isEmpty()) {
                generatorOptions = "1xbedrock;3xstone;2xdirt;1xgrass_block";
            }
            int totalHeight = 0;
            String[] parts = generatorOptions.split(";");
            for (String part : parts) {
                if (part.isEmpty()) continue;
                String[] split = part.split("x");
                if (split.length == 2) {
                    try {
                        totalHeight += Integer.parseInt(split[0]);
                    } catch (NumberFormatException ignored) {}
                }
            }
            return new Vector3f(0.5f, de.delautrer.game.world.Chunk.MIN_Y + totalHeight + 1.5f, 0.5f);
        }

        WorldGenerator generator = new WorldGenerator(seed, generatorType, generatorOptions);
        Chunk spawnChunk = new Chunk(0, 0);

        // 1. Chunk (0,0) synchron generieren (nur Datenstruktur, kein VRAM Upload)
        generator.generate(spawnChunk);

        byte waterId = de.delautrer.game.registry.Registries.BLOCKS.get(de.delautrer.Constants.NAMESPACE + ":water").getId();

        // 2. Spiralförmige Suche nach einem sicheren Spawnpunkt
        int centerX = 8, centerZ = 8;
        int radius = 8;
        for (int r = 0; r <= radius; r++) {
            for (int x = centerX - r; x <= centerX + r; x++) {
                for (int z = centerZ - r; z <= centerZ + r; z++) {
                    // Check only perimeter of the square for spiral search
                    if (Math.abs(x - centerX) != r && Math.abs(z - centerZ) != r) continue;
                    if (x < 0 || x >= Chunk.SIZE || z < 0 || z >= Chunk.SIZE) continue;

                    for (int y = Chunk.MAX_Y - 2; y > Chunk.MIN_Y; y--) {
                        byte blockId = spawnChunk.getBlock(x, y, z);
                        if (blockId != 0) {
                            if (blockId != waterId) {
                                de.delautrer.game.blocks.Block b = de.delautrer.game.blocks.BlockRegistry.get(blockId);
                                if (b.isSolid && !b.isTransparent && !b.isPassable) {
                                    return new Vector3f(x + 0.5f, y + 1.5f, z + 0.5f);
                                }
                            }
                            break; // Stop going down in this column
                        }
                    }
                }
            }
        }

        // Fallback, falls der Chunk komplett leer sein sollte oder nur aus Wasser besteht
        return new Vector3f(8.5f, 100.0f, 8.5f);
    }
}