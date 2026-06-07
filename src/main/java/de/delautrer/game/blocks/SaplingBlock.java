package de.delautrer.game.blocks;

import de.delautrer.Constants;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.IntProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.TreeFeature;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.registry.NamespacedKey;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import java.util.Random;

public class SaplingBlock extends PlantBlock {
    public static final IntProperty STAGE = IntProperty.create("stage", 0, 1);

    private final String treeType;
    private final String logBlockName;
    private final String leavesBlockName;
    private final int minGrowthTime;
    private final int maxGrowthTime;

    public SaplingBlock(String name, int minGrowthTime, int maxGrowthTime) {
        super();
        this.minGrowthTime = minGrowthTime;
        this.maxGrowthTime = maxGrowthTime;
        String base = name.replace("_sapling", "");
        this.logBlockName = base + "_log";
        this.leavesBlockName = base + "_leaves";

        // Map to specific TreeFeature types
        if (base.equals("oak")) {
            this.treeType = "alpha_oak";
        } else if (base.equals("birch")) {
            this.treeType = "alpha_birch";
        } else if (base.equals("pine")) {
            this.treeType = "alpha_pine";
        } else if (base.equals("willow")) {
            this.treeType = "alpha_willow";
        } else if (base.equals("baobab")) {
            this.treeType = "alpha_baobab";
        } else if (base.equals("mahogany")) {
            this.treeType = "alpha_mahogany";
        } else if (base.equals("palm")) {
            this.treeType = "alpha_palm";
        } else {
            this.treeType = "alpha_oak";
        }
    }

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
        world.getTickScheduler().scheduleTick(new Vector3i(x, y, z), this, delayTicks);
    }

    public void grow(World world, int x, int y, int z, Random random) {
        BlockState state = world.getBlockState(x, y, z);
        if (state.getBlock() != this) return;

        // Ensure sapling is placed on valid ground before growing
        byte belowId = world.getBlockAt(x, y - 1, z);
        Block belowBlock = BlockRegistry.get(belowId);
        NamespacedKey belowKey = BlockRegistry.REGISTRY.getKey(belowBlock);
        if (belowKey == null) return;
        String belowName = belowKey.getKey();
        boolean isSoil = belowName.equals("grass_block") || belowName.equals("dirt");
        if (!isSoil) return;

        int stage = state.getValue(STAGE);
        if (stage < 1) {
            // Stage 0 -> 1
            world.setBlockWithState(x, y, z, this.getId(), state.with(STAGE, 1).getStateId(), false);
            scheduleGrowthIfAbsent(world, x, y, z);
        } else {
            // Stage 1 -> Grow Tree
            // Turn grass block below the sapling to dirt
            byte grassBlockId = Registries.BLOCKS.get(Constants.NAMESPACE + ":grass_block").getId();
            byte dirtId = Registries.BLOCKS.get(Constants.NAMESPACE + ":dirt").getId();
            if (world.getBlockAt(x, y - 1, z) == grassBlockId) {
                world.setBlock(x, y - 1, z, dirtId);
            }

            // Temporarily clear the sapling block so the generator starts cleanly
            world.setBlock(x, y, z, (byte) 0, false);

            byte logId = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + logBlockName).getId();
            byte leavesId = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + leavesBlockName).getId();

            String actualTreeType = this.treeType;
            if (actualTreeType.equals("alpha_pine") && random.nextBoolean()) {
                actualTreeType = "alpha_tall_pine";
            }

            long treeSeed = random.nextLong();
            TreeFeature.generate(world, x, y, z, treeSeed, actualTreeType, logId, leavesId);
        }
    }
}
