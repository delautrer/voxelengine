package de.delautrer.game.blocks;

import com.google.gson.Gson;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.data.BlockDefinition;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registry;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class BlockRegistry {
    public static final Registry<Block> REGISTRY = new Registry<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        isInitialized = true;

        BlockTypeRegistry.initBuiltinTypes();
        registerAir();
        loadBlocksFromJson();

        System.out.println(REGISTRY.size() + " Blocks loaded.");
    }

    private static void registerAir() {
        Block air = new Block(false, true, true, false) {


            @Override
            public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
                return true;
            }
        };
        NamespacedKey airKey = new NamespacedKey(Constants.NAMESPACE, "air");
        REGISTRY.register(airKey, air);
    }

    private static void loadBlocksFromJson() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/blocks", ".json");
        for (String file : files) {
            String path = file.substring(0, file.length() - 5).replace('\\', '/');
            NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/blocks/" + file);
                BlockDefinition def = GSON.fromJson(reader, BlockDefinition.class);
                if (def.name == null) def.name = path;

                Block block = BlockTypeRegistry.create(def.type, def, key);
                registerBlock(key, block, def);
            } catch (Exception e) {
                System.err.println("Fehler beim Laden von Block: " + file);
                throw new IllegalStateException("Failed to load block file: " + file, e);
            }
        }
    }

    private static void registerBlock(NamespacedKey key, Block block, BlockDefinition def) {
        block.setHardness(def.hardness);
        if (def.lightEmission > 0) block.setLightEmission(def.lightEmission);
        if (def.opacity != -1) block.setOpacity(def.opacity);

        block.setSoundMaterialName(def.soundMaterial != null ? def.soundMaterial : key.getKey());
        block.setLootTable(def.customLootTable != null ? def.customLootTable : "blocks/" + key.getKey() + ".json");
        block.setCategory(def.category);

        try {
            block.setMinToolTier(de.delautrer.game.items.ToolTier.valueOf(def.minToolTier.toUpperCase()));
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown ToolTier: " + def.minToolTier + " for block " + key);
        }

        REGISTRY.register(key, block);
    }

    public static Block get(String fullId) {
        Block b = REGISTRY.get(fullId);
        return b != null ? b : get(Constants.NAMESPACE + ":air");
    }

    public static Map<String, Block> getAll() {
        return REGISTRY.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    }
}
