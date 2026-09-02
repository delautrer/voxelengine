package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.IntProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.TreeFeature;
import de.delautrer.game.world.generation.feature.ConfiguredFeature;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.registry.NamespacedKey;
import org.joml.Vector3i;
import java.util.List;
import java.util.Random;

public class SaplingBlock extends PlantBlock {
    public static final IntProperty STAGE = IntProperty.create("stage", 0, 1);

    private final String treeFeatureKey;
    private final String logBlockKey;
    private final String leavesBlockKey;
    private final int minGrowthTime;
    private final int maxGrowthTime;

    public SaplingBlock(String treeFeatureKey, String logBlockKey, String leavesBlockKey, int minGrowthTime, int maxGrowthTime) {
        super();
        this.minGrowthTime = minGrowthTime;
        this.maxGrowthTime = maxGrowthTime;
        this.treeFeatureKey = treeFeatureKey;
        this.logBlockKey = logBlockKey;
        this.leavesBlockKey = leavesBlockKey;
    }

    public String getTreeFeatureKey() { return treeFeatureKey; }
    public String getLogBlockKey() { return logBlockKey; }
    public String getLeavesBlockKey() { return leavesBlockKey; }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        super.appendProperties(properties);
        properties.add(STAGE);
    }

    @Override
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, Player player) {
        super.onBlockPlaced(world, pos, state, player);
        scheduleGrowthIfAbsent(world, pos.x, pos.y, pos.z);
    }

    @Override
    public void scheduledTick(World world, int x, int y, int z) {
        grow(world, x, y, z, new Random());
    }

    public void scheduleGrowthIfAbsent(World world, int x, int y, int z) {
        Random random = new Random();
        int minTicks = minGrowthTime * 20;
        int maxTicks = maxGrowthTime * 20;
        int delayTicks = minTicks;
        if (maxTicks > minTicks) {
            delayTicks += random.nextInt(maxTicks - minTicks + 1);
        }
        world.getTickScheduler().scheduleTick(new de.delautrer.game.world.BlockPos(x, y, z), this, delayTicks);
    }

    public void grow(World world, int x, int y, int z, Random random) {
        BlockState state = world.getBlockState(x, y, z);
        if (state.getBlock() != this) return;

        Block belowBlock = world.getBlock(x, y - 1, z);
        NamespacedKey belowKey = Registries.BLOCKS.getKey(belowBlock);
        if (belowKey == null) return;
        String belowPath = belowKey.getKey();
        boolean isSoil = belowPath.equals("grass_block") || belowPath.equals("dirt") || belowPath.equals("moss");
        if (!isSoil) return;

        int stage = state.getValue(STAGE);
        if (stage < 1) {
            world.setBlockState(x, y, z, state.with(STAGE, 1));
            scheduleGrowthIfAbsent(world, x, y, z);
        } else {
            Block grassBlock = Registries.BLOCKS.get("veinstride:grass_block");
            Block dirtBlock = Registries.BLOCKS.get("veinstride:dirt");
            if (world.getBlock(x, y - 1, z) == grassBlock) {
                world.setBlock(x, y - 1, z, dirtBlock);
            }

            world.setBlock(x, y, z, Registries.BLOCKS.get("veinstride:air"));

            Block logBlock = Registries.BLOCKS.get(logBlockKey);
            Block leavesBlock = Registries.BLOCKS.get(leavesBlockKey);
            if (logBlock == null) logBlock = Registries.BLOCKS.get("veinstride:oak_log");
            if (leavesBlock == null) leavesBlock = Registries.BLOCKS.get("veinstride:oak_leaves");

            if (treeFeatureKey != null) {
                NamespacedKey featKey = NamespacedKey.fromString(treeFeatureKey);
                ConfiguredFeature feature = FeatureRegistry.getConfiguredFeature(featKey);
                if (feature != null) {
                    feature.generate(world, x, y, z, random.nextLong());
                    return;
                }
            }

            TreeFeature.TreeShape shape = TreeFeature.TreeShape.STANDARD;
            long treeSeed = random.nextLong();
            TreeFeature.generate(world, x, y, z, treeSeed, shape, logBlock, leavesBlock, 4, 3);
        }
    }
}
