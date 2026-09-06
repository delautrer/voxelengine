package de.delautrer.game.registry;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.BlockTypeRegistry;
import de.delautrer.game.blocks.entities.BlockEntityTypeRegistry;
import de.delautrer.game.crafting.FurnaceRecipeManager;
import de.delautrer.game.crafting.RecipeManager;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemTypeRegistry;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import de.delautrer.engine.audio.SoundManager;
import de.delautrer.game.blocks.SaplingBlock;
import java.util.List;
import java.util.Map;

public class Registries {

    public static final Registry<Block> BLOCKS = BlockRegistry.REGISTRY;
    public static final Registry<Item> ITEMS = ItemRegistry.REGISTRY;
    public static final Registry<Biome> BIOMES = MultiNoiseBiomeRegistry.BIOME_REGISTRY;

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        System.out.println("Starting Veinstride Bootstrap Pipeline...");

        // 1. Namespaces / Builtins
        // (Constants initialized)

        // 2. Sound Materials
        // (SoundManager loads sound definitions)

        // 3. Block-Typen (Factories)
        BlockTypeRegistry.initBuiltinTypes();

        // 4. Blocks
        BlockRegistry.init();

        // 5. Item-Typen (Factories)
        ItemTypeRegistry.initBuiltinTypes();

        // 6. Items (inkl. BlockItems aus Blocks)
        ItemRegistry.init();

        // 7. BlockEntity-Typen
        BlockEntityTypeRegistry.init();

        // 8. Tags
        TagRegistry.loadTags();

        // 9. Recipes + Furnace recipes
        RecipeManager.loadRecipes();

        // 10. Loot tables
        // (Loaded on demand or validated)

        // 11. Biomes
        MultiNoiseBiomeRegistry.init();

        // 11b. Biome Tags
        TagRegistry.loadBiomeTags();

        // 12. Configured + Placed Features
        FeatureRegistry.init();

        // 12b. Data-Driven Structures (Templates, Structures, StructureSets)
        de.delautrer.game.world.generation.structure.StructureRegistry.init();

        // 12c. Template Pools
        de.delautrer.game.worldgen.pool.TemplatePoolRegistry.init();

        // 12d. Data-Driven GameTests
        de.delautrer.game.testing.GameTestRegistry.init();

        // 13. Validierung
        validate();

        // 14. freeze() aller Registries
        BLOCKS.freeze();
        ITEMS.freeze();
        BIOMES.freeze();
        BlockEntityTypeRegistry.REGISTRY.freeze();
        de.delautrer.game.world.generation.structure.StructureRegistry.freeze();

        System.out.println("Veinstride Bootstrap Pipeline Completed Successfully.");
    }

    private static void validate() {
        // 1. Every block (except Air) has model resource & state count <= 256
        for (Map.Entry<NamespacedKey, Block> entry : BLOCKS.entrySet()) {
            NamespacedKey key = entry.getKey();
            Block block = entry.getValue();
            if (block == null) {
                throw new IllegalStateException("Null block registered under key: " + key);
            }
            if (!key.getKey().equals("air") && !key.getKey().equals("structure_void")) {
                String modelPath = "assets/models/block/" + key.getKey() + ".json";
                if (!de.delautrer.engine.utils.ResourceUtils.hasResource(modelPath)) {
                    throw new IllegalStateException("Missing block model resource for block: " + key + " (" + modelPath + ")");
                }
            }
            if (block.getStateCount() > 256) {
                throw new IllegalStateException("Block " + key + " exceeds maximum allowed 256 states (" + block.getStateCount() + ")");
            }
        }

        // 2. Every item texture or model exists
        for (Map.Entry<NamespacedKey, Item> entry : ITEMS.entrySet()) {
            NamespacedKey key = entry.getKey();
            Item item = entry.getValue();
            if (item == null) {
                throw new IllegalStateException("Null item registered under key: " + key);
            }
            String tex = item.textureName != null ? item.textureName : key.getKey();
            String itemTexPath = "assets/textures/item/" + tex + ".png";
            String blockTexPath = "assets/textures/block/" + tex + ".png";
            String itemModelPath = "assets/models/item/" + key.getKey() + ".json";
            boolean hasTex = de.delautrer.engine.utils.ResourceUtils.hasResource(itemTexPath) || de.delautrer.engine.utils.ResourceUtils.hasResource(blockTexPath);
            boolean hasModel = de.delautrer.engine.utils.ResourceUtils.hasResource(itemModelPath);
            if (!hasTex && !hasModel) {
                throw new IllegalStateException("Missing texture and model for item: " + key);
            }
        }

        // 3. Loot tables: every item field exists in ItemRegistry
        List<String> lootFiles = de.delautrer.engine.utils.ResourceUtils.listResources("assets/data/veinstride/loot_tables", ".json");
        for (String lootFile : lootFiles) {
            de.delautrer.game.loot.LootTable table = de.delautrer.game.loot.LootTableManager.load(lootFile);
            if (table != null && table.pools != null) {
                for (de.delautrer.game.loot.LootTable.LootPool pool : table.pools) {
                    if (pool.entries != null) {
                        for (de.delautrer.game.loot.LootTable.LootEntry entry : pool.entries) {
                            if (entry.item != null) {
                                NamespacedKey itemKey = NamespacedKey.fromString(entry.item.contains(":") ? entry.item : "veinstride:" + entry.item);
                                if (ITEMS.get(itemKey) == null) {
                                    throw new IllegalStateException("Loot table " + lootFile + " references unknown item: " + entry.item);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Every biome: topBlock, underBlock, underwaterBlock, deepBlock, shoreBlock exist; flora exist; trees exist; blobs exist; features exist
        for (Biome biome : MultiNoiseBiomeRegistry.getBiomes()) {
            validateBiomeBlock(biome.id, "topBlock", biome.topBlock);
            validateBiomeBlock(biome.id, "underBlock", biome.underBlock);
            validateBiomeBlock(biome.id, "underwaterBlock", biome.underwaterBlock);
            validateBiomeBlock(biome.id, "deepBlock", biome.deepBlock != null ? biome.deepBlock : "stone");
            if (biome.shoreBlock != null) {
                validateBiomeBlock(biome.id, "shoreBlock", biome.shoreBlock);
            }

            if (biome.surfaceBlobs != null) {
                for (String blobKeyStr : biome.surfaceBlobs.keySet()) {
                    validateBiomeBlock(biome.id, "surfaceBlobs", blobKeyStr);
                }
            }

            if (biome.undergroundBlobs != null) {
                for (String blobKeyStr : biome.undergroundBlobs.keySet()) {
                    validateBiomeBlock(biome.id, "undergroundBlobs", blobKeyStr);
                }
            }

            if (biome.underwaterBlobs != null) {
                for (String blobKeyStr : biome.underwaterBlobs.keySet()) {
                    validateBiomeBlock(biome.id, "underwaterBlobs", blobKeyStr);
                }
            }

            if (biome.flora != null) {
                for (String floraKeyStr : biome.flora.keySet()) {
                    validateBiomeBlock(biome.id, "flora", floraKeyStr);
                }
            }

            if (biome.trees != null) {
                for (String treeKeyStr : biome.trees.keySet()) {
                    NamespacedKey tKey = NamespacedKey.fromString(treeKeyStr.contains(":") ? treeKeyStr : "veinstride:" + treeKeyStr);
                    if (FeatureRegistry.getConfiguredFeature(tKey) == null) {
                        throw new IllegalStateException("Biome " + biome.id + " references missing configured feature: " + treeKeyStr);
                    }
                }
            }

            if (biome.features != null) {
                for (String featKeyStr : biome.features) {
                    NamespacedKey fKey = NamespacedKey.fromString(featKeyStr.contains(":") ? featKeyStr : "veinstride:" + featKeyStr);
                    if (FeatureRegistry.getPlacedFeature(fKey) == null) {
                        throw new IllegalStateException("Biome " + biome.id + " references missing placed feature: " + featKeyStr);
                    }
                }
            }
        }

        // 5. Every *_sapling block: tree, log, leaves set and resolvable
        for (Map.Entry<NamespacedKey, Block> entry : BLOCKS.entrySet()) {
            NamespacedKey key = entry.getKey();
            Block block = entry.getValue();
            if (key.getKey().endsWith("_sapling") || block instanceof SaplingBlock) {
                if (block instanceof SaplingBlock sapling) {
                    if (sapling.getTreeFeatureKey() == null || sapling.getTreeFeatureKey().trim().isEmpty()) {
                        throw new IllegalStateException("Sapling block " + key + " is missing required 'tree' property!");
                    }
                    NamespacedKey treeKey = NamespacedKey.fromString(sapling.getTreeFeatureKey().contains(":") ? sapling.getTreeFeatureKey() : "veinstride:" + sapling.getTreeFeatureKey());
                    if (FeatureRegistry.getConfiguredFeature(treeKey) == null) {
                        throw new IllegalStateException("Sapling block " + key + " references unknown tree configured_feature: " + sapling.getTreeFeatureKey());
                    }

                    if (sapling.getLogBlockKey() == null || sapling.getLogBlockKey().trim().isEmpty()) {
                        throw new IllegalStateException("Sapling block " + key + " is missing required 'log' property!");
                    }
                    NamespacedKey logKey = NamespacedKey.fromString(sapling.getLogBlockKey().contains(":") ? sapling.getLogBlockKey() : "veinstride:" + sapling.getLogBlockKey());
                    if (BLOCKS.get(logKey) == null) {
                        throw new IllegalStateException("Sapling block " + key + " references unknown log block: " + sapling.getLogBlockKey());
                    }

                    if (sapling.getLeavesBlockKey() == null || sapling.getLeavesBlockKey().trim().isEmpty()) {
                        throw new IllegalStateException("Sapling block " + key + " is missing required 'leaves' property!");
                    }
                    NamespacedKey leavesKey = NamespacedKey.fromString(sapling.getLeavesBlockKey().contains(":") ? sapling.getLeavesBlockKey() : "veinstride:" + sapling.getLeavesBlockKey());
                    if (BLOCKS.get(leavesKey) == null) {
                        throw new IllegalStateException("Sapling block " + key + " references unknown leaves block: " + sapling.getLeavesBlockKey());
                    }
                }
            }
        }

        System.out.printf("Validation Report -> Blocks: %d, Items: %d, Biomes: %d - All Checks Passed.\n",
                BLOCKS.size(), ITEMS.size(), BIOMES.size());
    }

    private static void validateBiomeBlock(String biomeId, String fieldName, String rawName) {
        if (rawName == null) return;
        String blockName = rawName;
        if ("grass".equalsIgnoreCase(blockName)) blockName = "grass_block";
        if (!blockName.contains(":")) blockName = "veinstride:" + blockName;
        NamespacedKey bKey = NamespacedKey.fromString(blockName);
        if (BLOCKS.get(bKey) == null) {
            throw new IllegalStateException("Biome " + biomeId + " field '" + fieldName + "' references unknown block: " + rawName);
        }
    }
}
