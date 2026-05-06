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

        register("water_bucket", new BlockItem("Water bucket", "water_bucket", Registries.BLOCKS.get(Constants.NAMESPACE + ":water")).setMaxStackSize(1));
        register("empty_bucket", new EmptyBucketItem("Bucket", "empty_bucket").setMaxStackSize(1));

        register("glass", new BlockItem("Glass", "glass", Registries.BLOCKS.get(Constants.NAMESPACE + ":glass")));
        register("leaves", new BlockItem("Leaves", "leaves", Registries.BLOCKS.get(Constants.NAMESPACE + ":leaves")));
        register("torch", new BlockItem("Torch", "torch", Registries.BLOCKS.get(Constants.NAMESPACE + ":torch")));
        register("bedrock", new BlockItem("Bedrock", "bedrock", Registries.BLOCKS.get(Constants.NAMESPACE + ":bedrock")));
        register("gravel", new BlockItem("Gravel", "gravel", Registries.BLOCKS.get(Constants.NAMESPACE + ":gravel")));
        register("sand", new BlockItem("Sand", "sand", Registries.BLOCKS.get(Constants.NAMESPACE + ":sand")));
        register("log", new BlockItem("Log", "log", Registries.BLOCKS.get(Constants.NAMESPACE + ":log")));

        register("planks", new BlockItem("Planks", "planks", Registries.BLOCKS.get(Constants.NAMESPACE + ":planks")));
        register("stairs", new BlockItem("Stairs", "stairs", Registries.BLOCKS.get(Constants.NAMESPACE + ":stairs")));
        register("slabs", new BlockItem("Slabs", "slabs", Registries.BLOCKS.get(Constants.NAMESPACE + ":slabs")));

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

        register("chest", new BlockItem("Chest", "chest", Registries.BLOCKS.get(Constants.NAMESPACE + ":chest")));

        register("sticks", new Item("Sticks", "sticks") {
            @Override
            public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
                return false;
            }
        });

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
