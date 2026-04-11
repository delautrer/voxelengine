package de.delautrer.game.world;

public class WorldGenerator {

    public void generate(Chunk chunk) {
        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                float noise1 = NoiseGenerator.getNoise(realX * 0.02f, realZ * 0.02f) * 15.0f;
                float noise2 = NoiseGenerator.getNoise(realX * 0.1f, realZ * 0.1f) * 3.0f;

                int surfaceY = 25 + (int) (noise1 + noise2);

                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (y == surfaceY) chunk.setBlock(x, y, z, (byte)1);
                    else if (y < surfaceY && y > surfaceY - 4) chunk.setBlock(x, y, z, (byte)2);
                    else if (y <= surfaceY - 4) chunk.setBlock(x, y, z, (byte)3);
                    else chunk.setBlock(x, y, z, (byte)0);
                }
            }
        }
    }
}