package de.delautrer.game.blocks;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
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

    public PlantBlock() {
        super(false, true, true, true);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        TextureStitcher.AtlasRegion reg = getModel().top;
        if (reg == null) return;

        float light = 1.0f;
        float sl = chunk.getSmoothSkyLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);
        float bl = chunk.getSmoothBlockLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);
        float slBottom = sl * 0.67f; float blBottom = bl * 0.67f;

        // --- ERSTES KREUZ ---
        chunk.addFace(x, y, z, 1.0f, x + 1, y, z + 1, 1.0f, x + 1, y + 1, z + 1, 1.0f, x, y + 1, z, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slBottom, slBottom, sl, sl, blBottom, blBottom, bl, bl);
        chunk.addFace(x + 1, y, z + 1, 1.0f, x, y, z, 1.0f, x, y + 1, z, 1.0f, x + 1, y + 1, z + 1, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slBottom, slBottom, sl, sl, blBottom, blBottom, bl, bl);

        // --- ZWEITES KREUZ ---
        chunk.addFace(x + 1, y, z, 1.0f, x, y, z + 1, 1.0f, x, y + 1, z + 1, 1.0f, x + 1, y + 1, z, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slBottom, slBottom, sl, sl, blBottom, blBottom, bl, bl);
        chunk.addFace(x, y, z + 1, 1.0f, x + 1, y, z, 1.0f, x + 1, y + 1, z, 1.0f, x, y + 1, z + 1, 1.0f,
                reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slBottom, slBottom, sl, sl, blBottom, blBottom, bl, bl);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return true;
    }

    @Override
    public java.util.List<AABB> getBoundingBoxes(BlockState state) {
        return java.util.List.of(new AABB(new org.joml.Vector3f(4f/16f,0,4f/16f), new org.joml.Vector3f(12f/16f,8f/16f,12f/16f)));
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        // Überprüfen, ob sich der Block direkt UNTER der Pflanze geändert hat
        if (neighborPos.x == x && neighborPos.y == y - 1 && neighborPos.z == z) {
            Block blockBelow = BlockRegistry.get(newNeighborId);

            // Wenn der Block darunter nicht mehr solide ist (z.B. Luft, Wasser etc.)
            if (!blockBelow.isSolid) {
                dropAsItem(world, x, y, z); // NEU: Pflanze droppen
                world.setBlock(x, y, z, Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air").getId()); // Dann erst den Block löschen
            }
        }
    }

    /**
     * Zerstört die Pflanze physikalisch und spawnt ihr Item basierend auf der Loot-Tabelle.
     */
    private void dropAsItem(World world, int x, int y, int z) {
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
                            z + 0.5
                    );

                    // Ein kleiner Schubs nach oben und zufällig zur Seite ("Plopp"-Effekt)
                    Vector3f dropVel = new Vector3f(
                            (float)(Math.random() - 0.5) * 2.0f,
                            2.0f,
                            (float)(Math.random() - 0.5) * 2.0f
                    );

                    ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                    world.spawnEntity(entity);
                }
            }
        }
    }
}
