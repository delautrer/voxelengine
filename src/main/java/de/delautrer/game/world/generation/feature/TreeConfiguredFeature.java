package de.delautrer.game.world.generation.feature;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.biome.TreeFeature;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;

import java.util.Random;

public class TreeConfiguredFeature extends ConfiguredFeature {
    private final TreeFeature.TreeShape shape;
    private final Block log;
    private final Block leaves;
    private final int baseHeight;
    private final int heightVariation;

    public TreeConfiguredFeature(TreeFeature.TreeShape shape, Block log, Block leaves, int baseHeight, int heightVariation) {
        super(log);
        this.shape = shape;
        this.log = log;
        this.leaves = leaves;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    public void generate(Chunk chunk, int lx, int y, int lz, int worldX, int worldZ, Random rand, PlacementModifier modifier) {
        long treeSeed = rand.nextLong();
        TreeFeature.generate(chunk, null, worldX, y, worldZ, treeSeed, shape, log, leaves, baseHeight, heightVariation);
    }

    @Override
    public void generate(Chunk chunk, de.delautrer.game.world.WorldGenerator wg, int worldX, int worldY, int worldZ, long seed) {
        TreeFeature.generate(chunk, wg, worldX, worldY, worldZ, seed, shape, log, leaves, baseHeight, heightVariation);
    }

    @Override
    public void generate(de.delautrer.game.world.World world, int worldX, int worldY, int worldZ, long seed) {
        TreeFeature.generate(world, worldX, worldY, worldZ, seed, shape, log, leaves, baseHeight, heightVariation);
    }
}
