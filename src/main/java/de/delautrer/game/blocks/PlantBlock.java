package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class PlantBlock extends Block {

    public PlantBlock() {
        super(false, true, true, true);
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = getModel().top;
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
        if (neighborPos.x == x && neighborPos.y == y - 1 && neighborPos.z == z) {
            Block blockBelow = BlockRegistry.get(newNeighborId);
            if (!blockBelow.isSolid) {
                world.setBlock(x, y, z, BlockRegistry.AIR.getId());
            }
        }
    }
}