package de.delautrer.game.blocks;

import de.delautrer.Constants;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private static final Map<String, Block> BLOCKS = new HashMap<>();
    private static final Block[] BLOCKS_BY_ID = new Block[256];

    public static final Block AIR = registerAir();

    public static final Block GRASS_BLOCK = register(1, "grass_block", new CubeBlock(true, false).setHardness(0.6f));
    public static final Block GRASS_BLOCK_SLABS = register(24, "grass_block_slabs", new SlabBlock().setHardness(0.6f));
    public static final Block GRASS_BLOCK_STAIRS = register(25, "grass_block_stairs", new StairBlock().setHardness(0.6f));

    public static final Block DIRT        = register(2, "dirt", new CubeBlock(true, false).setHardness(0.5f));
    public static final Block DIRT_SLABS = register(26, "dirt_slabs", new SlabBlock().setHardness(0.5f));
    public static final Block DIRT_STAIRS = register(27, "dirt_stairs", new StairBlock().setHardness(0.5f));

    public static final Block STONE       = register(3, "stone", new CubeBlock(true, false).setHardness(1.5f));
    public static final Block STONE_SLABS = register(28,  "stone_slabs", new SlabBlock().setHardness(1.5f));
    public static final Block STONE_STAIRS = register(29, "stone_stairs", new StairBlock().setHardness(1.5f));

    public static final Block WATER       = register(4, "water", new WaterBlock().setHardness(-1f));
    public static final Block GLASS       = register(5, "glass", new CubeBlock(true, true).setHardness(0.25f));
    public static final Block LEAVES      = register(6, "leaves", new LeavesBlock().setHardness(0.2f));
    public static final Block TORCH       = register(7, "torch", new TorchBlock().setLightEmission(14).setHardness(0.0001f));
    public static final Block BEDROCK     = register(8, "bedrock", new CubeBlock(true, false).setHardness(-1f));
    public static final Block GRAVEL      = register(9, "gravel", new CubeBlock(true, false).setHardness(0.6f));
    public static final Block SAND        = register(10, "sand", new CubeBlock(true, false).setHardness(0.5f));
    public static final Block LOG         = register(11, "log", new LogBlock().setHardness(2.0f));

    public static final Block GRASS         = register(12, "grass", new PlantBlock().setHardness(0.0001f));
    public static final Block SANDY_GRASS   = register(13, "sandy_grass", new PlantBlock().setHardness(0.0001f));
    public static final Block POPPY         = register(14, "poppy", new PlantBlock().setHardness(0.0001f));
    public static final Block DANDELION     = register(15, "dandelion", new PlantBlock().setHardness(0.0001f));
    public static final Block DOTTY         = register(16, "dotty", new PlantBlock().setHardness(0.0001f));
    public static final Block FAIRY_BELL    = register(17, "fairy_bell", new PlantBlock().setHardness(0.0001f));
    public static final Block RED_TULIP     = register(18, "red_tulip", new PlantBlock().setHardness(0.0001f));
    public static final Block PURPLE_TULIP  = register(19, "purple_tulip", new PlantBlock().setHardness(0.0001f));
    public static final Block MAVVINILIA    = register(23, "mavvinilia", new PlantBlock().setHardness(0.0001f));

    public static final Block PLANKS  = register(20, "planks", new CubeBlock(true, false).setHardness(2.0f));
    public static final Block STAIRS  = register(21, "stairs", new StairBlock().setHardness(2.0f));
    public static final Block SLABS   = register(22, "slabs", new SlabBlock().setHardness(2.0f));

    public static final Block BRICKS  = register(30, "bricks", new CubeBlock(true, false).setHardness(2.0f));
    public static final Block BRICKS_STAIRS  = register(31, "bricks_stairs", new StairBlock().setHardness(2.0f));
    public static final Block BRICKS_SLABS   = register(32, "bricks_slabs", new SlabBlock().setHardness(2.0f));

    public static final Block CHEST   = register(33, "chest", new ChestBlock().setHardness(1.42f));


    public static void init() {
        System.out.println("BlockRegistry initialized. " + BLOCKS.size() + " Blocks loaded.");
    }

    private static Block registerAir() {
        Block air = new Block(false, true, true, false) {
            @Override public void generateMesh(int x, int y, int z, de.delautrer.game.world.Chunk chunk, de.delautrer.game.world.ChunkManager cm) {}
            @Override public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) { return true; }
        };
        air.setId((byte) 0);
        BLOCKS_BY_ID[0] = air;
        BLOCKS.put(Constants.NAMESPACE + ":air", air);
        return air;
    }

    private static Block register(int idInt, String path, Block block) {
        byte id = (byte) idInt;
        String fullId = Constants.NAMESPACE + ":" + path;
        block.setId(id);
        BLOCKS.put(fullId, block);
        BLOCKS_BY_ID[id & 0xFF] = block;
        return block;
    }

    public static Block get(byte internalId) { return BLOCKS_BY_ID[internalId & 0xFF] != null ? BLOCKS_BY_ID[internalId & 0xFF] : AIR; }
    public static Block get(String fullId) { return BLOCKS.getOrDefault(fullId, AIR); }
    public static Map<String, Block> getAll() { return BLOCKS; }
}