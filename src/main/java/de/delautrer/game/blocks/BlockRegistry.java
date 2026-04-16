package de.delautrer.game.blocks;

import de.delautrer.engine.Constants;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private static final Map<String, Block> BLOCKS = new HashMap<>();

    private static final Block[] BLOCKS_BY_ID = new Block[256];

    private static byte nextInternalId = 1; // 0 ist reserviert für AIR

    public static final Block AIR = registerAir();

    public static final Block GRASS_BLOCK = register("grass_block", new CubeBlock(true, false, 0, 1, 2));
    public static final Block DIRT = register("dirt", new CubeBlock(true, false, 2, 2, 2));
    public static final Block STONE = register("stone", new CubeBlock(true, false, 3, 3, 3));
    public static final Block WATER = register("water", new WaterBlock());
    public static final Block GLASS = register("glass", new CubeBlock(true, true, 5, 5, 5));
    public static final Block LEAVES = register("leaves", new LeavesBlock( 6, 6, 6));
    public static final Block TORCH = register("torch", new TorchBlock(7));
    public static final Block BEDROCK = register("bedrock", new CubeBlock(true, false, 8,8,8));
    public static final Block GRAVEL = register("gravel", new CubeBlock(true, false, 9,9,9));
    public static final Block SAND = register("sand", new CubeBlock(true, false, 10,10,10));
    public static final Block LOG = register("log", new LogBlock(12, 11));

    public static final Block GRASS = register("grass", new PlantBlock(13));
    public static final Block SANDY_GRASS = register("sandy_grass", new PlantBlock(14));
    public static final Block POPPY = register("poppy", new PlantBlock(15));
    public static final Block DANDELION = register("dandelion", new PlantBlock(16));
    public static final Block DOTTY = register("dotty", new PlantBlock(17));
    public static final Block FAIRY_BELL = register("fairy_bell", new PlantBlock(18));
    public static final Block RED_TULIP = register("red_tulip", new PlantBlock(19));
    public static final Block PURPLE_TULIP = register("purple_tulip", new PlantBlock(20));

    public static final Block PLANKS = register("planks", new CubeBlock(true, false, 21, 21 ,21));
    public static final Block STAIRS = register("stairs", new StairBlock(21,21,21));
    public static final Block SLABS = register("slabs", new SlabBlock(21,21,21));

    public static final Block MAVVINILIA = register("mavvinilia", new PlantBlock(22));

    public static void init() {
        System.out.println("BlockRegistry initialized. " + BLOCKS.size() + " Blocks loaded.");
    }

    private static Block registerAir() {
        Block air = new Block(false, true, true, false) {
            @Override public void generateMesh(int x, int y, int z, de.delautrer.game.world.Chunk chunk, de.delautrer.game.world.ChunkManager cm) {}
            @Override public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
                return true;
            }
        };
        air.setId((byte) 0);
        BLOCKS_BY_ID[0] = air;
        BLOCKS.put(Constants.NAMESPACE + ":air", air);
        return air;
    }

    private static Block register(String path, Block block) {
        String fullId = Constants.NAMESPACE + ":" + path;

        if (BLOCKS.containsKey(fullId)) {
            throw new RuntimeException("Block-ID " + fullId + " ist bereits vergeben!");
        }
        if (nextInternalId == 0) { // Ein byte wrappt nach 255 auf 0
            throw new RuntimeException("Zu viele Blöcke registriert! (Maximal 255)");
        }

        byte id = nextInternalId++;
        block.setId(id);

        BLOCKS.put(fullId, block);
        BLOCKS_BY_ID[id & 0xFF] = block;

        return block;
    }

    // Für den Chunk (schnell über byte)
    public static Block get(byte internalId) {
        Block b = BLOCKS_BY_ID[internalId & 0xFF];
        return b != null ? b : AIR;
    }

    // Für Befehle, Inventar, Speichern (über String)
    public static Block get(String fullId) {
        return BLOCKS.getOrDefault(fullId, AIR);
    }
}