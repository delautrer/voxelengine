package de.delautrer.game.world;

import de.delautrer.game.world.persistence.BiomePalette;
import de.delautrer.game.world.persistence.WorldPalette;
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
            return new Vector3f(0.5f, de.delautrer.game.world.Chunk.MIN_Y + totalHeight + 1.0f, 0.5f);
        }

        WorldPalette blockPalette = WorldPalette.createFreshFromRegistry();
        BiomePalette biomePalette = BiomePalette.createFreshFromRegistry();

        WorldGenerator generator = new WorldGenerator(seed, generatorType, generatorOptions);
        generator.setPalettes(blockPalette, biomePalette);

        Chunk spawnChunk = new Chunk(0, 0);
        spawnChunk.setPalette(blockPalette);

        // 1. Chunk (0,0) synchron generieren
        generator.generate(spawnChunk);

        de.delautrer.game.blocks.Block waterBlock = de.delautrer.game.registry.Registries.BLOCKS.get(de.delautrer.Constants.NAMESPACE + ":water");

        // 2. Spiralförmige Suche nach einem sicheren Spawnpunkt auf solidem Boden
        int centerX = 8, centerZ = 8;
        int radius = 8;
        for (int r = 0; r <= radius; r++) {
            for (int x = centerX - r; x <= centerX + r; x++) {
                for (int z = centerZ - r; z <= centerZ + r; z++) {
                    if (Math.abs(x - centerX) != r && Math.abs(z - centerZ) != r) continue;
                    if (x < 0 || x >= Chunk.SIZE || z < 0 || z >= Chunk.SIZE) continue;

                    for (int y = Chunk.MAX_Y - 2; y >= Chunk.MIN_Y; y--) {
                        de.delautrer.game.blocks.Block b = spawnChunk.getBlock(x, y, z);
                        if (b != null && !b.isAir()) {
                            if (b != waterBlock && b.isSolid && !b.isPassable) {
                                return new Vector3f(x + 0.5f, y + 1.0f, z + 0.5f);
                            }
                            break; // Stop going down in this column
                        }
                    }
                }
            }
        }

        // Fallback: Höchster nicht-Luft-Block im Zentrum
        for (int y = Chunk.MAX_Y - 2; y >= Chunk.MIN_Y; y--) {
            de.delautrer.game.blocks.Block b = spawnChunk.getBlock(8, y, 8);
            if (b != null && !b.isAir()) {
                return new Vector3f(8.5f, y + 1.0f, 8.5f);
            }
        }

        return new Vector3f(8.5f, 65.0f, 8.5f);
    }
}