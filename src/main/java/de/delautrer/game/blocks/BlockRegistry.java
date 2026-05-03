package de.delautrer.game.blocks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.delautrer.Constants;
import de.delautrer.game.blocks.data.BlockDefinition;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockRegistry {
    private static final Map<String, Block> BLOCKS = new HashMap<>();
    private static final Block[] BLOCKS_BY_ID = new Block[256];
    private static final Gson GSON = new Gson();

    // --- ALTE STATISCHE KONSTANTEN FÜR DEN ÜBERGANG ---
    public static Block AIR;
    public static Block GRASS_BLOCK;
    public static Block GRASS_BLOCK_SLABS;
    public static Block GRASS_BLOCK_STAIRS;
    public static Block DIRT;
    public static Block DIRT_SLABS;
    public static Block DIRT_STAIRS;
    public static Block STONE;
    public static Block STONE_SLABS;
    public static Block STONE_STAIRS;
    public static Block WATER;
    public static Block GLASS;
    public static Block LEAVES;
    public static Block TORCH;
    public static Block BEDROCK;
    public static Block GRAVEL;
    public static Block SAND;
    public static Block LOG;
    public static Block GRASS;
    public static Block SANDY_GRASS;
    public static Block POPPY;
    public static Block DANDELION;
    public static Block DOTTY;
    public static Block FAIRY_BELL;
    public static Block RED_TULIP;
    public static Block PURPLE_TULIP;
    public static Block MAVVINILIA;
    public static Block PLANKS;
    public static Block STAIRS;
    public static Block SLABS;
    public static Block BRICKS;
    public static Block BRICKS_STAIRS;
    public static Block BRICKS_SLABS;
    public static Block CHEST;

    private static boolean isInitialized = false;
    static {
        init();
    }

    public static void init() {
        if (isInitialized) return; // Wenn schon geladen, abbrechen
        isInitialized = true;
        System.out.println("[BlockRegistry] Initializing...");

        registerAir();
        loadBlocksFromJson();

        // Konstanten nachträglich mit den geladenen Daten füttern
        String ns = Constants.NAMESPACE + ":";
        GRASS_BLOCK = get(ns + "grass_block");
        GRASS_BLOCK_SLABS = get(ns + "grass_block_slabs");
        GRASS_BLOCK_STAIRS = get(ns + "grass_block_stairs");
        DIRT = get(ns + "dirt");
        DIRT_SLABS = get(ns + "dirt_slabs");
        DIRT_STAIRS = get(ns + "dirt_stairs");
        STONE = get(ns + "stone");
        STONE_SLABS = get(ns + "stone_slabs");
        STONE_STAIRS = get(ns + "stone_stairs");
        WATER = get(ns + "water");
        GLASS = get(ns + "glass");
        LEAVES = get(ns + "leaves");
        TORCH = get(ns + "torch");
        BEDROCK = get(ns + "bedrock");
        GRAVEL = get(ns + "gravel");
        SAND = get(ns + "sand");
        LOG = get(ns + "log");
        GRASS = get(ns + "grass");
        SANDY_GRASS = get(ns + "sandy_grass");
        POPPY = get(ns + "poppy");
        DANDELION = get(ns + "dandelion");
        DOTTY = get(ns + "dotty");
        FAIRY_BELL = get(ns + "fairy_bell");
        RED_TULIP = get(ns + "red_tulip");
        PURPLE_TULIP = get(ns + "purple_tulip");
        MAVVINILIA = get(ns + "mavvinilia");
        PLANKS = get(ns + "planks");
        STAIRS = get(ns + "stairs");
        SLABS = get(ns + "slabs");
        BRICKS = get(ns + "bricks");
        BRICKS_STAIRS = get(ns + "bricks_stairs");
        BRICKS_SLABS = get(ns + "bricks_slabs");
        CHEST = get(ns + "chest");

        System.out.println("[BlockRegistry] " + BLOCKS.size() + " Blocks loaded.");
    }

    private static void registerAir() {
        AIR = new Block(false, true, true, false) {
            @Override public void generateMesh(int x, int y, int z, de.delautrer.game.world.Chunk chunk, de.delautrer.game.world.ChunkManager cm) {}
            @Override public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) { return true; }
        };
        AIR.setId((byte) 0);
        BLOCKS_BY_ID[0] = AIR;
        BLOCKS.put(Constants.NAMESPACE + ":air", AIR);
    }

    private static void loadBlocksFromJson() {
        try {
            // Lesen der Datei. getResourceAsStream funktioniert sowohl in der IDE als auch in der .exe (.jar)
            InputStream is = BlockRegistry.class.getResourceAsStream("/assets/data/blocks.json");
            if (is == null) {
                System.err.println("[BlockRegistry] Error: /assets/data/blocks.json nicht gefunden!");
                return;
            }

            Type listType = new TypeToken<List<BlockDefinition>>(){}.getType();
            List<BlockDefinition> definitions = GSON.fromJson(new InputStreamReader(is), listType);

            for (BlockDefinition def : definitions) {
                Block block = createBlockInstance(def);
                if (block != null) {
                    register((byte) def.id, def.name, block, def);
                }
            }
        } catch (Exception e) {
            System.err.println("[BlockRegistry] Fehler beim Laden der blocks.json");
            e.printStackTrace();
        }
    }

    private static Block createBlockInstance(BlockDefinition def) {
        if (def.type == null) {
            System.err.println("[BlockRegistry] Block '" + def.name + "' hat keinen type!");
            return null;
        }

        switch (def.type.toLowerCase()) {
            case "cube":   return new CubeBlock(def.isSolid, def.isTransparent);
            case "slab":   return new SlabBlock();
            case "stair":  return new StairBlock();
            case "plant":  return new PlantBlock();
            case "water":  return new WaterBlock();
            case "leaves": return new LeavesBlock();
            case "torch":  return new TorchBlock();
            case "log":    return new LogBlock();
            case "chest":  return new ChestBlock();
            default:
                System.err.println("[BlockRegistry] Unbekannter Block Type: " + def.type + " bei " + def.name);
                return null;
        }
    }

    private static void register(byte id, String path, Block block, BlockDefinition def) {
        String fullId = Constants.NAMESPACE + ":" + path;
        block.setId(id);
        block.setHardness(def.hardness);

        if (def.lightEmission > 0) {
            block.setLightEmission(def.lightEmission);
        }

        // Sound und Loot-Table (Nutzt die überschriebenen Werte, falls vorhanden, sonst Fallback)
        block.setSoundMaterialName(def.soundMaterial != null ? def.soundMaterial : path);
        block.setLootTable(def.customLootTable != null ? def.customLootTable : "blocks/" + path + ".json");

        BLOCKS.put(fullId, block);
        BLOCKS_BY_ID[id & 0xFF] = block;
    }

    public static Block get(byte internalId) { return BLOCKS_BY_ID[internalId & 0xFF] != null ? BLOCKS_BY_ID[internalId & 0xFF] : AIR; }
    public static Block get(String fullId) {
        return BLOCKS.getOrDefault(fullId.startsWith(Constants.NAMESPACE + ":") ? fullId : Constants.NAMESPACE + ":" + fullId, AIR);
    }
    public static Map<String, Block> getAll() { return BLOCKS; }
}