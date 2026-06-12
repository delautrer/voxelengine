package de.delautrer.game.world.generation;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.WorldGenerator;

public class FlatChunkGenerator implements IChunkGenerator {

    private final byte[] layers;

    public FlatChunkGenerator(String options) {
        if (options == null || options.trim().isEmpty()) {
            options = "1xbedrock;3xstone;2xdirt;1xgrass_block";
        }
        
        int totalHeight = 0;
        String[] parts = options.split(";");
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String[] split = part.split("x");
            if (split.length == 2) {
                try {
                    totalHeight += Integer.parseInt(split[0]);
                } catch (NumberFormatException ignored) {}
            }
        }

        layers = new byte[totalHeight];
        int currentY = 0;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            String[] split = part.split("x");
            if (split.length == 2) {
                try {
                    int count = Integer.parseInt(split[0]);
                    String blockName = split[1].trim();
                    if (!blockName.contains(":")) {
                        blockName = de.delautrer.Constants.NAMESPACE + ":" + blockName;
                    }
                    byte blockId = BlockRegistry.get(blockName).getId();
                    
                    for (int i = 0; i < count; i++) {
                        if (currentY < totalHeight) {
                            layers[currentY++] = blockId;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse flat world layer: " + part);
                }
            }
        }
    }

    @Override
    public void generate(Chunk chunk, WorldGenerator worldGenerator) {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int i = 0; i < layers.length; i++) {
                    int y = Chunk.MIN_Y + i;
                    if (y < Chunk.MAX_Y) {
                        chunk.setBlock(x, y, z, layers[i]);
                    }
                }
            }
        }
    }
}
