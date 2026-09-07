package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockProperties.Half;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class TallPlantBlock extends CubeBlock implements IPairedBlock {

    @SuppressWarnings("this-escape")
    public TallPlantBlock() {
        super(false, true, true, true); // isSolid=false, isTransparent=true, isPassable=true, isRaycastable=true
        setSoundMaterialName("grass");
        setHardness(0.0f);

        this.mesher = (state, x, y, z, chunk, cm) -> {
            Half half = state.getValue(DoorBlock.HALF);
            AtlasRegion reg = (half == Half.BOTTOM)
                ? (getModel().bottom != null ? getModel().bottom : (getModel().side_bottom != null ? getModel().side_bottom : getModel().top))
                : (getModel().top != null ? getModel().top : (getModel().side_top != null ? getModel().side_top : getModel().bottom));
            if (reg == null) return;

            int baseY = (half == Half.BOTTOM) ? y : y - 1;
            int topY = baseY + 1;
            long seed = ((long) x * 3129871L) ^ ((long) z * 116129781L) ^ ((long) baseY * 42317861L);
            seed = seed * seed * 42317861L + seed * 11L;
            float offX = (((float) (seed >> 16 & 15L) / 15.0f) - 0.5f) * 0.3f;
            float offZ = (((float) (seed >> 24 & 15L) / 15.0f) - 0.5f) * 0.3f;

            float x0 = x + offX;
            float x1 = x + 1 + offX;
            float z0 = z + offZ;
            float z1 = z + 1 + offZ;

            float light = 1.0f;
            float sl = Math.max(
                chunk.getSmoothSkyLight(x, baseY, z, 0, 0, 0, 0, 0, 0, cm),
                chunk.getSmoothSkyLight(x, topY, z, 0, 0, 0, 0, 0, 0, cm));
            float bl = Math.max(
                chunk.getSmoothBlockLight(x, baseY, z, 0, 0, 0, 0, 0, 0, cm),
                chunk.getSmoothBlockLight(x, topY, z, 0, 0, 0, 0, 0, 0, cm));

            float slLo, slHi, blLo, blHi;
            if (half == Half.BOTTOM) {
                slLo = sl * 0.72f; slHi = sl * 0.90f;
                blLo = bl * 0.72f; blHi = bl * 0.90f;
            } else {
                slLo = sl * 0.90f; slHi = sl;
                blLo = bl * 0.90f; blHi = bl;
            }

            chunk.addFace(x0, y, z0, 1.0f, x1, y, z1, 1.0f, x1, y + 1, z1, 1.0f, x0, y + 1, z0, 1.0f,
                    reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slLo, slLo, slHi, slHi, blLo, blLo, blHi, blHi);
            chunk.addFace(x1, y, z1, 1.0f, x0, y, z0, 1.0f, x0, y + 1, z0, 1.0f, x1, y + 1, z1, 1.0f,
                    reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slLo, slLo, slHi, slHi, blLo, blLo, blHi, blHi);

            chunk.addFace(x1, y, z0, 1.0f, x0, y, z1, 1.0f, x0, y + 1, z1, 1.0f, x1, y + 1, z0, 1.0f,
                    reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slLo, slLo, slHi, slHi, blLo, blLo, blHi, blHi);
            chunk.addFace(x0, y, z1, 1.0f, x1, y, z0, 1.0f, x1, y + 1, z0, 1.0f, x0, y + 1, z1, 1.0f,
                    reg.u0, reg.v0, reg.u1, reg.v1, reg.layer, light, this, slLo, slLo, slHi, slHi, blLo, blLo, blHi, blHi);
        };
    }

    @Override
    public List<AABB> getHighlightBoxes(BlockState state) {
        Half half = state.getValue(DoorBlock.HALF);
        float minY = (half == Half.BOTTOM) ? 0.0f : -1.0f;
        float maxY = (half == Half.BOTTOM) ? 2.0f : 1.0f;
        float e = 0.002f;
        return List.of(new AABB(
            new Vector3f(2f / 16f - e, minY - e, 2f / 16f - e),
            new Vector3f(14f / 16f + e, maxY + e, 14f / 16f + e)
        ));
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return true;
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(DoorBlock.HALF);
    }

    protected boolean canSurviveOn(Block blockBelow) {
        if (blockBelow == null || blockBelow.isAir()) return false;
        NamespacedKey key = Registries.BLOCKS.getKey(blockBelow);
        if (key != null) {
            String name = key.getKey();
            if (name.equals("grass_block") || name.equals("dirt") || name.equals("moss") || name.equals("coarse_dirt") || name.equals("sandy_grass")) {
                return true;
            }
        }
        return blockBelow.isSolid && !blockBelow.isPassable;
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        Vector3i placePos = hitPos;
        BlockState targetState = world.getBlockState(hitPos.x, hitPos.y, hitPos.z);
        if (!targetState.getBlock().canBeReplaced(targetState, null, hitFace, exactHit)) {
            placePos = new Vector3i(hitPos.x, hitPos.y + 1, hitPos.z);
        }

        BlockState aboveState = world.getBlockState(placePos.x, placePos.y + 1, placePos.z);
        if (aboveState == null || !aboveState.getBlock().canBeReplaced(aboveState, null, hitFace, exactHit)) {
            return null;
        }

        Block blockBelow = world.getBlock(placePos.x, placePos.y - 1, placePos.z);
        if (!canSurviveOn(blockBelow)) {
            return null;
        }

        return getDefaultState().with(DoorBlock.HALF, Half.BOTTOM);
    }

    @Override
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, Player player) {
        if (state.getValue(DoorBlock.HALF) == Half.BOTTOM) {
            PairedBlocks.placePair(world, pos, state);
        }
    }

    @Override
    public void onBlockRemoved(World world, Vector3i pos, BlockState state) {
        PairedBlocks.breakPair(world, pos, state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block neighborBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, neighborBlock);
        PairedBlocks.validateOrDrop(world, x, y, z, world.getBlockState(x, y, z));
    }

    @Override
    public Vector3i getPartnerPos(Vector3i pos, BlockState state) {
        if (state.getValue(DoorBlock.HALF) == Half.BOTTOM) {
            return new Vector3i(pos.x, pos.y + 1, pos.z);
        } else {
            return new Vector3i(pos.x, pos.y - 1, pos.z);
        }
    }

    @Override
    public Vector3i getPrimaryPos(Vector3i pos, BlockState state) {
        if (state.getValue(DoorBlock.HALF) == Half.TOP) {
            return new Vector3i(pos.x, pos.y - 1, pos.z);
        }
        return new Vector3i(pos);
    }

    @Override
    public boolean isValidPartner(BlockState self, BlockState other) {
        if (self == null || other == null) return false;
        if (self.getBlock() != other.getBlock()) return false;
        return self.getValue(DoorBlock.HALF) != other.getValue(DoorBlock.HALF);
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        return List.of(new AABB(new Vector3f(2f / 16f, 0, 2f / 16f), new Vector3f(14f / 16f, 1, 14f / 16f)));
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        return List.of();
    }
}
