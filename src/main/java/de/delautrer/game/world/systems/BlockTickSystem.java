package de.delautrer.game.world.systems;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.LeavesBlock;
import de.delautrer.game.blocks.LogBlock;
import de.delautrer.game.blocks.SaplingBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.Random;

public class BlockTickSystem implements WorldSystem {

    private final Random random = new Random();
    private static final int RANDOM_TICKS_PER_CHUNK = 12;

    private byte grassId = -1;
    private byte dirtId = -1;
    private byte sandId = -1;
    private byte gravelId = -1;

    private void initIds() {
        if (grassId == -1) {
            grassId = BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":grass_block").getId();
            dirtId = BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":dirt").getId();
            sandId = BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":sand").getId();
            gravelId = BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":gravel").getId();
        }
    }

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        org.joml.Vector3d pos = localPlayer.getCamera().getPosition();
        int radius = 16;
        int count = (int) (20000 * deltaTime);

        for (int i = 0; i < count; i++) {
            int x = (int) pos.x + random.nextInt(radius * 2) - radius;
            int y = (int) pos.y + random.nextInt(radius * 2) - radius;
            int z = (int) pos.z + random.nextInt(radius * 2) - radius;

            if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y) continue;

            byte blockId = world.getBlockAt(x, y, z);
            if (blockId != 0) {
                Block block = BlockRegistry.get(blockId);
                block.randomDisplayTick(world, new Vector3i(x, y, z), random);
            }
        }
    }

    @Override
    public void onTick(World world, LocalPlayer localPlayer) {
        initIds();
        
        // Random Ticks (3 pro Chunk pro Spiel-Tick)
        for (Chunk chunk : world.getChunkManager().getLoadedChunks()) {
            for (int i = 0; i < RANDOM_TICKS_PER_CHUNK; i++) {
                int x = random.nextInt(Chunk.SIZE);
                int y = Chunk.MIN_Y + random.nextInt(Chunk.HEIGHT);
                int z = random.nextInt(Chunk.SIZE);

                int globalX = chunk.getWorldX() * Chunk.SIZE + x;
                int globalZ = chunk.getWorldZ() * Chunk.SIZE + z;

                byte blockId = chunk.getBlock(x, y, z);
                if (blockId == grassId) {
                    handleGrassDecay(world, globalX, y, globalZ);
                } else if (blockId == dirtId) {
                    handleGrassSpread(world, globalX, y, globalZ, chunk);
                } else {
                    Block block = BlockRegistry.get(blockId);
                    if (block instanceof LeavesBlock) {
                        handleLeavesDecay(world, globalX, y, globalZ);
                    } else if (block instanceof SaplingBlock) {
                        ((SaplingBlock) block).scheduleGrowthIfAbsent(world, globalX, y, globalZ);
                    }
                }
            }
        }
    }

    private void handleGrassDecay(World world, int x, int y, int z) {
        if (y < Chunk.MAX_Y - 1) {
            byte blockAbove = world.getBlockAt(x, y + 1, z);
            Block bAbove = BlockRegistry.get(blockAbove);
            if (bAbove.isSolid && !bAbove.isTransparent) {
                world.setBlock(x, y, z, dirtId, false);
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
                        world.setBlock(x, y, z, grassId, false);
                        return;
                    }
                }
            }
        }
    }
    private void handleLeavesDecay(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(x, y, z);
        if (!(state.getBlock() instanceof LeavesBlock)) return;

        // Spieler-platziertes Laub zerfällt nicht
        if (state.getValue(LeavesBlock.PERSISTENT)) return;

        // Prüfe nach Holz in der Nähe (Radius 6 für große Bäume wie Baobab)
        if (isLogNearby(world, x, y, z, 6)) return;

        // Kein Holz -> Drop Items und löschen
        dropBlockAsItem(world, x, y, z, state);
        world.setBlock(x, y, z, (byte) 0);
    }

    private boolean isLogNearby(World world, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Manhattan-Distanz Optimierung
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius + 2) continue;

                    Block b = world.getBlockState(x + dx, y + dy, z + dz).getBlock();
                    if (b instanceof LogBlock) return true;
                }
            }
        }
        return false;
    }

    private void dropBlockAsItem(World world, int x, int y, int z, BlockState state) {
        Block block = state.getBlock();
        String lootPath = block.getLootTable();

        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();
                for (ItemStack stack : drops) {
                    Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    Vector3f dropVel = new Vector3f(
                            (float) (Math.random() - 0.5) * 1.5f,
                            1.5f,
                            (float) (Math.random() - 0.5) * 1.5f);
                    world.spawnEntity(new de.delautrer.game.entity.ItemEntity(stack, dropPos, dropVel));
                }
            }
        }
    }
}
