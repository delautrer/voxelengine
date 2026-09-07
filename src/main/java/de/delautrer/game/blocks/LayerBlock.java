package de.delautrer.game.blocks;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.IntProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class LayerBlock extends CubeBlock {
    public static final IntProperty LAYERS = IntProperty.create("layers", 1, 8);

    @SuppressWarnings("this-escape")
    public LayerBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false, isRaycastable=true
        setSoundMaterialName("snow");
        setHardness(0.2f);
        this.mesher = (state, x, y, z, chunk, cm) -> {
            int layers = state.contains(LAYERS) ? state.getValue(LAYERS) : 1;
            float H1 = layers * 0.125f;

            // 1. Render top and bottom faces
            renderBox(state, x, y, z, 0.0f, 0.0f, 0.0f, 1.0f, H1, 1.0f, true, true, false, false, false, false, false, chunk, cm);

            // 2. Render South face (z+1) if H1 > hSouth
            BlockState sSouth = getNeighborState(chunk, cm, x, y, z + 1);
            float hSouth = getNeighborHeight(sSouth);
            if (H1 > hSouth) {
                renderBox(state, x, y, z, 0.0f, hSouth, 0.0f, 1.0f, H1, 1.0f, false, false, false, true, false, false, false, chunk, cm);
            }

            // 3. Render North face (z-1) if H1 > hNorth
            BlockState sNorth = getNeighborState(chunk, cm, x, y, z - 1);
            float hNorth = getNeighborHeight(sNorth);
            if (H1 > hNorth) {
                renderBox(state, x, y, z, 0.0f, hNorth, 0.0f, 1.0f, H1, 1.0f, false, false, true, false, false, false, false, chunk, cm);
            }

            // 4. Render West face (x-1) if H1 > hWest
            BlockState sWest = getNeighborState(chunk, cm, x - 1, y, z);
            float hWest = getNeighborHeight(sWest);
            if (H1 > hWest) {
                renderBox(state, x, y, z, 0.0f, hWest, 0.0f, 1.0f, H1, 1.0f, false, false, false, false, false, true, false, chunk, cm);
            }

            // 5. Render East face (x+1) if H1 > hEast
            BlockState sEast = getNeighborState(chunk, cm, x + 1, y, z);
            float hEast = getNeighborHeight(sEast);
            if (H1 > hEast) {
                renderBox(state, x, y, z, 0.0f, hEast, 0.0f, 1.0f, H1, 1.0f, false, false, false, false, true, false, false, chunk, cm);
            }
        };
    }

    private float getNeighborHeight(BlockState nState) {
        if (nState == null || nState.getBlock() == null || nState.getBlock().isAir()) {
            return 0.0f;
        }
        Block nBlock = nState.getBlock();
        if (nBlock instanceof LayerBlock) {
            int nLayers = nState.contains(LAYERS) ? nState.getValue(LAYERS) : 1;
            return nLayers * 0.125f;
        }
        if (nBlock.isSolid && !nBlock.isTransparent) {
            return 1.0f;
        }
        return 0.0f;
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            return super.shouldRenderFaceAgainstState(myState, neighborState, face);
        }
        if (neighborState == null || neighborState.getBlock() == null || neighborState.getBlock().isAir()) {
            return true;
        }
        Block nBlock = neighborState.getBlock();
        if (nBlock == this) {
            int myLayers = myState.contains(LAYERS) ? myState.getValue(LAYERS) : 1;
            int nLayers = neighborState.contains(LAYERS) ? neighborState.getValue(LAYERS) : 1;
            return myLayers > nLayers;
        }
        if (nBlock.isSolid && !nBlock.isTransparent) {
            return false;
        }
        return super.shouldRenderFaceAgainstState(myState, neighborState, face);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        super.appendProperties(properties);
        properties.add(LAYERS);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        if (item != null && (item.getBlock() == this || item.getBlock() == getFullBlock())) {
            int layers = state.contains(LAYERS) ? state.getValue(LAYERS) : 1;
            if (layers < 8) {
                if (hitFace.y == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        int layers = state.contains(LAYERS) ? state.getValue(LAYERS) : 1;
        float height = layers * 0.125f;
        return List.of(new AABB(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f, height, 1.0f)));
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        return getBoundingBoxes(state);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        BlockState currentState = world.getBlockState(hitPos.x, hitPos.y, hitPos.z);
        if (currentState.getBlock() == this) {
            int currentLayers = currentState.contains(LAYERS) ? currentState.getValue(LAYERS) : 1;
            if (currentLayers + 1 >= 8) {
                Block fullBlock = getFullBlock();
                if (fullBlock != null) {
                    return fullBlock.getDefaultState();
                }
            }
            return currentState.with(LAYERS, currentLayers + 1);
        }

        Block blockBelow = world.getBlock(hitPos.x, hitPos.y - 1, hitPos.z);
        if (blockBelow != null && blockBelow.isSolid && !blockBelow.isPassable) {
            return getDefaultState().with(LAYERS, 1);
        }
        return null;
    }

    private Block getFullBlock() {
        NamespacedKey key = Registries.BLOCKS.getKey(this);
        if (key != null) {
            if (key.getKey().equals("snow")) {
                return Registries.BLOCKS.get("veinstride:snow_block");
            } else if (key.getKey().equals("sand_layer")) {
                return Registries.BLOCKS.get("veinstride:sand");
            }
        }
        return null;
    }

    public boolean scrapeLayerWithShovel(World world, Vector3i pos) {
        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        int layers = state.contains(LAYERS) ? state.getValue(LAYERS) : 1;
        if (layers > 1) {
            world.setBlockWithState(pos.x, pos.y, pos.z, this, state.with(LAYERS, layers - 1).getStateId(), true);
        } else {
            world.setBlock(pos.x, pos.y, pos.z, Registries.BLOCKS.get("veinstride:air"));
        }
        spawnSingleDrop(world, pos);
        SoundManager.playEvent(getSoundMaterialName() != null ? getSoundMaterialName() : "snow", "place", 0.4f);
        return true;
    }

    public void spawnSingleDrop(World world, Vector3i pos) {
        String lootPath = this.getLootTable();
        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();
                for (ItemStack stack : drops) {
                    Vector3d dropPos = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
                    Vector3f dropVel = new Vector3f((float) (Math.random() - 0.5) * 1.5f, 1.5f, (float) (Math.random() - 0.5) * 1.5f);
                    ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        Block blockBelow = world.getBlock(x, y - 1, z);
        if (blockBelow == null || !blockBelow.isSolid || blockBelow.isPassable) {
            dropAsItems(world, x, y, z);
            world.setBlock(x, y, z, Registries.BLOCKS.get("veinstride:air"));
        }
    }

    private void dropAsItems(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(x, y, z);
        int layers = (state != null && state.contains(LAYERS)) ? state.getValue(LAYERS) : 1;
        String lootPath = this.getLootTable();
        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                for (int i = 0; i < layers; i++) {
                    List<ItemStack> drops = table.generateLoot();
                    for (ItemStack stack : drops) {
                        Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                        Vector3f dropVel = new Vector3f((float) (Math.random() - 0.5) * 2.0f, 2.0f, (float) (Math.random() - 0.5) * 2.0f);
                        ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                        world.spawnEntity(entity);
                    }
                }
            }
        }
    }
}
