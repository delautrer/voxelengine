package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.entity.ItemEntity;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.registry.Registries;

public class PlantBlock extends Block {

    @SuppressWarnings("this-escape")
    public PlantBlock() {
        super(false, true, true, true);
        this.mesher = new de.delautrer.engine.graphics.meshing.PlantMesher(this);
    }



    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return true;
    }

    @Override
    public java.util.List<AABB> getBoundingBoxes(BlockState state) {
        return java.util.List.of(new AABB(new org.joml.Vector3f(4f / 16f, 0, 4f / 16f),
                new org.joml.Vector3f(12f / 16f, 8f / 16f, 12f / 16f)));
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    protected boolean canSurviveOn(String blockName) {
        if (blockName.equals("grass_block") || blockName.equals("dirt") || blockName.equals("moss")) {
            return true;
        }
        NamespacedKey myKey = BlockRegistry.REGISTRY.getKey(this);
        if (myKey != null && myKey.getKey().equals("sandy_grass")) {
            return blockName.equals("sand");
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        // Only allow placing on the top face of a block to strictly prevent placing "against" sides
        if (hitFace.y != 1) {
            // Exception: if we are replacing a plant (like tall grass), hitFace might not be top
            BlockState currentState = world.getBlockState(hitPos.x, hitPos.y, hitPos.z);
            if (!currentState.getBlock().canBeReplaced(currentState, null, hitFace, exactHit)) {
                return null;
            }
        }

        Block belowBlock = world.getBlock(hitPos.x, hitPos.y - 1, hitPos.z);
        NamespacedKey belowKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(belowBlock);
        
        if (belowKey != null && canSurviveOn(belowKey.getKey())) {
            return getDefaultState();
        }
        return null;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        if (neighborPos.x == x && neighborPos.y == y - 1 && neighborPos.z == z) {
            Block blockBelow = world.getBlock(x, y - 1, z);
            NamespacedKey belowKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(blockBelow);
            boolean isValid = false;
            
            if (belowKey != null) {
                isValid = canSurviveOn(belowKey.getKey());
            }

            if (!isValid) {
                dropAsItem(world, x, y, z);
                world.setBlock(x, y, z, de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air"));
            }
        }
    }

    /**
     * Zerstört die Pflanze physikalisch und spawnt ihr Item basierend auf der
     * Loot-Tabelle.
     */
    protected void dropAsItem(World world, int x, int y, int z) {
        String lootPath = this.getLootTable();

        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();

                for (ItemStack stack : drops) {
                    // Position zentriert in der Mitte des Blocks
                    Vector3d dropPos = new Vector3d(
                            x + 0.5,
                            y + 0.5,
                            z + 0.5);

                    // Ein kleiner Schubs nach oben und zufällig zur Seite ("Plopp"-Effekt)
                    Vector3f dropVel = new Vector3f(
                            (float) (Math.random() - 0.5) * 2.0f,
                            2.0f,
                            (float) (Math.random() - 0.5) * 2.0f);

                    ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }
}
