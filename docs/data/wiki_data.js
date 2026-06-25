const WIKI_DATA = {
  blocks: [
    {
        "id":  1,
        "name":  "grass_block",
        "type":  "cube",
        "hardness":  0.6,
        "soundMaterial":  "grass",
        "category":  "natural"
    },
    {
        "id":  24,
        "name":  "grass_block_slabs",
        "type":  "slab",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  0.6,
        "soundMaterial":  "grass",
        "category":  "natural"
    },
    {
        "id":  25,
        "name":  "grass_block_stairs",
        "type":  "stair",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  0.6,
        "soundMaterial":  "grass",
        "category":  "natural"
    },
    {
        "id":  2,
        "name":  "dirt",
        "type":  "cube",
        "hardness":  0.5,
        "category":  "natural"
    },
    {
        "id":  26,
        "name":  "dirt_slabs",
        "type":  "slab",
        "hardness":  0.5,
        "category":  "natural"
    },
    {
        "id":  27,
        "name":  "dirt_stairs",
        "type":  "stair",
        "hardness":  0.5,
        "category":  "natural"
    },
    {
        "id":  3,
        "name":  "stone",
        "type":  "cube",
        "hardness":  1.5,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  28,
        "name":  "stone_slabs",
        "type":  "slab",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  1.5,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  29,
        "name":  "stone_stairs",
        "type":  "stair",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  1.5,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  4,
        "name":  "water",
        "type":  "water",
        "hardness":  -1.0,
        "soundMaterial":  "water",
        "category":  "misc"
    },
    {
        "id":  5,
        "name":  "glass",
        "type":  "cube",
        "isTransparent":  true,
        "hardness":  0.25,
        "category":  "building"
    },
    {
        "id":  6,
        "name":  "oak_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  7,
        "name":  "torch",
        "type":  "torch",
        "hardness":  0.0001,
        "lightEmission":  14,
        "category":  "misc"
    },
    {
        "id":  8,
        "name":  "bedrock",
        "type":  "cube",
        "hardness":  -1.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "DIAMOND"
    },
    {
        "id":  9,
        "name":  "gravel",
        "type":  "gravity",
        "hardness":  0.6,
        "category":  "natural"
    },
    {
        "id":  10,
        "name":  "sand",
        "type":  "gravity",
        "hardness":  0.5,
        "category":  "natural"
    },
    {
        "id":  11,
        "name":  "oak_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  12,
        "name":  "grass",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "misc"
    },
    {
        "id":  13,
        "name":  "sandy_grass",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "misc"
    },
    {
        "id":  14,
        "name":  "poppy",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  15,
        "name":  "dandelion",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  16,
        "name":  "dotty",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  17,
        "name":  "fairy_bell",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  18,
        "name":  "red_tulip",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  19,
        "name":  "purple_tulip",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  23,
        "name":  "mavvinilia",
        "type":  "plant",
        "hardness":  0.0001,
        "soundMaterial":  "leaves",
        "category":  "natural"
    },
    {
        "id":  20,
        "name":  "oak_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  21,
        "name":  "oak_stairs",
        "type":  "stair",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  22,
        "name":  "oak_slabs",
        "type":  "slab",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  30,
        "name":  "bricks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  31,
        "name":  "bricks_stairs",
        "type":  "stair",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  32,
        "name":  "bricks_slabs",
        "type":  "slab",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  33,
        "name":  "oak_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  34,
        "name":  "oak_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  35,
        "name":  "oak_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  100,
        "name":  "birch_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  101,
        "name":  "birch_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  102,
        "name":  "birch_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  103,
        "name":  "birch_stairs",
        "type":  "stair",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  104,
        "name":  "birch_slabs",
        "type":  "slab",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  105,
        "name":  "birch_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  106,
        "name":  "birch_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  107,
        "name":  "birch_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  110,
        "name":  "pine_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  111,
        "name":  "pine_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  112,
        "name":  "pine_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  113,
        "name":  "pine_stairs",
        "type":  "stair",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  114,
        "name":  "pine_slabs",
        "type":  "slab",
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  115,
        "name":  "pine_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  116,
        "name":  "pine_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  117,
        "name":  "pine_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  120,
        "name":  "willow_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  121,
        "name":  "willow_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  122,
        "name":  "willow_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  123,
        "name":  "willow_stairs",
        "type":  "stair",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  124,
        "name":  "willow_slabs",
        "type":  "slab",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  125,
        "name":  "willow_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  126,
        "name":  "willow_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  127,
        "name":  "willow_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  130,
        "name":  "baobab_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  131,
        "name":  "baobab_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  132,
        "name":  "baobab_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  133,
        "name":  "baobab_stairs",
        "type":  "stair",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  134,
        "name":  "baobab_slabs",
        "type":  "slab",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  135,
        "name":  "baobab_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  136,
        "name":  "baobab_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  137,
        "name":  "baobab_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  140,
        "name":  "mahogany_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  141,
        "name":  "mahogany_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  142,
        "name":  "mahogany_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  143,
        "name":  "mahogany_stairs",
        "type":  "stair",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  144,
        "name":  "mahogany_slabs",
        "type":  "slab",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  145,
        "name":  "mahogany_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  146,
        "name":  "mahogany_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  147,
        "name":  "mahogany_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  150,
        "name":  "palm_log",
        "type":  "log",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  151,
        "name":  "palm_planks",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  152,
        "name":  "palm_leaves",
        "type":  "leaves",
        "hardness":  0.2,
        "soundMaterial":  "leaves",
        "category":  "wood"
    },
    {
        "id":  153,
        "name":  "palm_stairs",
        "type":  "stair",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  154,
        "name":  "palm_slabs",
        "type":  "slab",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  155,
        "name":  "palm_chest",
        "type":  "chest",
        "hardness":  1.42,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  156,
        "name":  "palm_trapdoor",
        "type":  "trapdoor",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood",
        "textures":  {

                     }
    },
    {
        "id":  157,
        "name":  "palm_door",
        "type":  "door",
        "hardness":  2.0,
        "soundMaterial":  "wood",
        "category":  "wood"
    },
    {
        "id":  160,
        "name":  "crafting_table",
        "type":  "crafting_table",
        "hardness":  1.5,
        "soundMaterial":  "wood",
        "category":  "building"
    },
    {
        "id":  161,
        "name":  "furnace",
        "type":  "furnace",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  36,
        "name":  "oak_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  108,
        "name":  "birch_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  118,
        "name":  "pine_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  128,
        "name":  "willow_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  138,
        "name":  "baobab_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  148,
        "name":  "mahogany_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  158,
        "name":  "palm_sapling",
        "type":  "sapling",
        "isSolid":  false,
        "isTransparent":  true,
        "hardness":  0.0,
        "soundMaterial":  "grass",
        "category":  "natural",
        "minGrowthTime":  60,
        "maxGrowthTime":  180
    },
    {
        "id":  170,
        "name":  "coal_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  171,
        "name":  "iron_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  172,
        "name":  "copper_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  173,
        "name":  "zinc_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  174,
        "name":  "diamond_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "IRON"
    },
    {
        "id":  175,
        "name":  "gold_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "IRON"
    },
    {
        "id":  37,
        "name":  "cobblestone",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  38,
        "name":  "cobblestone_slabs",
        "type":  "slab",
        "isSolid":  false,
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  39,
        "name":  "cobblestone_stairs",
        "type":  "stair",
        "isSolid":  false,
        "isTransparent":  true,
        "opacity":  15,
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "building",
        "minToolTier":  "WOOD"
    },
    {
        "id":  176,
        "name":  "dolomite",
        "type":  "cube",
        "hardness":  2.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  180,
        "name":  "dolomite_coal_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "WOOD"
    },
    {
        "id":  181,
        "name":  "dolomite_iron_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  182,
        "name":  "dolomite_copper_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  183,
        "name":  "dolomite_zinc_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "STONE"
    },
    {
        "id":  184,
        "name":  "dolomite_diamond_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "IRON"
    },
    {
        "id":  185,
        "name":  "dolomite_gold_ore",
        "type":  "cube",
        "hardness":  3.0,
        "soundMaterial":  "rock",
        "category":  "natural",
        "minToolTier":  "IRON"
    }
]
,
  items: [
    {
        "id":  "grass_block",
        "name":  "Grass",
        "textureName":  "grass_block",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "grass_block_slabs",
        "name":  "Grass Slabs",
        "textureName":  "grass_block_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "grass_block_stairs",
        "name":  "Grass Stairs",
        "textureName":  "grass_block_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "dirt",
        "name":  "Dirt",
        "textureName":  "dirt",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "dirt_slabs",
        "name":  "Dirt Slabs",
        "textureName":  "dirt_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "dirt_stairs",
        "name":  "Dirt Stairs",
        "textureName":  "dirt_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "stone",
        "name":  "Stone",
        "textureName":  "stone",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "stone_slabs",
        "name":  "Stone Slabs",
        "textureName":  "stone_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "stone_stairs",
        "name":  "Stone Stairs",
        "textureName":  "stone_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "dolomite",
        "name":  "Dolomite",
        "textureName":  "dolomite",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "cobblestone",
        "name":  "Cobblestone",
        "textureName":  "cobblestone",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "cobblestone_slabs",
        "name":  "Cobblestone Slabs",
        "textureName":  "cobblestone_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "cobblestone_stairs",
        "name":  "Cobblestone Stairs",
        "textureName":  "cobblestone_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "water_bucket",
        "name":  "Water bucket",
        "textureName":  "water_bucket",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "tools",
        "renderAsItem":  true,
        "blockId":  "water"
    },
    {
        "id":  "empty_bucket",
        "name":  "Bucket",
        "textureName":  "empty_bucket",
        "type":  "empty_bucket",
        "maxStackSize":  1,
        "renderAsItem":  true,
        "category":  "tools"
    },
    {
        "id":  "glass",
        "name":  "Glass",
        "textureName":  "glass",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "oak_leaves",
        "name":  "Oak Leaves",
        "textureName":  "oak_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "torch",
        "name":  "Torch",
        "textureName":  "torch",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "misc"
    },
    {
        "id":  "bedrock",
        "name":  "Bedrock",
        "textureName":  "bedrock",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "gravel",
        "name":  "Gravel",
        "textureName":  "gravel",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "sand",
        "name":  "Sand",
        "textureName":  "sand",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "oak_log",
        "name":  "Oak Log",
        "textureName":  "oak_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "oak_planks",
        "name":  "Oak Planks",
        "textureName":  "oak_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "oak_stairs",
        "name":  "Oak Stairs",
        "textureName":  "oak_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "oak_slabs",
        "name":  "Oak Slabs",
        "textureName":  "oak_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "bricks",
        "name":  "Bricks",
        "textureName":  "bricks_block",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "bricks_stairs",
        "name":  "Bricks Stairs",
        "textureName":  "bricks_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "bricks_slabs",
        "name":  "Bricks Slabs",
        "textureName":  "bricks_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "grass",
        "name":  "Grass",
        "textureName":  "grass",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "sandy_grass",
        "name":  "Sandy Grass",
        "textureName":  "sandy_grass",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "poppy",
        "name":  "Poppy",
        "textureName":  "poppy",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "dandelion",
        "name":  "Dandelion",
        "textureName":  "dandelion",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "dotty",
        "name":  "Dotty",
        "textureName":  "dotty",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "fairy_bell",
        "name":  "Fairy Bell",
        "textureName":  "fairy_bell",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "red_tulip",
        "name":  "Red Tulip",
        "textureName":  "red_tulip",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "purple_tulip",
        "name":  "Purple Tulip",
        "textureName":  "purple_tulip",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "mavvinilia",
        "name":  "Mavvinilia",
        "textureName":  "mavvinilia",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "oak_chest",
        "name":  "Oak Chest",
        "textureName":  "oak_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "oak_trapdoor",
        "name":  "Oak Trapdoor",
        "textureName":  "oak_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "oak_door",
        "name":  "Oak Door",
        "textureName":  "oak_door",
        "renderAsItem":  true,
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "oak_sapling",
        "name":  "Oak Sapling",
        "textureName":  "oak_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "birch_log",
        "name":  "Birch Log",
        "textureName":  "birch_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "birch_planks",
        "name":  "Birch Planks",
        "textureName":  "birch_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "birch_leaves",
        "name":  "Birch Leaves",
        "textureName":  "birch_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "birch_stairs",
        "name":  "Birch Stairs",
        "textureName":  "birch_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "birch_slabs",
        "name":  "Birch Slabs",
        "textureName":  "birch_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "birch_chest",
        "name":  "Birch Chest",
        "textureName":  "birch_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "birch_trapdoor",
        "name":  "Birch Trapdoor",
        "textureName":  "birch_trapdoor",
        "renderAsItem":  true,
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "birch_door",
        "name":  "Birch Door",
        "textureName":  "birch_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "birch_sapling",
        "name":  "Birch Sapling",
        "textureName":  "birch_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "pine_log",
        "name":  "Pine Log",
        "textureName":  "pine_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "pine_planks",
        "name":  "Pine Planks",
        "textureName":  "pine_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "pine_leaves",
        "name":  "Pine Leaves",
        "textureName":  "pine_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "pine_stairs",
        "name":  "Pine Stairs",
        "textureName":  "pine_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "pine_slabs",
        "name":  "Pine Slabs",
        "textureName":  "pine_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "pine_chest",
        "name":  "Pine Chest",
        "textureName":  "pine_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "pine_trapdoor",
        "name":  "Pine Trapdoor",
        "textureName":  "pine_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "pine_door",
        "name":  "Pine Door",
        "textureName":  "pine_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "pine_sapling",
        "name":  "Pine Sapling",
        "textureName":  "pine_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "willow_log",
        "name":  "Willow Log",
        "textureName":  "willow_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "willow_planks",
        "name":  "Willow Planks",
        "textureName":  "willow_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "willow_leaves",
        "name":  "Willow Leaves",
        "textureName":  "willow_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "willow_stairs",
        "name":  "Willow Stairs",
        "textureName":  "willow_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "willow_slabs",
        "name":  "Willow Slabs",
        "textureName":  "willow_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "willow_chest",
        "name":  "Willow Chest",
        "textureName":  "willow_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "willow_trapdoor",
        "name":  "Willow Trapdoor",
        "textureName":  "willow_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "willow_door",
        "name":  "Willow Door",
        "textureName":  "willow_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "willow_sapling",
        "name":  "Willow Sapling",
        "textureName":  "willow_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "baobab_log",
        "name":  "Baobab Log",
        "textureName":  "baobab_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "baobab_planks",
        "name":  "Baobab Planks",
        "textureName":  "baobab_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "baobab_leaves",
        "name":  "Baobab Leaves",
        "textureName":  "baobab_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "baobab_stairs",
        "name":  "Baobab Stairs",
        "textureName":  "baobab_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "baobab_slabs",
        "name":  "Baobab Slabs",
        "textureName":  "baobab_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "baobab_chest",
        "name":  "Baobab Chest",
        "textureName":  "baobab_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "baobab_trapdoor",
        "name":  "Baobab Trapdoor",
        "textureName":  "baobab_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "baobab_door",
        "name":  "Baobab Door",
        "textureName":  "baobab_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "baobab_sapling",
        "name":  "Baobab Sapling",
        "textureName":  "baobab_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "mahogany_log",
        "name":  "Mahogany Log",
        "textureName":  "mahogany_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "mahogany_planks",
        "name":  "Mahogany Planks",
        "textureName":  "mahogany_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "mahogany_leaves",
        "name":  "Mahogany Leaves",
        "textureName":  "mahogany_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "mahogany_stairs",
        "name":  "Mahogany Stairs",
        "textureName":  "mahogany_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "mahogany_slabs",
        "name":  "Mahogany Slabs",
        "textureName":  "mahogany_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "mahogany_chest",
        "name":  "Mahogany Chest",
        "textureName":  "mahogany_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "mahogany_trapdoor",
        "name":  "Mahogany Trapdoor",
        "textureName":  "mahogany_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "mahogany_door",
        "name":  "Mahogany Door",
        "textureName":  "mahogany_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "mahogany_sapling",
        "name":  "Mahogany Sapling",
        "textureName":  "mahogany_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "palm_log",
        "name":  "Palm Log",
        "textureName":  "palm_log",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "palm_planks",
        "name":  "Palm Planks",
        "textureName":  "palm_planks",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "palm_leaves",
        "name":  "Palm Leaves",
        "textureName":  "palm_leaves",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "natural"
    },
    {
        "id":  "palm_stairs",
        "name":  "Palm Stairs",
        "textureName":  "palm_stairs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "palm_slabs",
        "name":  "Palm Slabs",
        "textureName":  "palm_slabs",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "building"
    },
    {
        "id":  "palm_chest",
        "name":  "Palm Chest",
        "textureName":  "palm_chest",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "wood"
    },
    {
        "id":  "palm_trapdoor",
        "name":  "Palm Trapdoor",
        "textureName":  "palm_trapdoor",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "palm_door",
        "name":  "Palm Door",
        "textureName":  "palm_door",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "wood"
    },
    {
        "id":  "palm_sapling",
        "name":  "Palm Sapling",
        "textureName":  "palm_sapling",
        "type":  "block",
        "maxStackSize":  64,
        "renderAsItem":  true,
        "category":  "natural"
    },
    {
        "id":  "sticks",
        "name":  "Sticks",
        "textureName":  "sticks",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "crafting_table",
        "name":  "Crafting Table",
        "textureName":  "crafting_table",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "furnace",
        "name":  "Furnace",
        "textureName":  "furnace",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "coal",
        "name":  "Coal",
        "textureName":  "coal",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "coal_ore",
        "name":  "Coal Ore",
        "textureName":  "coal_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "iron_ore",
        "name":  "Iron Ore",
        "textureName":  "iron_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "copper_ore",
        "name":  "Copper Ore",
        "textureName":  "copper_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "zinc_ore",
        "name":  "Zinc Ore",
        "textureName":  "zinc_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "diamond_ore",
        "name":  "Diamond Ore",
        "textureName":  "diamond_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "gold_ore",
        "name":  "Gold Ore",
        "textureName":  "gold_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_coal_ore",
        "name":  "Dolomite Coal Ore",
        "textureName":  "dolomite_coal_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_iron_ore",
        "name":  "Dolomite Iron Ore",
        "textureName":  "dolomite_iron_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_copper_ore",
        "name":  "Dolomite Copper Ore",
        "textureName":  "dolomite_copper_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_zinc_ore",
        "name":  "Dolomite Zinc Ore",
        "textureName":  "dolomite_zinc_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_diamond_ore",
        "name":  "Dolomite Diamond Ore",
        "textureName":  "dolomite_diamond_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "dolomite_gold_ore",
        "name":  "Dolomite Gold Ore",
        "textureName":  "dolomite_gold_ore",
        "type":  "block",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "diamond",
        "name":  "Diamond",
        "textureName":  "diamond",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "raw_iron",
        "name":  "Raw Iron",
        "textureName":  "raw_iron",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "raw_copper",
        "name":  "Raw Copper",
        "textureName":  "raw_copper",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "raw_zinc",
        "name":  "Raw Zinc",
        "textureName":  "raw_zinc",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "raw_gold",
        "name":  "Raw Gold",
        "textureName":  "raw_gold",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "iron_ingot",
        "name":  "Iron Ingot",
        "textureName":  "iron_ingot",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "copper_ingot",
        "name":  "Copper Ingot",
        "textureName":  "copper_ingot",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "zinc_ingot",
        "name":  "Zinc Ingot",
        "textureName":  "zinc_ingot",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "gold_ingot",
        "name":  "Gold Ingot",
        "textureName":  "gold_ingot",
        "type":  "simple",
        "maxStackSize":  64,
        "category":  "misc"
    },
    {
        "id":  "wooden_pickaxe",
        "name":  "Wooden Pickaxe",
        "textureName":  "wooden_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "WOOD",
        "toolEfficiency":  2.0,
        "toolMaxDurability":  59,
        "category":  "tools"
    },
    {
        "id":  "wooden_shovel",
        "name":  "Wooden Shovel",
        "textureName":  "wooden_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "WOOD",
        "toolEfficiency":  2.0,
        "toolMaxDurability":  59,
        "category":  "tools"
    },
    {
        "id":  "wooden_axe",
        "name":  "Wooden Axe",
        "textureName":  "wooden_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "WOOD",
        "toolEfficiency":  2.0,
        "toolMaxDurability":  59,
        "category":  "tools"
    },
    {
        "id":  "stone_pickaxe",
        "name":  "Stone Pickaxe",
        "textureName":  "stone_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "STONE",
        "toolEfficiency":  4.0,
        "toolMaxDurability":  131,
        "category":  "tools"
    },
    {
        "id":  "stone_shovel",
        "name":  "Stone Shovel",
        "textureName":  "stone_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "STONE",
        "toolEfficiency":  4.0,
        "toolMaxDurability":  131,
        "category":  "tools"
    },
    {
        "id":  "stone_axe",
        "name":  "Stone Axe",
        "textureName":  "stone_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "STONE",
        "toolEfficiency":  4.0,
        "toolMaxDurability":  131,
        "category":  "tools"
    },
    {
        "id":  "copper_pickaxe",
        "name":  "Copper Pickaxe",
        "textureName":  "copper_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "COPPER",
        "toolEfficiency":  5.0,
        "toolMaxDurability":  180,
        "category":  "tools"
    },
    {
        "id":  "copper_shovel",
        "name":  "Copper Shovel",
        "textureName":  "copper_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "COPPER",
        "toolEfficiency":  5.0,
        "toolMaxDurability":  180,
        "category":  "tools"
    },
    {
        "id":  "copper_axe",
        "name":  "Copper Axe",
        "textureName":  "copper_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "COPPER",
        "toolEfficiency":  5.0,
        "toolMaxDurability":  180,
        "category":  "tools"
    },
    {
        "id":  "iron_pickaxe",
        "name":  "Iron Pickaxe",
        "textureName":  "iron_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "IRON",
        "toolEfficiency":  6.0,
        "toolMaxDurability":  250,
        "category":  "tools"
    },
    {
        "id":  "iron_shovel",
        "name":  "Iron Shovel",
        "textureName":  "iron_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "IRON",
        "toolEfficiency":  6.0,
        "toolMaxDurability":  250,
        "category":  "tools"
    },
    {
        "id":  "iron_axe",
        "name":  "Iron Axe",
        "textureName":  "iron_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "IRON",
        "toolEfficiency":  6.0,
        "toolMaxDurability":  250,
        "category":  "tools"
    },
    {
        "id":  "gold_pickaxe",
        "name":  "Gold Pickaxe",
        "textureName":  "gold_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "GOLD",
        "toolEfficiency":  7.0,
        "toolMaxDurability":  32,
        "category":  "tools"
    },
    {
        "id":  "gold_shovel",
        "name":  "Gold Shovel",
        "textureName":  "gold_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "GOLD",
        "toolEfficiency":  7.0,
        "toolMaxDurability":  32,
        "category":  "tools"
    },
    {
        "id":  "gold_axe",
        "name":  "Gold Axe",
        "textureName":  "gold_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "GOLD",
        "toolEfficiency":  7.0,
        "toolMaxDurability":  32,
        "category":  "tools"
    },
    {
        "id":  "diamond_pickaxe",
        "name":  "Diamond Pickaxe",
        "textureName":  "diamond_pickaxe",
        "type":  "tool",
        "toolType":  "PICKAXE",
        "toolTier":  "DIAMOND",
        "toolEfficiency":  8.0,
        "toolMaxDurability":  1561,
        "category":  "tools"
    },
    {
        "id":  "diamond_shovel",
        "name":  "Diamond Shovel",
        "textureName":  "diamond_shovel",
        "type":  "tool",
        "toolType":  "SHOVEL",
        "toolTier":  "DIAMOND",
        "toolEfficiency":  8.0,
        "toolMaxDurability":  1561,
        "category":  "tools"
    },
    {
        "id":  "diamond_axe",
        "name":  "Diamond Axe",
        "textureName":  "diamond_axe",
        "type":  "tool",
        "toolType":  "AXE",
        "toolTier":  "DIAMOND",
        "toolEfficiency":  8.0,
        "toolMaxDurability":  1561,
        "category":  "tools"
    }
]
,
  biomes: [
  {
    "id": "OCEAN",
    "temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-1.0, -0.3], "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "gravel", "underBlock": "gravel", "underwaterBlock": "gravel", "deepBlock": "stone",
    "baseHeight": -29.0, "heightVariation": 10.0,
    "floraProbability": 0.0, "treeProbability": 0.0,
    "undergroundBlobs": {"dirt": 0.05}, "underwaterBlobScale": 0.05, "underwaterBlobs": {"sand": 0.7, "gravel": 0.3}
  },
  {
    "id": "DESERT",
    "temperature": [0.3, 1.0], "humidity": [-1.0, -0.2], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.4], "weirdness": [-1.0, 1.0],
    "topBlock": "sand", "underBlock": "sand", "underwaterBlock": "sand", "deepBlock": "sand",
    "baseHeight": 1.0, "heightVariation": 15.0,
    "floraProbability": 0.08, "treeProbability": 0.0,
    "undergroundBlobs": {"gravel": 0.05}
  },
  {
    "id": "SAVANNA",
    "temperature": [0.4, 1.0], "humidity": [-0.2, 0.2], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.4], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "sand", "deepBlock": "stone",
    "baseHeight": 1.0, "heightVariation": 15.0,
    "floraProbability": 0.2, "floraPatchThreshold": 0.0, "floraDensity": 0.4,
    "flora": {"grass": 100, "dandelion": 10},
    "treeProbability": 0.01, "trees": {"alpha_baobab": 1},
    "undergroundBlobs": {"sand": 0.4, "dirt": 0.2}
  },
  {
    "id": "JUNGLE",
    "temperature": [0.3, 1.0], "humidity": [0.4, 1.0], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 6.0, "heightVariation": 30.0,
    "floraProbability": 0.5, "floraPatchThreshold": -0.3, "floraDensity": 0.7,
    "flora": {"grass": 300, "mavvinilia": 25},
    "treeProbability": 0.08, "trees": {"alpha_mahogany": 1},
    "undergroundBlobs": {"dirt": 0.2}
  },
  {
    "id": "MOUNTAINS",
    "temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-0.1, 1.0], "erosion": [0.3, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "stone", "underBlock": "stone", "underwaterBlock": "stone", "shoreBlock": "stone", "deepBlock": "stone",
    "baseHeight": 46.0, "heightVariation": 140.0,
    "floraProbability": 0.0, "treeProbability": 0.0,
    "surfaceBlobs": {"gravel": 0.4},
    "undergroundBlobs": {"gravel": 0.1}, "underwaterBlobScale": 0.08, "underwaterBlobs": {"gravel": 0.8, "stone": 0.2}
  },
  {
    "id": "BIRCH_PLAINS",
    "temperature": [0.1, 0.4], "humidity": [0.0, 0.2], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.2], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 0.0, "heightVariation": 10.0,
    "floraProbability": 0.25, "flora": {"grass": 200, "poppy": 15, "purple_tulip": 5},
    "treeProbability": 0.015, "trees": {"alpha_birch": 1}
  },
  {
    "id": "FLOWER_PLAINS",
    "temperature": [0.0, 0.3], "humidity": [0.3, 0.6], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.2], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 0.0, "heightVariation": 10.0,
    "floraProbability": 0.55, "floraPatchThreshold": -0.5, "floraDensity": 0.9,
    "flora": {"grass": 300, "dotty": 40, "red_tulip": 40, "purple_tulip": 40, "poppy": 25, "dandelion": 25, "fairy_bell": 10},
    "treeProbability": 0.000, "trees": {"alpha_oak": 1}
  },
  {
    "id": "BIRCH_FOREST",
    "temperature": [0.1, 0.4], "humidity": [0.3, 0.7], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 2.0, "heightVariation": 15.0,
    "floraProbability": 0.4, "flora": {"grass": 200, "purple_tulip": 12, "dotty": 8},
    "treeProbability": 0.06, "trees": {"alpha_birch": 1}
  },
  {
    "id": "FOREST",
    "temperature": [-0.2, 0.2], "humidity": [0.2, 0.5], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 2.0, "heightVariation": 15.0,
    "floraProbability": 0.4, "flora": {"grass": 200, "poppy": 12, "red_tulip": 8},
    "treeProbability": 0.06, "trees": {"alpha_oak": 1}
  },
  {
    "id": "BIRCH_HILLS",
    "temperature": [0.1, 0.4], "humidity": [-1.0, 1.0], "continentalness": [-0.1, 1.0], "erosion": [0.15, 0.3], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "sand", "deepBlock": "stone",
    "baseHeight": 21.0, "heightVariation": 50.0,
    "floraProbability": 0.4, "flora": {"grass": 150, "purple_tulip": 10},
    "treeProbability": 0.015, "trees": {"alpha_birch": 1}
  },
  {
    "id": "HILLS",
    "temperature": [-1.0, 0.4], "humidity": [-1.0, 1.0], "continentalness": [-0.1, 1.0], "erosion": [0.15, 0.3], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "sand", "deepBlock": "stone",
    "baseHeight": 21.0, "heightVariation": 50.0,
    "floraProbability": 0.4, "flora": {"grass": 150, "red_tulip": 10},
    "treeProbability": 0.015, "trees": {"alpha_oak": 1}
  },
  {
    "id": "SWAMP",
    "temperature": [0.2, 1.0], "humidity": [0.3, 1.0], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.3], "weirdness": [0.3, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "water", "shoreBlock": "dirt", "deepBlock": "stone",
    "baseHeight": -2.0, "heightVariation": 5.0,
    "floraProbability": 0.6, "floraPatchThreshold": -0.3, "floraDensity": 0.6,
    "flora": {"grass": 200, "fairy_bell": 40, "dotty": 15},
    "treeProbability": 0.15, "trees": {"alpha_willow": 1}
  },
  {
    "id": "PINE_FOREST",
    "temperature": [-1.0, -0.1], "humidity": [0.1, 1.0], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 2.0, "heightVariation": 15.0,
    "floraProbability": 0.4, "flora": {"grass": 200, "dotty": 15, "fairy_bell": 5},
    "treeProbability": 0.07, "trees": {"alpha_pine": 1}
  },
  {
    "id": "PLAINS",
    "temperature": [-0.1, 0.1], "humidity": [-0.2, 0.1], "continentalness": [-0.1, 1.0], "erosion": [-1.0, 0.15], "weirdness": [-1.0, 1.0],
    "topBlock": "grass_block", "underBlock": "dirt", "underwaterBlock": "sand", "shoreBlock": "grass_block", "deepBlock": "stone",
    "baseHeight": 0.0, "heightVariation": 10.0,
    "floraProbability": 0.2, "flora": {"grass": 200, "dandelion": 20, "red_tulip": 5},
    "treeProbability": 0.0015, "trees": {"alpha_oak": 1}
  }
],
  furnace_recipes: [
  {
    "input": "engine:cobblestone",
    "result": "engine:stone",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:sand",
    "result": "engine:glass",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:stone",
    "result": "engine:bricks",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:oak_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:birch_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:pine_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:willow_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:baobab_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:mahogany_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:palm_log",
    "result": "engine:coal",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:raw_iron",
    "result": "engine:iron_ingot",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:raw_copper",
    "result": "engine:copper_ingot",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:raw_zinc",
    "result": "engine:zinc_ingot",
    "count": 1,
    "cook_time": 200
  },
  {
    "input": "engine:raw_gold",
    "result": "engine:gold_ingot",
    "count": 1,
    "cook_time": 200
  }
]

,
  furnace_fuels: [
  {
    "item": "engine:coal",
    "burn_time": 1600
  },
  {
    "item": "engine:oak_planks",
    "burn_time": 300
  },
  {
    "item": "engine:birch_planks",
    "burn_time": 300
  },
  {
    "item": "engine:pine_planks",
    "burn_time": 300
  },
  {
    "item": "engine:willow_planks",
    "burn_time": 300
  },
  {
    "item": "engine:baobab_planks",
    "burn_time": 300
  },
  {
    "item": "engine:mahogany_planks",
    "burn_time": 300
  },
  {
    "item": "engine:palm_planks",
    "burn_time": 300
  },
  {
    "item": "engine:oak_log",
    "burn_time": 300
  },
  {
    "item": "engine:birch_log",
    "burn_time": 300
  },
  {
    "item": "engine:pine_log",
    "burn_time": 300
  },
  {
    "item": "engine:willow_log",
    "burn_time": 300
  },
  {
    "item": "engine:baobab_log",
    "burn_time": 300
  },
  {
    "item": "engine:mahogany_log",
    "burn_time": 300
  },
  {
    "item": "engine:palm_log",
    "burn_time": 300
  },
  {
    "item": "engine:sticks",
    "burn_time": 100
  }
]
,
  recipes: [
{ "_id": "baobab_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:baobab_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:baobab_chest"
  }
}
,
{ "_id": "baobab_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:baobab_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:baobab_door"
  }
}
,
{ "_id": "baobab_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:baobab_log"
  ],
  "result": {
    "item": "engine:baobab_planks",
    "count": 4
  }
}
,
{ "_id": "baobab_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:baobab_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:baobab_slabs"
  }
}
,
{ "_id": "baobab_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:baobab_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:baobab_stairs"
  }
}
,
{ "_id": "baobab_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:baobab_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:baobab_trapdoor"
  }
}
,
{ "_id": "birch_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:birch_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:birch_chest"
  }
}
,
{ "_id": "birch_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:birch_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:birch_door"
  }
}
,
{ "_id": "birch_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:birch_log"
  ],
  "result": {
    "item": "engine:birch_planks",
    "count": 4
  }
}
,
{ "_id": "birch_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:birch_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:birch_slabs"
  }
}
,
{ "_id": "birch_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:birch_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:birch_stairs"
  }
}
,
{ "_id": "birch_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:birch_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:birch_trapdoor"
  }
}
,
{ "_id": "bricks", 
  "type": "shapeless",
  "ingredients": [
    "engine:gravel",
    "engine:sand"
  ],
  "result": {
    "item": "engine:bricks"
  }
},
{ "_id": "bricks_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:bricks"
  },
  "result": {
    "count": 6,
    "item": "engine:bricks_slabs"
  }
},
{ "_id": "bricks_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:bricks"
  },
  "result": {
    "count": 4,
    "item": "engine:bricks_stairs"
  }
},
﻿{
  "type": "shaped",
  "pattern": [
    "   ",
    "   ",
    "CCC"
  ],
  "keys": {
    "C": "engine:cobblestone"
  },
  "result": {
    "item": "engine:cobblestone_slabs",
    "count": 6
  }
}
,
﻿{
  "type": "shaped",
  "pattern": [
    "C  ",
    "CC ",
    "CCC"
  ],
  "keys": {
    "C": "engine:cobblestone"
  },
  "result": {
    "item": "engine:cobblestone_stairs",
    "count": 4
  }
}
,
{ "_id": "copper_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": "engine:copper_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:copper_axe",
    "count": 1
  }
}
,
{ "_id": "copper_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": "engine:copper_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:copper_pickaxe",
    "count": 1
  }
}
,
{ "_id": "copper_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": "engine:copper_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:copper_shovel",
    "count": 1
  }
}
,
{ "_id": "crafting_table", 
  "pattern": [
    "AA",
    "AA"
  ],
  "keys": {
    "A": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks"
    ]
  },
  "type": "shaped",
  "result": {
    "count": 1,
    "item": "engine:crafting_table"
  }
}
,
{ "_id": "diamond_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": "engine:diamond",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:diamond_axe",
    "count": 1
  }
}
,
{ "_id": "diamond_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": "engine:diamond",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:diamond_pickaxe",
    "count": 1
  }
}
,
{ "_id": "diamond_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": "engine:diamond",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:diamond_shovel",
    "count": 1
  }
}
,
{ "_id": "dirt_slabs", 
  "type": "shapeless",
  "ingredients": [
    "engine:dirt"
  ],
  "result": {
    "item": "engine:dirt_slabs",
    "count": 2
  }
},
{ "_id": "dirt_stairs", 
  "type": "shaped",
  "pattern": [
    "A ",
    "AA"
  ],
  "keys": {
    "A": "engine:dirt"
  },
  "result": {
    "item": "engine:dirt_stairs",
    "count": 4
  }
},
{ "_id": "empty_bucket", 
  "type": "shaped",
  "pattern": [
    "A A",
    " A "
  ],
  "keys": {
    "A": [
      "engine:iron_ingot",
      "engine:zinc_ingot"
    ]
  },
  "result": {
    "count": 1,
    "item": "engine:empty_bucket"
  }
},
{ "_id": "furnace", 
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:cobblestone"
  },
  "type": "shaped",
  "result": {
    "count": 1,
    "item": "engine:furnace"
  }
}
,
{ "_id": "glass", 
  "type": "shapeless",
  "ingredients": [
    "engine:sand"
  ],
  "result": {
    "item": "engine:glass",
    "count": 1
  }
},
{ "_id": "gold_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": "engine:gold_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:gold_axe",
    "count": 1
  }
}
,
{ "_id": "gold_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": "engine:gold_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:gold_pickaxe",
    "count": 1
  }
}
,
{ "_id": "gold_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": "engine:gold_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:gold_shovel",
    "count": 1
  }
}
,
{ "_id": "grass", 
  "type": "shapeless",
  "ingredients": [
    "engine:leaves"
  ],
  "result": {
    "item": "engine:grass",
    "count": 4
  }
},
{ "_id": "grass_block", 
  "type": "shapeless",
  "ingredients": [
    "engine:dirt",
    "engine:grass"
  ],
  "result": {
    "item": "engine:grass_block"
  }
},
{ "_id": "grass_block_slabs", 
  "type": "shapeless",
  "ingredients": [
    "engine:grass_block"
  ],
  "result": {
    "item": "engine:grass_block_slabs",
    "count": 2
  }
},
{ "_id": "grass_block_slabs_2", 
  "type": "shapeless",
  "ingredients": [
    "engine:dirt_slabs",
    "engine:grass"
  ],
  "result": {
    "item": "engine:grass_block_slabs"
  }
},
{ "_id": "grass_block_stairs", 
  "type": "shaped",
  "pattern": [
    "A ",
    "AA"
  ],
  "keys": {
    "A": "engine:grass_block"
  },
  "result": {
    "item": "engine:grass_block_stairs",
    "count": 4
  }
},
{ "_id": "grass_block_stairs_2", 
  "type": "shapeless",
  "ingredients": [
    "engine:dirt_stairs",
    "engine:grass"
  ],
  "result": {
    "item": "engine:grass_block_stairs"
  }
},
{ "_id": "iron_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": "engine:iron_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:iron_axe",
    "count": 1
  }
}
,
{ "_id": "iron_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": "engine:iron_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:iron_pickaxe",
    "count": 1
  }
}
,
{ "_id": "iron_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": "engine:iron_ingot",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:iron_shovel",
    "count": 1
  }
}
,
{ "_id": "mahogany_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:mahogany_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:mahogany_chest"
  }
}
,
{ "_id": "mahogany_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:mahogany_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:mahogany_door"
  }
}
,
{ "_id": "mahogany_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:mahogany_log"
  ],
  "result": {
    "item": "engine:mahogany_planks",
    "count": 4
  }
}
,
{ "_id": "mahogany_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:mahogany_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:mahogany_slabs"
  }
}
,
{ "_id": "mahogany_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:mahogany_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:mahogany_stairs"
  }
}
,
{ "_id": "mahogany_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:mahogany_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:mahogany_trapdoor"
  }
}
,
{ "_id": "oak_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:oak_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:oak_chest"
  }
}
,
{ "_id": "oak_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:oak_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:oak_door"
  }
}
,
{ "_id": "oak_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:oak_log"
  ],
  "result": {
    "item": "engine:oak_planks",
    "count": 4
  }
},
{ "_id": "oak_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:oak_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:oak_slabs"
  }
}
,
{ "_id": "oak_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:oak_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:oak_stairs"
  }
}
,
{ "_id": "oak_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:oak_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:oak_trapdoor"
  }
}
,
{ "_id": "palm_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:palm_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:palm_chest"
  }
}
,
{ "_id": "palm_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:palm_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:palm_door"
  }
}
,
{ "_id": "palm_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:palm_log"
  ],
  "result": {
    "item": "engine:palm_planks",
    "count": 4
  }
}
,
{ "_id": "palm_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:palm_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:palm_slabs"
  }
}
,
{ "_id": "palm_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:palm_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:palm_stairs"
  }
}
,
{ "_id": "palm_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:palm_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:palm_trapdoor"
  }
}
,
{ "_id": "pine_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:pine_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:pine_chest"
  }
}
,
{ "_id": "pine_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:pine_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:pine_door"
  }
}
,
{ "_id": "pine_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:pine_log"
  ],
  "result": {
    "item": "engine:pine_planks",
    "count": 4
  }
}
,
{ "_id": "pine_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:pine_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:pine_slabs"
  }
}
,
{ "_id": "pine_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:pine_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:pine_stairs"
  }
}
,
{ "_id": "pine_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:pine_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:pine_trapdoor"
  }
}
,
{ "_id": "sticks", 
  "type": "shaped",
  "pattern": [
    "A",
    "A"
  ],
  "keys": {
    "A": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks"
    ]
  },
  "result": {
    "item": "engine:sticks",
    "count": 4
  }
},
{ "_id": "stone_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": "engine:cobblestone",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:stone_axe",
    "count": 1
  }
}
,
{ "_id": "stone_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": "engine:cobblestone",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:stone_pickaxe",
    "count": 1
  }
}
,
{ "_id": "stone_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": "engine:cobblestone",
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:stone_shovel",
    "count": 1
  }
}
,
{ "_id": "stone_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:stone"
  },
  "result": {
    "count": 6,
    "item": "engine:stone_slabs"
  }
},
{ "_id": "stone_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:stone"
  },
  "result": {
    "count": 4,
    "item": "engine:stone_stairs"
  }
},
{ "_id": "torch", 
  "type": "shaped",
  "pattern": [
    "A",
    "B"
  ],
  "keys": {
    "A": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks",
      "engine:coal"
    ],
    "B": "engine:sticks"
  },
  "result": {
    "item": "engine:torch",
    "count": 4
  }
},
{ "_id": "willow_chest", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "A A",
    "AAA"
  ],
  "keys": {
    "A": "engine:willow_planks"
  },
  "result": {
    "count": 1,
    "item": "engine:willow_chest"
  }
}
,
{ "_id": "willow_door", 
  "type": "shaped",
  "pattern": [
    "AA",
    "AA",
    "AA"
  ],
  "keys": {
    "A": "engine:willow_planks"
  },
  "result": {
    "count": 3,
    "item": "engine:willow_door"
  }
}
,
{ "_id": "willow_planks", 
  "type": "shapeless",
  "ingredients": [
    "engine:willow_log"
  ],
  "result": {
    "item": "engine:willow_planks",
    "count": 4
  }
}
,
{ "_id": "willow_slabs", 
  "type": "shaped",
  "pattern": [
    "AAA"
  ],
  "keys": {
    "A": "engine:willow_planks"
  },
  "result": {
    "count": 6,
    "item": "engine:willow_slabs"
  }
}
,
{ "_id": "willow_stairs", 
  "type": "shaped",
  "pattern": [
    "A  ",
    "AA ",
    "AAA"
  ],
  "keys": {
    "A": "engine:willow_planks"
  },
  "result": {
    "count": 4,
    "item": "engine:willow_stairs"
  }
}
,
{ "_id": "willow_trapdoor", 
  "type": "shaped",
  "pattern": [
    "AAA",
    "AAA"
  ],
  "keys": {
    "A": "engine:willow_planks"
  },
  "result": {
    "count": 2,
    "item": "engine:willow_trapdoor"
  }
}
,
{ "_id": "wooden_axe", 
  "type": "shaped",
  "pattern": [
    "MM",
    "MS",
    " S"
  ],
  "keys": {
    "M": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks"
    ],
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:wooden_axe",
    "count": 1
  }
}
,
{ "_id": "wooden_pickaxe", 
  "type": "shaped",
  "pattern": [
    "MMM",
    " S ",
    " S "
  ],
  "keys": {
    "M": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks"
    ],
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:wooden_pickaxe",
    "count": 1
  }
}
,
{ "_id": "wooden_shovel", 
  "type": "shaped",
  "pattern": [
    "M",
    "S",
    "S"
  ],
  "keys": {
    "M": [
      "engine:oak_planks",
      "engine:birch_planks",
      "engine:pine_planks",
      "engine:willow_planks",
      "engine:baobab_planks",
      "engine:mahogany_planks",
      "engine:palm_planks"
    ],
    "S": "engine:sticks"
  },
  "result": {
    "item": "engine:wooden_shovel",
    "count": 1
  }
}

],
  loot_tables: [

],
  mechanics: {
  "fluids": "Water is the only fluid in VoxelEngine and uses a complex algorithm to spread.\n\n### Water Levels\nWater internally has 9 stages (Level 0 to 8).\n- **Level 8** is a full Source Block.\n- **Level 7 to 1** are flowing water. For every block water flows horizontally, the level decreases by 1. This means water flows a maximum distance of **7 blocks**.\n- **Level 0** is air (the water dissipates).\n\n### Infinite Water Sources\nJust like in Minecraft, there are infinite water sources in VoxelEngine!\nAn empty space will automatically turn back into a Level 8 Source Block if:\n1. It is adjacent to at least **2 water sources** (Level 8).\n2. The block underneath the space is solid OR is itself a full water source.\n\n### Flow Behavior and Pathfinding\nWater updates every **5 Ticks** (0.25 seconds) and calculates its path.\nIt prioritizes drops/cliffs:\n1. **Falling Downwards:** If there is air beneath the water, it immediately drops straight down. It forms a vertical stream (Level 7) and will not spread horizontally on that level.\n2. **Smart Pathfinding:** When water spreads horizontally, it scans a radius of up to 4 blocks to check if there is a drop (a hole) nearby. If it finds a hole, the water flows **directly** towards it, rather than spreading out in a circle in all directions!\n\n### Block Destruction\nWater is destructive to weak blocks! If water flows over plants (like grass, dandelions, saplings, etc.), the water automatically breaks these blocks. The items pop out and float away.\n",
  "gravity": "Gravity is not the same for all blocks in VoxelEngine. Most blocks magically float in the air, but some obey the laws of physics!\n\n### Which Blocks Fall?\nAll blocks that inherit from the `GravityBlock` class are affected by gravity. Currently, this mainly includes:\n- **Sand**\n- **Gravel**\n\n### How Does Falling Work?\nAs soon as a block underneath a gravity-affected block is broken (or changed), the block waits exactly **2 Ticks** (0.1 seconds).\nIf there is still air underneath it after this short delay, the block transforms into a dynamic object (a `FallingBlockEntity`) and begins to fall.\n\nThe object accelerates downwards due to the game's defined gravity until it hits a solid surface. Upon impact, it turns back into a normal, solid block.\n\n### Fall Damage for Players!\n> [!CAUTION]\n> Be careful in caves! If a falling sand or gravel block lands on a player, it deals extreme damage!\n> As long as the block is falling and is inside the player's hitbox, the player takes **2.0 Damage (1 full heart)** per tick.\n> This means a block falling on your head can take you out in a fraction of a second!\n",
  "smelting": "The Furnace is one of the most important utility blocks in the game. Here you can find out exactly how the internal timers and rules are calculated.\n\n### Smelting Speed (Cook Time)\nBy default, it takes **200 Ticks (10 seconds)** to smelt a single item in the furnace.\nThis value can theoretically be overridden by special recipes (`furnace_recipes.json`), but 10 seconds is the hardcoded default for almost all ores and food.\n\n### Fuel\nTo operate the furnace, a valid fuel must be placed in the bottom slot. The exact burn times are defined in the `furnace_fuels.json`.\nAs soon as a smelting process can be started, **one** fuel item is consumed and the furnace stores the burn time.\n* Coal, for example, burns long enough to smelt multiple items in a row.\n* Wooden planks or sticks burn for a much shorter duration.\n\nAs long as the `burnTime` counter is greater than 0, the furnace lights up (Light Level 13) and the `cookTime` of the current item increases.\n\n### Special Rules\n* **Output Limit:** The furnace will only continue smelting if the resulting item can fit in the output slot on the right (the Max Stack Size must not be exceeded).\n* **Destruction:** If you break a furnace, all items currently inside it will fly out into the world in random directions. Nothing is ever lost!\n",
  "tree_growth": "The growth of trees in VoxelEngine is an organic process that happens in multiple phases. Here is a detailed breakdown of how the system works.\n\n### Growth Conditions\nFor a sapling to grow, the following conditions must be met:\n- **Ground:** The sapling must be planted on a `grass_block` or `dirt`.\n- **Growth Stages:** A sapling has two stages (Stage 0 and Stage 1). When planted, it starts at Stage 0.\n\n### Growth Timers (Ticks)\nEvery tree species has a defined minimum and maximum growth time (in seconds) in the configuration. A tick equals one-twentieth of a second (20 Ticks = 1 Second).\nWhen a sapling is planted or changes its stage, a random timer between the minimum and maximum time is chosen.\n*Note: You can view the exact times for saplings in the JSON configuration. On average, a stage takes a few minutes.*\n\n### The Growth Process\n1. **Planting:** The sapling is placed. The timer to reach Stage 1 starts.\n2. **Stage 1:** The sapling reaches Stage 1. Visually, nothing changes, but the timer restarts for the final tree generation.\n3. **Tree Generation:** Once the second timer expires, the tree sprouts.\n   - The grass block underneath the sapling is converted to dirt.\n   - The sapling disappears and is replaced by logs and leaves.\n   - **Pine Trees Exception:** When a pine sapling grows, there is a **50% chance** that it will generate as an exceptionally tall pine tree (`alpha_tall_pine`) instead of a normal one!\n\n### Farming Saplings\nTrees drop new saplings when you break their leaves. Breaking logs does not trigger an automatic leaf decay; leaves must be broken manually to roll for a sapling drop.\n",
  "world_generation": "VoxelEngine features a highly advanced, infinite procedural world generation system. The terrain and biomes are entirely driven by a sophisticated mathematical model known as **MultiNoise**.\n\n### The 5 Dimensions of MultiNoise\nRather than relying on simple 2D heatmaps (like older generation techniques), the VoxelEngine determines which biome belongs where by calculating a 5-dimensional coordinate for every point in the world. The dimensions are:\n1. **Temperature:** Ranges from freezing cold to scorching hot.\n2. **Humidity:** Determines if an area is a dry desert or a lush, rainy forest.\n3. **Continentalness:** Dictates whether a point is an ocean, a shore, flat inland, or deep inland.\n4. **Erosion:** High erosion creates flatter, worn-down terrain, while low erosion allows for jagged, towering mountains.\n5. **Weirdness:** Used for rare, unique biome variants and bizarre terrain formations.\n\n### How Biomes are Chosen\nEvery biome in the game (defined in `assets/world/biomes.json`) is assigned an ideal \"target point\" within these 5 dimensions.\nWhen the game generates a new chunk, it calculates the noise values for all 5 dimensions at that specific location. It then compares these values against every single biome to find the one with the lowest \"fitness distance\" (the closest match). That biome is then selected for that column of blocks!\n\n### Seamless Blending\nBecause biomes are selected based on continuous noise functions, the transition between them is completely natural. You won't find a snowy tundra directly bordering a hot desert; there will always be mathematically appropriate transitional biomes in between.\nAdditionally, the terrain parameters (Base Height and Height Variation) are mathematically blended across biome borders. This means a flat plains biome gracefully ramps up into a mountainous biome, avoiding unnatural, sheer cliffs at biome borders.\n"
},
  blocks_md: {
  "baobab_chest": "This is the official wiki page for **Baobab Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_door": "This is the official wiki page for **Baobab Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_leaves": "This is the official wiki page for **Baobab Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_log": "This is the official wiki page for **Baobab Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "baobab_planks": "This is the official wiki page for **Baobab Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_sapling": "This is the official wiki page for **Baobab Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_slabs": "This is the official wiki page for **Baobab Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_stairs": "This is the official wiki page for **Baobab Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "baobab_trapdoor": "This is the official wiki page for **Baobab Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "bedrock": "This is the official wiki page for **Bedrock**!\n\n### Description\nAn absolutely indestructible block that marks the very bottom of the world. Nothing can break this barrier.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_chest": "This is the official wiki page for **Birch Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_door": "This is the official wiki page for **Birch Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_leaves": "This is the official wiki page for **Birch Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_log": "This is the official wiki page for **Birch Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "birch_planks": "This is the official wiki page for **Birch Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_sapling": "This is the official wiki page for **Birch Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_slabs": "This is the official wiki page for **Birch Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_stairs": "This is the official wiki page for **Birch Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "birch_trapdoor": "This is the official wiki page for **Birch Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "bricks": "This is the official wiki page for **Bricks**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "bricks_slabs": "This is the official wiki page for **Bricks Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "bricks_stairs": "This is the official wiki page for **Bricks Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "coal_ore": "This is the official wiki page for **Coal Ore**!\n\n### Description\nAn essential resource found deep underground. Indispensable as fuel for the furnace or for crafting torches.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "cobblestone": "This is the official wiki page for **Cobblestone**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "cobblestone_slabs": "This is the official wiki page for **Cobblestone Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "cobblestone_stairs": "This is the official wiki page for **Cobblestone Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "copper_ore": "This is the official wiki page for **Copper Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "crafting_table": "This is the official wiki page for **Crafting Table**!\n\n### Description\nThe heart of every adventure! Expands your inventory crafting grid from 2x2 to 3x3, allowing you to build tools and better items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dandelion": "This is the official wiki page for **Dandelion**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "diamond_ore": "This is the official wiki page for **Diamond Ore**!\n\n### Description\nThe most precious material in VoxelEngine! Extremely rare and only found deep underground. Used for the strongest tools and armor.\n\n### Trivia\n* Diamonds are a player's best friend when it comes to durability.\n",
  "dirt": "This is the official wiki page for **Dirt**!\n\n### Description\nDirt is one of the most common surface blocks. When exposed to sunlight and near grass, fresh grass will eventually grow on it.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dirt_slabs": "This is the official wiki page for **Dirt Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dirt_stairs": "This is the official wiki page for **Dirt Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite": "This is the official wiki page for **Dolomite**!\n\n### Description\nA special, particularly hard type of rock often generated in deep areas of the world.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite_coal_ore": "This is the official wiki page for **Dolomite Coal Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite_copper_ore": "This is the official wiki page for **Dolomite Copper Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite_diamond_ore": "This is the official wiki page for **Dolomite Diamond Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Diamonds are a player's best friend when it comes to durability.\n",
  "dolomite_gold_ore": "This is the official wiki page for **Dolomite Gold Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite_iron_ore": "This is the official wiki page for **Dolomite Iron Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dolomite_zinc_ore": "This is the official wiki page for **Dolomite Zinc Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "dotty": "This is the official wiki page for **Dotty**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "fairy_bell": "This is the official wiki page for **Fairy Bell**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "furnace": "This is the official wiki page for **Furnace**!\n\n### Description\nAn oven powered by coal or wood. It smelts ores into ingots and bakes sand into glass.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "glass": "This is the official wiki page for **Glass**!\n\n### Description\nA transparent block made from smelting sand. Ideal for windows and beautiful buildings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "gold_ore": "This is the official wiki page for **Gold Ore**!\n\n### Description\nA very soft but shiny metal. While not very durable for tools, it is extremely easy to enchant and valuable for special purposes.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "grass": "This is the official wiki page for **Grass**!\n\n### Description\nA block of dirt covered in lush green grass on top. Ideal for agriculture or peaceful landscapes.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "grass_block": "This is the official wiki page for **Grass Block**!\n\n### Description\nA block of dirt covered in lush green grass on top. Ideal for agriculture or peaceful landscapes.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "grass_block_slabs": "This is the official wiki page for **Grass Block Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "grass_block_stairs": "This is the official wiki page for **Grass Block Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "gravel": "This is the official wiki page for **Gravel**!\n\n### Description\nLike sand, gravel is affected by gravity. Often found in caves or near water.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "iron_ore": "This is the official wiki page for **Iron Ore**!\n\n### Description\nAn important metal. It must be smelted in a furnace before it can be forged into robust tools or armor.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_chest": "This is the official wiki page for **Mahogany Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_door": "This is the official wiki page for **Mahogany Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_leaves": "This is the official wiki page for **Mahogany Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_log": "This is the official wiki page for **Mahogany Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "mahogany_planks": "This is the official wiki page for **Mahogany Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_sapling": "This is the official wiki page for **Mahogany Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_slabs": "This is the official wiki page for **Mahogany Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_stairs": "This is the official wiki page for **Mahogany Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mahogany_trapdoor": "This is the official wiki page for **Mahogany Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "mavvinilia": "This is the official wiki page for **Mavvinilia**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_chest": "This is the official wiki page for **Oak Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_door": "This is the official wiki page for **Oak Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_leaves": "This is the official wiki page for **Oak Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_log": "This is the official wiki page for **Oak Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "oak_planks": "This is the official wiki page for **Oak Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_sapling": "This is the official wiki page for **Oak Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_slabs": "This is the official wiki page for **Oak Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_stairs": "This is the official wiki page for **Oak Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "oak_trapdoor": "This is the official wiki page for **Oak Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_chest": "This is the official wiki page for **Palm Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_door": "This is the official wiki page for **Palm Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_leaves": "This is the official wiki page for **Palm Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_log": "This is the official wiki page for **Palm Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "palm_planks": "This is the official wiki page for **Palm Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_sapling": "This is the official wiki page for **Palm Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_slabs": "This is the official wiki page for **Palm Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_stairs": "This is the official wiki page for **Palm Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "palm_trapdoor": "This is the official wiki page for **Palm Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_chest": "This is the official wiki page for **Pine Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_door": "This is the official wiki page for **Pine Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_leaves": "This is the official wiki page for **Pine Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_log": "This is the official wiki page for **Pine Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "pine_planks": "This is the official wiki page for **Pine Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_sapling": "This is the official wiki page for **Pine Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_slabs": "This is the official wiki page for **Pine Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_stairs": "This is the official wiki page for **Pine Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "pine_trapdoor": "This is the official wiki page for **Pine Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "poppy": "This is the official wiki page for **Poppy**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "purple_tulip": "This is the official wiki page for **Purple Tulip**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "red_tulip": "This is the official wiki page for **Red Tulip**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "sand": "This is the official wiki page for **Sand**!\n\n### Description\nA block affected by gravity that falls downwards. Very commonly found on beaches and in deserts.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "sandy_grass": "This is the official wiki page for **Sandy Grass**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "stone": "This is the official wiki page for **Stone**!\n\n### Description\nA solid stone block that makes up the majority of the underground world. Mining it without special tools yields cobblestone.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "stone_slabs": "This is the official wiki page for **Stone Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "stone_stairs": "This is the official wiki page for **Stone Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "torch": "This is the official wiki page for **Torch**!\n\n### Description\nA fascinating block in VoxelEngine. You can check out its exact physical properties in the infobox on the right side.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "water": "This is the official wiki page for **Water**!\n\n### Description\nA clear fluid that spreads outwards. Perfect for swimming, irrigating fields, or cushioning dangerous falls.\n\n### Trivia\n* Falling into water from any height completely negates all fall damage.\n",
  "willow_chest": "This is the official wiki page for **Willow Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_door": "This is the official wiki page for **Willow Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_leaves": "This is the official wiki page for **Willow Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_log": "This is the official wiki page for **Willow Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Wood is the first material every new player should gather. Punch a tree!\n",
  "willow_planks": "This is the official wiki page for **Willow Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_sapling": "This is the official wiki page for **Willow Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_slabs": "This is the official wiki page for **Willow Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_stairs": "This is the official wiki page for **Willow Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "willow_trapdoor": "This is the official wiki page for **Willow Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "zinc_ore": "This is the official wiki page for **Zinc Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n"
},
  items_md: {
  "Baobab Chest": "This is the official wiki page for **Baobab Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Door": "This is the official wiki page for **Baobab Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Leaves": "This is the official wiki page for **Baobab Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Log": "This is the official wiki page for **Baobab Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Planks": "This is the official wiki page for **Baobab Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Sapling": "This is the official wiki page for **Baobab Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Slabs": "This is the official wiki page for **Baobab Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Stairs": "This is the official wiki page for **Baobab Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Baobab Trapdoor": "This is the official wiki page for **Baobab Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Bedrock": "This is the official wiki page for **Bedrock**!\n\n### Description\nAn absolutely indestructible block that marks the very bottom of the world. Nothing can break this barrier.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Chest": "This is the official wiki page for **Birch Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Door": "This is the official wiki page for **Birch Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Leaves": "This is the official wiki page for **Birch Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Log": "This is the official wiki page for **Birch Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Planks": "This is the official wiki page for **Birch Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Sapling": "This is the official wiki page for **Birch Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Slabs": "This is the official wiki page for **Birch Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Stairs": "This is the official wiki page for **Birch Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Birch Trapdoor": "This is the official wiki page for **Birch Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Bricks Slabs": "This is the official wiki page for **Bricks Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Bricks Stairs": "This is the official wiki page for **Bricks Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Bricks": "This is the official wiki page for **Bricks**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Bucket": "This is the official wiki page for **Bucket**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Coal Ore": "This is the official wiki page for **Coal Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Coal": "This is the official wiki page for **Coal**!\n\n### Description\nAn essential resource found deep underground. Indispensable as fuel for the furnace or for crafting torches.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Cobblestone Slabs": "This is the official wiki page for **Cobblestone Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Cobblestone Stairs": "This is the official wiki page for **Cobblestone Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Cobblestone": "This is the official wiki page for **Cobblestone**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Copper Axe": "This is the official wiki page for **Copper Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Copper Ingot": "This is the official wiki page for **Copper Ingot**!\n\n### Description\nA smelted metal bar, ready to be processed further on a crafting table.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Copper Ore": "This is the official wiki page for **Copper Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Copper Pickaxe": "This is the official wiki page for **Copper Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Copper Shovel": "This is the official wiki page for **Copper Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Crafting Table": "This is the official wiki page for **Crafting Table**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dandelion": "This is the official wiki page for **Dandelion**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Diamond Axe": "This is the official wiki page for **Diamond Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Diamond Ore": "This is the official wiki page for **Diamond Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Diamond Pickaxe": "This is the official wiki page for **Diamond Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Diamond Shovel": "This is the official wiki page for **Diamond Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Diamond": "This is the official wiki page for **Diamond**!\n\n### Description\nThe most precious material in VoxelEngine! Extremely rare and only found deep underground. Used for the strongest tools and armor.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dirt Slabs": "This is the official wiki page for **Dirt Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dirt Stairs": "This is the official wiki page for **Dirt Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dirt": "This is the official wiki page for **Dirt**!\n\n### Description\nDirt is one of the most common surface blocks. When exposed to sunlight and near grass, fresh grass will eventually grow on it.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Coal Ore": "This is the official wiki page for **Dolomite Coal Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Copper Ore": "This is the official wiki page for **Dolomite Copper Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Diamond Ore": "This is the official wiki page for **Dolomite Diamond Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Gold Ore": "This is the official wiki page for **Dolomite Gold Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Iron Ore": "This is the official wiki page for **Dolomite Iron Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite Zinc Ore": "This is the official wiki page for **Dolomite Zinc Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dolomite": "This is the official wiki page for **Dolomite**!\n\n### Description\nA special, particularly hard type of rock often generated in deep areas of the world.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Dotty": "This is the official wiki page for **Dotty**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Fairy Bell": "This is the official wiki page for **Fairy Bell**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Furnace": "This is the official wiki page for **Furnace**!\n\n### Description\nAn oven powered by coal or wood. It smelts ores into ingots and bakes sand into glass.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Glass": "This is the official wiki page for **Glass**!\n\n### Description\nA transparent block made from smelting sand. Ideal for windows and beautiful buildings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gold Axe": "This is the official wiki page for **Gold Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gold Ingot": "This is the official wiki page for **Gold Ingot**!\n\n### Description\nA smelted metal bar, ready to be processed further on a crafting table.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gold Ore": "This is the official wiki page for **Gold Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gold Pickaxe": "This is the official wiki page for **Gold Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gold Shovel": "This is the official wiki page for **Gold Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Grass Slabs": "This is the official wiki page for **Grass Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Grass Stairs": "This is the official wiki page for **Grass Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Grass": "This is the official wiki page for **Grass**!\n\n### Description\nA block of dirt covered in lush green grass on top. Ideal for agriculture or peaceful landscapes.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Gravel": "This is the official wiki page for **Gravel**!\n\n### Description\nLike sand, gravel is affected by gravity. Often found in caves or near water.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Iron Axe": "This is the official wiki page for **Iron Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Iron Ingot": "This is the official wiki page for **Iron Ingot**!\n\n### Description\nA smelted metal bar, ready to be processed further on a crafting table.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Iron Ore": "This is the official wiki page for **Iron Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Iron Pickaxe": "This is the official wiki page for **Iron Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Iron Shovel": "This is the official wiki page for **Iron Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Chest": "This is the official wiki page for **Mahogany Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Door": "This is the official wiki page for **Mahogany Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Leaves": "This is the official wiki page for **Mahogany Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Log": "This is the official wiki page for **Mahogany Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Planks": "This is the official wiki page for **Mahogany Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Sapling": "This is the official wiki page for **Mahogany Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Slabs": "This is the official wiki page for **Mahogany Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Stairs": "This is the official wiki page for **Mahogany Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mahogany Trapdoor": "This is the official wiki page for **Mahogany Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Mavvinilia": "This is the official wiki page for **Mavvinilia**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Chest": "This is the official wiki page for **Oak Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Door": "This is the official wiki page for **Oak Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Leaves": "This is the official wiki page for **Oak Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Log": "This is the official wiki page for **Oak Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Planks": "This is the official wiki page for **Oak Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Sapling": "This is the official wiki page for **Oak Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Slabs": "This is the official wiki page for **Oak Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Stairs": "This is the official wiki page for **Oak Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Oak Trapdoor": "This is the official wiki page for **Oak Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Chest": "This is the official wiki page for **Palm Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Door": "This is the official wiki page for **Palm Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Leaves": "This is the official wiki page for **Palm Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Log": "This is the official wiki page for **Palm Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Planks": "This is the official wiki page for **Palm Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Sapling": "This is the official wiki page for **Palm Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Slabs": "This is the official wiki page for **Palm Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Stairs": "This is the official wiki page for **Palm Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Palm Trapdoor": "This is the official wiki page for **Palm Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Chest": "This is the official wiki page for **Pine Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Door": "This is the official wiki page for **Pine Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Leaves": "This is the official wiki page for **Pine Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Log": "This is the official wiki page for **Pine Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Planks": "This is the official wiki page for **Pine Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Sapling": "This is the official wiki page for **Pine Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Slabs": "This is the official wiki page for **Pine Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Stairs": "This is the official wiki page for **Pine Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Pine Trapdoor": "This is the official wiki page for **Pine Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Poppy": "This is the official wiki page for **Poppy**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Purple Tulip": "This is the official wiki page for **Purple Tulip**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Raw Copper": "This is the official wiki page for **Raw Copper**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Raw Gold": "This is the official wiki page for **Raw Gold**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Raw Iron": "This is the official wiki page for **Raw Iron**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Raw Zinc": "This is the official wiki page for **Raw Zinc**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Red Tulip": "This is the official wiki page for **Red Tulip**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Sand": "This is the official wiki page for **Sand**!\n\n### Description\nA block affected by gravity that falls downwards. Very commonly found on beaches and in deserts.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Sandy Grass": "This is the official wiki page for **Sandy Grass**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Sticks": "This is the official wiki page for **Sticks**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone Axe": "This is the official wiki page for **Stone Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone Pickaxe": "This is the official wiki page for **Stone Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone Shovel": "This is the official wiki page for **Stone Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone Slabs": "This is the official wiki page for **Stone Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone Stairs": "This is the official wiki page for **Stone Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Stone": "This is the official wiki page for **Stone**!\n\n### Description\nA solid stone block that makes up the majority of the underground world. Mining it without special tools yields cobblestone.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Torch": "This is the official wiki page for **Torch**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Water bucket": "This is the official wiki page for **Water bucket**!\n\n### Description\nA practical item in VoxelEngine. Experiment with it on a crafting table to see what you can make!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Chest": "This is the official wiki page for **Willow Chest**!\n\n### Description\nA wooden container where you can safely store your valuable items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Door": "This is the official wiki page for **Willow Door**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Leaves": "This is the official wiki page for **Willow Leaves**!\n\n### Description\nThe leaves of a tree. They can be broken and have a chance to drop saplings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Log": "This is the official wiki page for **Willow Log**!\n\n### Description\nA solid tree trunk found in forests. It is the fundamental building block for wooden planks and sticks.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Planks": "This is the official wiki page for **Willow Planks**!\n\n### Description\nProcessed wood. Perfect for building houses, tools, and other basic items.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Sapling": "This is the official wiki page for **Willow Sapling**!\n\n### Description\nA young tree sapling. Plant it on dirt or grass and give it some time to grow into a magnificent tree.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Slabs": "This is the official wiki page for **Willow Slabs**!\n\n### Description\nA half-block. Ideal for delicate structures, floors, or stair landings.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Stairs": "This is the official wiki page for **Willow Stairs**!\n\n### Description\nPractical stair blocks used to build smooth ascents or beautiful roofs.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Willow Trapdoor": "This is the official wiki page for **Willow Trapdoor**!\n\n### Description\nA mechanism to protect entrances, separate rooms, and keep unwanted monsters out.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Wooden Axe": "This is the official wiki page for **Wooden Axe**!\n\n### Description\nA tool specifically designed to chop wooden blocks like logs and planks exceptionally fast.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Wooden Pickaxe": "This is the official wiki page for **Wooden Pickaxe**!\n\n### Description\nAn essential tool for efficiently mining stone and hard ores. Without a pickaxe, there is no progress!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Wooden Shovel": "This is the official wiki page for **Wooden Shovel**!\n\n### Description\nPerfectly suited to clear soft materials like dirt, sand, or gravel at lightning speed.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Zinc Ingot": "This is the official wiki page for **Zinc Ingot**!\n\n### Description\nA smelted metal bar, ready to be processed further on a crafting table.\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n",
  "Zinc Ore": "This is the official wiki page for **Zinc Ore**!\n\n### Description\nA massive block of rock with valuable minerals trapped inside. Mine it and smelt it!\n\n### Trivia\n* Developer Trivia: This item was carefully defined in the blocks.json / items.json files.\n"
}
};
