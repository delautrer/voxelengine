package de.delautrer.game.blocks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.delautrer.Constants;
import de.delautrer.game.blocks.data.BlockDefinition;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3f;
import org.joml.Vector3i;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class BlockRegistry {
    public static final Registry<Block> REGISTRY = new Registry<>();
    private static final Block[] BLOCKS_BY_ID = new Block[256];
    private static final Gson GSON = new Gson();

    private static boolean isInitialized = false;
    static {
        init();
    }

    public static void init() {
        if (isInitialized)
            return; // Wenn schon geladen, abbrechen
        isInitialized = true;
        System.out.println("[BlockRegistry] Initializing...");

        registerAir();
        loadBlocksFromJson();

        System.out.println("[BlockRegistry] " + REGISTRY.size() + " Blocks loaded.");
    }

    private static void registerAir() {
        Block air = new Block(false, true, true, false) {
            @Override
            public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
            }

            @Override
            public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
                return true;
            }
        };
        air.setId((byte) 0);
        BLOCKS_BY_ID[0] = air;
        REGISTRY.register(new NamespacedKey(Constants.NAMESPACE, "air"), air);
    }

    private static void loadBlocksFromJson() {
        try {
            // Lesen der Datei. getResourceAsStream funktioniert sowohl in der IDE als auch
            // in der .exe (.jar)
            InputStream is = BlockRegistry.class.getResourceAsStream("/assets/data/blocks.json");
            if (is == null) {
                System.err.println("[BlockRegistry] Error: /assets/data/blocks.json nicht gefunden!");
                return;
            }

            Type listType = new TypeToken<List<BlockDefinition>>() {
            }.getType();
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
            case "cube":
                return new CubeBlock(def.isSolid, def.isTransparent);
            case "slab":
                return new SlabBlock(def.isSolid, true);
            case "stair":
                return new StairBlock(def.isSolid, true);
            case "plant":
                return new PlantBlock();
            case "water":
                return new WaterBlock();
            case "leaves":
                return new LeavesBlock();
            case "torch":
                return new TorchBlock();
            case "log":
                return new LogBlock();
            case "chest":
                return new ChestBlock();
            case "trapdoor":
                return new TrapdoorBlock();
            case "door":
                return new DoorBlock();
            case "gravity":
                return new GravityBlock();
            default:
                System.err.println("[BlockRegistry] Unbekannter Block Type: " + def.type + " bei " + def.name);
                return null;
        }
    }

    private static void register(byte id, String path, Block block, BlockDefinition def) {
        // String fullId = Constants.NAMESPACE + ":" + path;
        block.setId(id);
        block.setHardness(def.hardness);

        if (def.lightEmission > 0) {
            block.setLightEmission(def.lightEmission);
        }

        if (def.opacity != -1) {
            block.setOpacity(def.opacity);
        }

        // Sound und Loot-Table (Nutzt die überschriebenen Werte, falls vorhanden, sonst
        // Fallback)
        block.setSoundMaterialName(def.soundMaterial != null ? def.soundMaterial : path);
        block.setLootTable(def.customLootTable != null ? def.customLootTable : "blocks/" + path + ".json");
        block.setCategory(def.category);

        NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
        REGISTRY.register(key, block);
        BLOCKS_BY_ID[id & 0xFF] = block;
    }

    public static Block get(byte internalId) {
        return BLOCKS_BY_ID[internalId & 0xFF] != null ? BLOCKS_BY_ID[internalId & 0xFF]
                : get(Constants.NAMESPACE + ":air");
    }

    public static Block get(String fullId) {
        Block b = REGISTRY.get(fullId);
        return b != null ? b : get(Constants.NAMESPACE + ":air");
    }

    public static Map<String, Block> getAll() {
        return REGISTRY.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    }
}
