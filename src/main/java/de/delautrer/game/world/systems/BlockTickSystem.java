package de.delautrer.game.world.systems;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

import java.util.Random;

public class BlockTickSystem implements WorldSystem {

    private final Random random = new Random();
    private static final int RANDOM_TICKS_PER_CHUNK = 3;

    private byte grassId = -1;
    private byte dirtId = -1;
    private byte sandId = -1;
    private byte gravelId = -1;

    private void initIds() {
        if (grassId == -1) {
            grassId = BlockRegistry.get("grass_block").getId();
            dirtId = BlockRegistry.get("dirt").getId();
            sandId = BlockRegistry.get("sand").getId();
            gravelId = BlockRegistry.get("gravel").getId();
        }
    }

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        initIds();
        
        // Random Ticks
        for (Chunk chunk : world.getChunkManager().getLoadedChunks()) {
            for (int i = 0; i < RANDOM_TICKS_PER_CHUNK; i++) {
                int x = random.nextInt(Chunk.SIZE);
                int y = random.nextInt(Chunk.HEIGHT);
                int z = random.nextInt(Chunk.SIZE);

                int globalX = chunk.getWorldX() * Chunk.SIZE + x;
                int globalZ = chunk.getWorldZ() * Chunk.SIZE + z;

                byte blockId = chunk.getBlock(x, y, z);
                if (blockId == grassId) {
                    handleGrassDecay(world, globalX, y, globalZ);
                } else if (blockId == dirtId) {
                    handleGrassSpread(world, globalX, y, globalZ, chunk);
                }
            }
        }
    }

    private void handleGrassDecay(World world, int x, int y, int z) {
        if (y < Chunk.HEIGHT - 1) {
            byte blockAbove = world.getBlockAt(x, y + 1, z);
            Block bAbove = BlockRegistry.get(blockAbove);
            if (bAbove.isSolid && !bAbove.isTransparent) {
                world.setBlock(x, y, z, dirtId);
            }
        }
    }

    private void handleGrassSpread(World world, int x, int y, int z, Chunk chunk) {
        int xLocal = Math.floorMod(x, Chunk.SIZE);
        int zLocal = Math.floorMod(z, Chunk.SIZE);
        
        // Light check
        if (chunk.getSkyLightAt(xLocal, y + 1, zLocal, world.getChunkManager()) < 4 && 
            chunk.getBlockLightAt(xLocal, y + 1, zLocal, world.getChunkManager()) < 4) {
            return;
        }
        
        // Check if block above is transparent
        byte blockAboveId = world.getBlockAt(x, y + 1, z);
        if (BlockRegistry.get(blockAboveId).isSolid && !BlockRegistry.get(blockAboveId).isTransparent) {
            return;
        }

        // Check neighbors for grass
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    if (world.getBlockAt(x + dx, y + dy, z + dz) == grassId) {
                        // Found grass neighbor!
                        world.setBlock(x, y, z, grassId);
                        return;
                    }
                }
            }
        }
    }
}
