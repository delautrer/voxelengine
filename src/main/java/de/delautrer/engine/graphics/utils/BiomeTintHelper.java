package de.delautrer.engine.graphics.utils;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.generation.biome.Biome;

public class BiomeTintHelper {

    // Referenz-Farbe #478a15 (r=0.27843137, g=0.5411765, b=0.08235294)
    private static final float REF_R = 71.0f / 255.0f;
    private static final float REF_G = 138.0f / 255.0f;
    private static final float REF_B = 21.0f / 255.0f;
    private static final String DEFAULT_HEX = "#478a15";

    public static class Tint {
        public final float r;
        public final float g;
        public final float b;

        public Tint(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static final Tint DEFAULT_TINT = new Tint(1.0f, 1.0f, 1.0f);

    public static Tint getGrassTint(Biome biome) {
        String hex = (biome != null && biome.effects != null && biome.effects.containsKey("grass_color"))
                ? biome.effects.get("grass_color")
                : DEFAULT_HEX;
        return calculateTint(hex);
    }

    public static Tint getFoliageTint(Biome biome) {
        // foliage_color vorübergehend deaktiviert -> benutze grass_color
        /*
        if (biome != null && biome.effects != null && biome.effects.containsKey("foliage_color")) {
            return calculateTint(biome.effects.get("foliage_color"));
        }
        */
        return getGrassTint(biome);
    }

    /**
     * Berechnet den geglätteten Gras-Tint (Biome Blend) über ein 5x5-Fenster um (x, z).
     */
    public static Tint getBlendedGrassTint(Chunk chunk, int x, int z, ChunkManager cm) {
        if (chunk == null) return DEFAULT_TINT;

        float sumR = 0.0f;
        float sumG = 0.0f;
        float sumB = 0.0f;
        int count = 0;

        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int tx = x + dx;
                int tz = z + dz;
                Biome b = null;

                if (tx >= 0 && tx < 16 && tz >= 0 && tz < 16) {
                    b = chunk.getBiome(tx, tz);
                } else if (cm != null) {
                    int worldX = (chunkX << 4) + tx;
                    int worldZ = (chunkZ << 4) + tz;
                    Chunk nChunk = cm.getChunkAtBlock(worldX, 0, worldZ);
                    if (nChunk != null) {
                        b = nChunk.getBiome(Math.floorMod(worldX, 16), Math.floorMod(worldZ, 16));
                    }
                }

                if (b == null) {
                    b = chunk.getBiome(x, z);
                }

                Tint t = getGrassTint(b);
                sumR += t.r;
                sumG += t.g;
                sumB += t.b;
                count++;
            }
        }

        return new Tint(sumR / count, sumG / count, sumB / count);
    }

    /**
     * Geglätteter Foliage-Tint (nutzt ebenfalls die geglättete Grasfarbe).
     */
    public static Tint getBlendedFoliageTint(Chunk chunk, int x, int z, ChunkManager cm) {
        return getBlendedGrassTint(chunk, x, z, cm);
    }

    public static Tint calculateTint(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() < 7) {
            return DEFAULT_TINT;
        }
        try {
            int rInt = Integer.parseInt(hexColor.substring(1, 3), 16);
            int gInt = Integer.parseInt(hexColor.substring(3, 5), 16);
            int bInt = Integer.parseInt(hexColor.substring(5, 7), 16);

            float targetR = rInt / 255.0f;
            float targetG = gInt / 255.0f;
            float targetB = bInt / 255.0f;

            float tintR = Math.min(2.5f, Math.max(0.25f, targetR / REF_R));
            float tintG = Math.min(2.5f, Math.max(0.25f, targetG / REF_G));
            float tintB = Math.min(2.5f, Math.max(0.25f, targetB / REF_B));

            return new Tint(tintR, tintG, tintB);
        } catch (Exception e) {
            return DEFAULT_TINT;
        }
    }
}
