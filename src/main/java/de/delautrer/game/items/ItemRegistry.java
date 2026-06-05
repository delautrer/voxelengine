package de.delautrer.game.items;

import de.delautrer.Constants;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registry;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import de.delautrer.game.registry.Registries;

public class ItemRegistry {
    public static final Registry<Item> REGISTRY = new Registry<>();

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        isInitialized = true;
        register("grass_block", new BlockItem("Grass", "grass_block", Registries.BLOCKS.get(Constants.NAMESPACE + ":grass_block")));
        register("grass_block_slabs", new BlockItem("Grass Slabs", "grass_block_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":grass_block_slabs")));
        register("grass_block_stairs", new BlockItem("Grass Stairs", "grass_block_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":grass_block_stairs")));

        register("dirt", new BlockItem("Dirt", "dirt", Registries.BLOCKS.get(Constants.NAMESPACE + ":dirt")));
        register("dirt_slabs", new BlockItem("Dirt Slabs", "dirt_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":dirt_slabs")));
        register("dirt_stairs", new BlockItem("Dirt Stairs", "dirt_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":dirt_stairs")));

        register("stone", new BlockItem("Stone", "stone", Registries.BLOCKS.get(Constants.NAMESPACE + ":stone")));
        register("stone_slabs", new BlockItem("Stone Slabs", "stone_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":stone_slabs")));
        register("stone_stairs", new BlockItem("Stone Stairs", "stone_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":stone_stairs")));

        register("water_bucket", new BlockItem("Water bucket", "water_bucket", Registries.BLOCKS.get(Constants.NAMESPACE + ":water")).setMaxStackSize(1).setCategory("misc"));
        register("empty_bucket", new EmptyBucketItem("Bucket", "empty_bucket").setMaxStackSize(1).setCategory("misc"));

        register("glass", new BlockItem("Glass", "glass", Registries.BLOCKS.get(Constants.NAMESPACE + ":glass")));
        register("oak_leaves", new BlockItem("Oak Leaves", "oak_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_leaves")));
        register("torch", new BlockItem("Torch", "torch", Registries.BLOCKS.get(Constants.NAMESPACE + ":torch")));
        register("bedrock", new BlockItem("Bedrock", "bedrock", Registries.BLOCKS.get(Constants.NAMESPACE + ":bedrock")));
        register("gravel", new BlockItem("Gravel", "gravel", Registries.BLOCKS.get(Constants.NAMESPACE + ":gravel")));
        register("sand", new BlockItem("Sand", "sand", Registries.BLOCKS.get(Constants.NAMESPACE + ":sand")));
        register("oak_log", new BlockItem("Oak Log", "oak_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_log")));

        register("oak_planks", new BlockItem("Oak Planks", "oak_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_planks")));
        register("oak_stairs", new BlockItem("Oak Stairs", "oak_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_stairs")));
        register("oak_slabs", new BlockItem("Oak Slabs", "oak_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_slabs")));

        register("bricks", new BlockItem("Bricks", "bricks_block", Registries.BLOCKS.get(Constants.NAMESPACE + ":bricks")));
        register("bricks_stairs", new BlockItem("Bricks Stairs", "bricks_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":bricks_stairs")));
        register("bricks_slabs", new BlockItem("Bricks Slabs", "bricks_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":bricks_slabs")));

        register("grass", new BlockItem("Grass","grass", Registries.BLOCKS.get(Constants.NAMESPACE + ":grass")));
        register("sandy_grass", new BlockItem("Sandy Grass", "sandy_grass", Registries.BLOCKS.get(Constants.NAMESPACE + ":sandy_grass")));
        register("poppy", new BlockItem("Poppy", "poppy", Registries.BLOCKS.get(Constants.NAMESPACE + ":poppy")));
        register("dandelion", new BlockItem("Dandelion", "dandelion", Registries.BLOCKS.get(Constants.NAMESPACE + ":dandelion")));
        register("dotty", new BlockItem("Dotty", "dotty", Registries.BLOCKS.get(Constants.NAMESPACE + ":dotty")));
        register("fairy_bell", new BlockItem("Fairy Bell", "fairy_bell", Registries.BLOCKS.get(Constants.NAMESPACE + ":fairy_bell")));
        register("red_tulip", new BlockItem("Red Tulip", "red_tulip", Registries.BLOCKS.get(Constants.NAMESPACE + ":red_tulip")));
        register("purple_tulip", new BlockItem("Purple Tulip", "purple_tulip", Registries.BLOCKS.get(Constants.NAMESPACE + ":purple_tulip")));
        register("mavvinilia", new BlockItem("Mavvinilia", "mavvinilia", Registries.BLOCKS.get(Constants.NAMESPACE + ":mavvinilia")));

        register("oak_chest", new BlockItem("Oak Chest", "oak_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_chest")));
        register("oak_trapdoor", new BlockItem("Oak Trapdoor", "oak_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_trapdoor")));
        register("oak_door", new BlockItem("Oak Door", "oak_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_door")));
        register("oak_sapling", new BlockItem("Oak Sapling", "oak_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":oak_sapling")));

        // Birch
        register("birch_log", new BlockItem("Birch Log", "birch_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_log")));
        register("birch_planks", new BlockItem("Birch Planks", "birch_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_planks")));
        register("birch_leaves", new BlockItem("Birch Leaves", "birch_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_leaves")));
        register("birch_stairs", new BlockItem("Birch Stairs", "birch_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_stairs")));
        register("birch_slabs", new BlockItem("Birch Slabs", "birch_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_slabs")));
        register("birch_chest", new BlockItem("Birch Chest", "birch_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_chest")));
        register("birch_trapdoor", new BlockItem("Birch Trapdoor", "birch_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_trapdoor")));
        register("birch_door", new BlockItem("Birch Door", "birch_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_door")));
        register("birch_sapling", new BlockItem("Birch Sapling", "birch_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":birch_sapling")));

        // Pine
        register("pine_log", new BlockItem("Pine Log", "pine_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_log")));
        register("pine_planks", new BlockItem("Pine Planks", "pine_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_planks")));
        register("pine_leaves", new BlockItem("Pine Leaves", "pine_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_leaves")));
        register("pine_stairs", new BlockItem("Pine Stairs", "pine_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_stairs")));
        register("pine_slabs", new BlockItem("Pine Slabs", "pine_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_slabs")));
        register("pine_chest", new BlockItem("Pine Chest", "pine_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_chest")));
        register("pine_trapdoor", new BlockItem("Pine Trapdoor", "pine_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_trapdoor")));
        register("pine_door", new BlockItem("Pine Door", "pine_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_door")));
        register("pine_sapling", new BlockItem("Pine Sapling", "pine_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":pine_sapling")));

        // Willow
        register("willow_log", new BlockItem("Willow Log", "willow_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_log")));
        register("willow_planks", new BlockItem("Willow Planks", "willow_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_planks")));
        register("willow_leaves", new BlockItem("Willow Leaves", "willow_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_leaves")));
        register("willow_stairs", new BlockItem("Willow Stairs", "willow_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_stairs")));
        register("willow_slabs", new BlockItem("Willow Slabs", "willow_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_slabs")));
        register("willow_chest", new BlockItem("Willow Chest", "willow_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_chest")));
        register("willow_trapdoor", new BlockItem("Willow Trapdoor", "willow_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_trapdoor")));
        register("willow_door", new BlockItem("Willow Door", "willow_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_door")));
        register("willow_sapling", new BlockItem("Willow Sapling", "willow_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":willow_sapling")));

        // Baobab
        register("baobab_log", new BlockItem("Baobab Log", "baobab_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_log")));
        register("baobab_planks", new BlockItem("Baobab Planks", "baobab_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_planks")));
        register("baobab_leaves", new BlockItem("Baobab Leaves", "baobab_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_leaves")));
        register("baobab_stairs", new BlockItem("Baobab Stairs", "baobab_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_stairs")));
        register("baobab_slabs", new BlockItem("Baobab Slabs", "baobab_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_slabs")));
        register("baobab_chest", new BlockItem("Baobab Chest", "baobab_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_chest")));
        register("baobab_trapdoor", new BlockItem("Baobab Trapdoor", "baobab_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_trapdoor")));
        register("baobab_door", new BlockItem("Baobab Door", "baobab_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_door")));
        register("baobab_sapling", new BlockItem("Baobab Sapling", "baobab_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":baobab_sapling")));

        // Mahogany
        register("mahogany_log", new BlockItem("Mahogany Log", "mahogany_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_log")));
        register("mahogany_planks", new BlockItem("Mahogany Planks", "mahogany_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_planks")));
        register("mahogany_leaves", new BlockItem("Mahogany Leaves", "mahogany_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_leaves")));
        register("mahogany_stairs", new BlockItem("Mahogany Stairs", "mahogany_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_stairs")));
        register("mahogany_slabs", new BlockItem("Mahogany Slabs", "mahogany_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_slabs")));
        register("mahogany_chest", new BlockItem("Mahogany Chest", "mahogany_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_chest")));
        register("mahogany_trapdoor", new BlockItem("Mahogany Trapdoor", "mahogany_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_trapdoor")));
        register("mahogany_door", new BlockItem("Mahogany Door", "mahogany_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_door")));
        register("mahogany_sapling", new BlockItem("Mahogany Sapling", "mahogany_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":mahogany_sapling")));

        // Palm
        register("palm_log", new BlockItem("Palm Log", "palm_log", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_log")));
        register("palm_planks", new BlockItem("Palm Planks", "palm_planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_planks")));
        register("palm_leaves", new BlockItem("Palm Leaves", "palm_leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_leaves")));
        register("palm_stairs", new BlockItem("Palm Stairs", "palm_stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_stairs")));
        register("palm_slabs", new BlockItem("Palm Slabs", "palm_slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_slabs")));
        register("palm_chest", new BlockItem("Palm Chest", "palm_chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_chest")));
        register("palm_trapdoor", new BlockItem("Palm Trapdoor", "palm_trapdoor", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_trapdoor")));
        register("palm_door", new BlockItem("Palm Door", "palm_door", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_door")));
        register("palm_sapling", new BlockItem("Palm Sapling", "palm_sapling", Registries.BLOCKS.get(Constants.NAMESPACE + ":palm_sapling")));

        register("sticks", new Item("Sticks", "sticks") {
            @Override
            public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
                return false;
            }
        }.setCategory("misc"));

        register("crafting_table", new BlockItem("Crafting Table", "crafting_table", Registries.BLOCKS.get(Constants.NAMESPACE + ":crafting_table")));
        register("furnace", new BlockItem("Furnace", "furnace", Registries.BLOCKS.get(Constants.NAMESPACE + ":furnace")));
        register("coal", new SimpleItem("Coal", "coal"));

        // Ores
        register("coal_ore", new BlockItem("Coal Ore", "coal_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":coal_ore")));
        register("iron_ore", new BlockItem("Iron Ore", "iron_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":iron_ore")));
        register("copper_ore", new BlockItem("Copper Ore", "copper_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":copper_ore")));
        register("zinc_ore", new BlockItem("Zinc Ore", "zinc_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":zinc_ore")));
        register("diamond_ore", new BlockItem("Diamond Ore", "diamond_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":diamond_ore")));
        register("gold_ore", new BlockItem("Gold Ore", "gold_ore", Registries.BLOCKS.get(Constants.NAMESPACE + ":gold_ore")));

        // Resource Items
        register("diamond", new SimpleItem("Diamond", "diamond"));
        register("raw_iron", new SimpleItem("Raw Iron", "raw_iron"));
        register("raw_copper", new SimpleItem("Raw Copper", "raw_copper"));
        register("raw_zinc", new SimpleItem("Raw Zinc", "raw_zinc"));
        register("raw_gold", new SimpleItem("Raw Gold", "raw_gold"));
        register("iron_ingot", new SimpleItem("Iron Ingot", "iron_ingot"));
        register("copper_ingot", new SimpleItem("Copper Ingot", "copper_ingot"));
        register("zinc_ingot", new SimpleItem("Zinc Ingot", "zinc_ingot"));
        register("gold_ingot", new SimpleItem("Gold Ingot", "gold_ingot"));

        // Wooden Tools
        register("wooden_pickaxe", new ToolItem("Wooden Pickaxe", "wooden_pickaxe", ToolItem.ToolType.PICKAXE, "wood", 2.0f, 59));
        register("wooden_shovel", new ToolItem("Wooden Shovel", "wooden_shovel", ToolItem.ToolType.SHOVEL, "wood", 2.0f, 59));
        register("wooden_axe", new ToolItem("Wooden Axe", "wooden_axe", ToolItem.ToolType.AXE, "wood", 2.0f, 59));

        // Stone Tools
        register("stone_pickaxe", new ToolItem("Stone Pickaxe", "stone_pickaxe", ToolItem.ToolType.PICKAXE, "stone", 4.0f, 131));
        register("stone_shovel", new ToolItem("Stone Shovel", "stone_shovel", ToolItem.ToolType.SHOVEL, "stone", 4.0f, 131));
        register("stone_axe", new ToolItem("Stone Axe", "stone_axe", ToolItem.ToolType.AXE, "stone", 4.0f, 131));

        // Copper Tools
        register("copper_pickaxe", new ToolItem("Copper Pickaxe", "copper_pickaxe", ToolItem.ToolType.PICKAXE, "copper", 5.0f, 180));
        register("copper_shovel", new ToolItem("Copper Shovel", "copper_shovel", ToolItem.ToolType.SHOVEL, "copper", 5.0f, 180));
        register("copper_axe", new ToolItem("Copper Axe", "copper_axe", ToolItem.ToolType.AXE, "copper", 5.0f, 180));

        // Iron Tools
        register("iron_pickaxe", new ToolItem("Iron Pickaxe", "iron_pickaxe", ToolItem.ToolType.PICKAXE, "iron", 6.0f, 250));
        register("iron_shovel", new ToolItem("Iron Shovel", "iron_shovel", ToolItem.ToolType.SHOVEL, "iron", 6.0f, 250));
        register("iron_axe", new ToolItem("Iron Axe", "iron_axe", ToolItem.ToolType.AXE, "iron", 6.0f, 250));

        // Gold Tools
        register("gold_pickaxe", new ToolItem("Gold Pickaxe", "gold_pickaxe", ToolItem.ToolType.PICKAXE, "gold", 12.0f, 32));
        register("gold_shovel", new ToolItem("Gold Shovel", "gold_shovel", ToolItem.ToolType.SHOVEL, "gold", 12.0f, 32));
        register("gold_axe", new ToolItem("Gold Axe", "gold_axe", ToolItem.ToolType.AXE, "gold", 12.0f, 32));

        // Diamond Tools
        register("diamond_pickaxe", new ToolItem("Diamond Pickaxe", "diamond_pickaxe", ToolItem.ToolType.PICKAXE, "diamond", 8.0f, 1561));
        register("diamond_shovel", new ToolItem("Diamond Shovel", "diamond_shovel", ToolItem.ToolType.SHOVEL, "diamond", 8.0f, 1561));
        register("diamond_axe", new ToolItem("Diamond Axe", "diamond_axe", ToolItem.ToolType.AXE, "diamond", 8.0f, 1561));

        System.out.println("[ItemRegistry] " + REGISTRY.size() + " Items loaded.");
    }

    private static Item register(String path, Item item) {
        NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
        REGISTRY.register(key, item);
        return item;
    }

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();
        for (Item item : REGISTRY.values()) {
            textures.add(item.textureName);
        }
        return textures;
    }

    public static String getId(Item targetItem) {
        if (targetItem == null) return null;
        NamespacedKey key = REGISTRY.getKey(targetItem);
        return key != null ? key.toString() : null;
    }

    public static Item get(String fullId) { return REGISTRY.get(fullId); }
    public static Map<String, Item> getAll() {
        return REGISTRY.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    }
}
