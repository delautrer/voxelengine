package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.entity.ItemEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class TorchBlock extends CubeBlock {

    public enum TorchAttach { FLOOR, NORTH, SOUTH, EAST, WEST }
    public static final EnumProperty<TorchAttach> ATTACH = EnumProperty.create("attach", TorchAttach.class);

    // Speicher für die vorberechneten rotierten Eckpunkte (Maximale Performance!)
    private static final Vector3f[][] VERTS = new Vector3f[5][8];

    static {
        float w = 1f / 16f;  // Fackel ist 2 Pixel breit
        float h = 10f / 16f; // Fackel ist 10 Pixel hoch

        // Die 8 Eckpunkte einer unrotierten Fackel im Ursprung (0,0,0)
        Vector3f[] base = new Vector3f[]{
                new Vector3f(-w, 0, -w), new Vector3f(w, 0, -w), new Vector3f(w, 0, w), new Vector3f(-w, 0, w), // Unten
                new Vector3f(-w, h, -w), new Vector3f(w, h, -w), new Vector3f(w, h, w), new Vector3f(-w, h, w)  // Oben
        };

        // Wir berechnen die Schrägen für alle 5 Zustände
        for (TorchAttach attach : TorchAttach.values()) {
            Matrix4f mat = new Matrix4f();

            if (attach == TorchAttach.FLOOR) {
                mat.translate(0.5f, 0.0f, 0.5f);
            } else if (attach == TorchAttach.NORTH) { // Hängt an Z=0 Wand (Lehnt nach Süd)
                mat.translate(0.5f, 0.2f, 0.0f).rotateX((float) Math.toRadians(25));
            } else if (attach == TorchAttach.SOUTH) { // Hängt an Z=1 Wand (Lehnt nach Nord)
                mat.translate(0.5f, 0.2f, 1.0f).rotateX((float) Math.toRadians(-25));
            } else if (attach == TorchAttach.WEST) {  // Hängt an X=0 Wand (Lehnt nach Ost)
                mat.translate(0.0f, 0.2f, 0.5f).rotateZ((float) Math.toRadians(-25));
            } else if (attach == TorchAttach.EAST) {  // Hängt an X=1 Wand (Lehnt nach West)
                mat.translate(1.0f, 0.2f, 0.5f).rotateZ((float) Math.toRadians(25));
            }

            VERTS[attach.ordinal()] = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                VERTS[attach.ordinal()][i] = mat.transformPosition(new Vector3f(base[i]));
            }
        }
    }

    public TorchBlock() {
        super(false, true, true);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(ATTACH);
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        if (hitFace.y == -1) return null;

        Vector3i wallPos = new Vector3i(hitPos).sub(hitFace);
        Block wallBlock = world.getBlockState(wallPos.x, wallPos.y, wallPos.z).getBlock();

        if (!wallBlock.isSolid) return null;

        if (hitFace.y == 1) return getDefaultState().with(ATTACH, TorchAttach.FLOOR);
        if (hitFace.z == 1) return getDefaultState().with(ATTACH, TorchAttach.NORTH);
        if (hitFace.z == -1) return getDefaultState().with(ATTACH, TorchAttach.SOUTH);
        if (hitFace.x == 1) return getDefaultState().with(ATTACH, TorchAttach.WEST);
        if (hitFace.x == -1) return getDefaultState().with(ATTACH, TorchAttach.EAST);

        return null;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        BlockState state = world.getBlockState(x, y, z);
        TorchAttach attach = state.getValue(ATTACH);

        Vector3i wallPos = new Vector3i(x, y, z);
        if (attach == TorchAttach.FLOOR) wallPos.y--;
        else if (attach == TorchAttach.NORTH) wallPos.z--;
        else if (attach == TorchAttach.SOUTH) wallPos.z++;
        else if (attach == TorchAttach.WEST) wallPos.x--;
        else if (attach == TorchAttach.EAST) wallPos.x++;

        Block wallBlock = world.getBlockState(wallPos.x, wallPos.y, wallPos.z).getBlock();
        if (!wallBlock.isSolid) {
            dropAsItem(world, x, y, z); // NEU: Fackel droppt, bevor sie verschwindet
            world.setBlockState(x, y, z, BlockRegistry.AIR.getDefaultState());
        }
    }

    /**
     * Zerstört die Fackel physikalisch und spawnt ihr Item basierend auf der Loot-Tabelle.
     */
    private void dropAsItem(World world, int x, int y, int z) {
        String lootPath = this.getLootTable();

        if (lootPath != null) {
            LootTable table = LootTableManager.load(lootPath);
            if (table != null) {
                List<ItemStack> drops = table.generateLoot();

                for (ItemStack stack : drops) {
                    Vector3f dropPos = new Vector3f(
                            x + 0.5f,
                            y + 0.5f,
                            z + 0.5f
                    );

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

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        TorchAttach attach = state.getValue(ATTACH);
        if (attach == TorchAttach.FLOOR) return List.of(new AABB(new Vector3f(0.4f, 0.0f, 0.4f), new Vector3f(0.6f, 0.6f, 0.6f)));
        if (attach == TorchAttach.NORTH) return List.of(new AABB(new Vector3f(0.4f, 0.2f, 0.0f), new Vector3f(0.6f, 0.8f, 0.3f)));
        if (attach == TorchAttach.SOUTH) return List.of(new AABB(new Vector3f(0.4f, 0.2f, 0.7f), new Vector3f(0.6f, 0.8f, 1.0f)));
        if (attach == TorchAttach.WEST)  return List.of(new AABB(new Vector3f(0.0f, 0.2f, 0.4f), new Vector3f(0.3f, 0.8f, 0.6f)));
        if (attach == TorchAttach.EAST)  return List.of(new AABB(new Vector3f(0.7f, 0.2f, 0.4f), new Vector3f(1.0f, 0.8f, 0.6f)));
        return super.getBoundingBoxes(state);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        BlockState state = chunk.getBlockState(x, y, z);
        TorchAttach attach = state.getValue(ATTACH);
        Vector3f[] v = VERTS[attach.ordinal()];
        float light = 1.0f;

        de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = getModel().top;
        if (reg == null) return;

        addQuad(chunk, x, y, z, v[4], v[7], v[6], v[5], 7f/16f, 6f/16f, 9f/16f, 8f/16f, reg, light);
        addQuad(chunk, x, y, z, v[3], v[0], v[1], v[2], 7f/16f, 14f/16f, 9f/16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[3], v[2], v[6], v[7], 7f/16f, 6f/16f, 9f/16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[1], v[0], v[4], v[5], 7f/16f, 6f/16f, 9f/16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[0], v[3], v[7], v[4], 7f/16f, 6f/16f, 9f/16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[2], v[1], v[5], v[6], 7f/16f, 6f/16f, 9f/16f, 1.0f, reg, light);
    }

    private void addQuad(Chunk chunk, int x, int y, int z, Vector3f vec0, Vector3f vec1, Vector3f vec2, Vector3f vec3, float lu0, float lv0, float lu1, float lv1, de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg, float light) {
        float u0 = reg.u0 + (reg.u1 - reg.u0) * lu0;
        float v0 = reg.v0 + (reg.v1 - reg.v0) * lv0;
        float u1 = reg.u0 + (reg.u1 - reg.u0) * lu1;
        float v1 = reg.v0 + (reg.v1 - reg.v0) * lv1;
        chunk.addFace(
                x + vec0.x, y + vec0.y, z + vec0.z, 1.0f,
                x + vec1.x, y + vec1.y, z + vec1.z, 1.0f,
                x + vec2.x, y + vec2.y, z + vec2.z, 1.0f,
                x + vec3.x, y + vec3.y, z + vec3.z, 1.0f,
                u0, v0, u1, v1, reg.layer, light, this,
                1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
        );
    }

}