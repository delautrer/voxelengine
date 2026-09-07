package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.BooleanProperty;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.registry.Tag;
import de.delautrer.game.registry.TagRegistry;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class FenceGateBlock extends CubeBlock implements IInteractable {

    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty IN_WALL = BooleanProperty.create("in_wall");
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    @SuppressWarnings("this-escape")
    public FenceGateBlock() {
        super(false, true, false, true); // isSolid=false, isTransparent=true, isPassable=false, isRaycastable=true
        setSoundMaterialName("wood");
        setHardness(2.0f);

        this.mesher = (state, x, y, z, chunk, cm) -> {
            boolean open = state.contains(OPEN) && state.getValue(OPEN);
            boolean inWall = state.contains(IN_WALL) && state.getValue(IN_WALL);
            Direction facing = state.contains(FACING) ? state.getValue(FACING) : Direction.NORTH;
            float yOff = inWall ? -0.1875f : 0.0f;

            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                // Gate runs along X axis
                // Side posts
                renderBox(state, x, y, z, 0.0f, 0.0f + yOff, 0.375f, 0.125f, 1.0f + yOff, 0.625f, true, true, true, true, true, true, false, chunk, cm);
                renderBox(state, x, y, z, 0.875f, 0.0f + yOff, 0.375f, 1.0f, 1.0f + yOff, 0.625f, true, true, true, true, true, true, false, chunk, cm);

                if (!open) {
                    // Closed doors
                    renderBox(state, x, y, z, 0.125f, 0.1875f + yOff, 0.4375f, 0.5f, 0.8125f + yOff, 0.5625f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.5f, 0.1875f + yOff, 0.4375f, 0.875f, 0.8125f + yOff, 0.5625f, true, true, true, true, true, true, false, chunk, cm);
                } else if (facing == Direction.NORTH) {
                    // Open doors swung North (-Z)
                    renderBox(state, x, y, z, 0.0625f, 0.1875f + yOff, 0.0625f, 0.1875f, 0.8125f + yOff, 0.4375f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.8125f, 0.1875f + yOff, 0.0625f, 0.9375f, 0.8125f + yOff, 0.4375f, true, true, true, true, true, true, false, chunk, cm);
                } else {
                    // Open doors swung South (+Z)
                    renderBox(state, x, y, z, 0.0625f, 0.1875f + yOff, 0.5625f, 0.1875f, 0.8125f + yOff, 0.9375f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.8125f, 0.1875f + yOff, 0.5625f, 0.9375f, 0.8125f + yOff, 0.9375f, true, true, true, true, true, true, false, chunk, cm);
                }
            } else {
                // Gate runs along Z axis
                // Side posts
                renderBox(state, x, y, z, 0.375f, 0.0f + yOff, 0.0f, 0.625f, 1.0f + yOff, 0.125f, true, true, true, true, true, true, false, chunk, cm);
                renderBox(state, x, y, z, 0.375f, 0.0f + yOff, 0.875f, 0.625f, 1.0f + yOff, 1.0f, true, true, true, true, true, true, false, chunk, cm);

                if (!open) {
                    // Closed doors
                    renderBox(state, x, y, z, 0.4375f, 0.1875f + yOff, 0.125f, 0.5625f, 0.8125f + yOff, 0.5f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.4375f, 0.1875f + yOff, 0.5f, 0.5625f, 0.8125f + yOff, 0.875f, true, true, true, true, true, true, false, chunk, cm);
                } else if (facing == Direction.WEST) {
                    // Open doors swung West (-X)
                    renderBox(state, x, y, z, 0.0625f, 0.1875f + yOff, 0.0625f, 0.4375f, 0.8125f + yOff, 0.1875f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.0625f, 0.1875f + yOff, 0.8125f, 0.4375f, 0.8125f + yOff, 0.9375f, true, true, true, true, true, true, false, chunk, cm);
                } else {
                    // Open doors swung East (+X)
                    renderBox(state, x, y, z, 0.5625f, 0.1875f + yOff, 0.0625f, 0.9375f, 0.8125f + yOff, 0.1875f, true, true, true, true, true, true, false, chunk, cm);
                    renderBox(state, x, y, z, 0.5625f, 0.1875f + yOff, 0.8125f, 0.9375f, 0.8125f + yOff, 0.9375f, true, true, true, true, true, true, false, chunk, cm);
                }
            }
        };
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        super.appendProperties(properties);
        properties.add(OPEN);
        properties.add(IN_WALL);
        properties.add(FACING);
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        Block nBlock = neighborState.getBlock();
        if (nBlock == null || nBlock.isAir()) return true;

        if (nBlock instanceof FenceGateBlock) {
            boolean myInWall = myState.contains(IN_WALL) && myState.getValue(IN_WALL);
            boolean nInWall = neighborState.contains(IN_WALL) && neighborState.getValue(IN_WALL);
            if (myInWall != nInWall) {
                return true; // Adjacent gates have different heights -> render connecting face!
            }
        }
        return nBlock.isTransparent;
    }

    public static boolean checkIfInWall(World world, int x, int y, int z, Direction facing) {
        Tag<Block> wallsTag = TagRegistry.getBlockTag("veinstride:walls");
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            Block wBlock = world.getBlock(x - 1, y, z);
            Block eBlock = world.getBlock(x + 1, y, z);
            boolean wIsWall = (wBlock instanceof WallBlock) || (wallsTag != null && wallsTag.contains(wBlock));
            boolean eIsWall = (eBlock instanceof WallBlock) || (wallsTag != null && wallsTag.contains(eBlock));
            return wIsWall || eIsWall;
        } else {
            Block nBlock = world.getBlock(x, y, z - 1);
            Block sBlock = world.getBlock(x, y, z + 1);
            boolean nIsWall = (nBlock instanceof WallBlock) || (wallsTag != null && wallsTag.contains(nBlock));
            boolean sIsWall = (sBlock instanceof WallBlock) || (wallsTag != null && wallsTag.contains(sBlock));
            return nIsWall || sIsWall;
        }
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        Direction facing;
        if (player != null && player instanceof LocalPlayer lp) {
            float yaw = lp.getCamera().getYaw();
            yaw = (yaw % 360 + 360) % 360;
            if (yaw >= 45 && yaw < 135) facing = Direction.SOUTH;
            else if (yaw >= 135 && yaw < 225) facing = Direction.WEST;
            else if (yaw >= 225 && yaw < 315) facing = Direction.NORTH;
            else facing = Direction.EAST;
        } else {
            facing = Direction.NORTH;
        }

        boolean inWall = checkIfInWall(world, hitPos.x, hitPos.y, hitPos.z, facing);

        return getDefaultState()
                .with(OPEN, false)
                .with(FACING, facing)
                .with(IN_WALL, inWall);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        if (state == null || state.getBlock() != this) return false;

        boolean open = !state.getValue(OPEN);
        Direction facing = state.getValue(FACING);

        if (open && player != null) {
            float yaw = player.getCamera().getYaw();
            yaw = (yaw % 360 + 360) % 360;
            Direction playerFacing;
            if (yaw >= 45 && yaw < 135) playerFacing = Direction.SOUTH;
            else if (yaw >= 135 && yaw < 225) playerFacing = Direction.WEST;
            else if (yaw >= 225 && yaw < 315) playerFacing = Direction.NORTH;
            else playerFacing = Direction.EAST;

            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                if (playerFacing == Direction.NORTH || playerFacing == Direction.SOUTH) {
                    facing = playerFacing;
                }
            } else {
                if (playerFacing == Direction.EAST || playerFacing == Direction.WEST) {
                    facing = playerFacing;
                }
            }
        }

        world.setBlockWithState(pos.x, pos.y, pos.z, this, state.with(OPEN, open).with(FACING, facing).getStateId(), true);
        return true;
    }

    @Override
    public List<AABB> getCollisionBoxes(BlockState state) {
        boolean open = state.getValue(OPEN);
        boolean inWall = state.getValue(IN_WALL);
        float yOff = inWall ? -0.1875f : 0.0f;
        Direction facing = state.getValue(FACING);

        if (open) {
            // Open gate -> middle passage is clear, returns side post collision boxes
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                return List.of(
                        new AABB(new Vector3f(0.0f, 0.0f + yOff, 0.375f), new Vector3f(0.125f, 1.5f + yOff, 0.625f)),
                        new AABB(new Vector3f(0.875f, 0.0f + yOff, 0.375f), new Vector3f(1.0f, 1.5f + yOff, 0.625f))
                );
            } else {
                return List.of(
                        new AABB(new Vector3f(0.375f, 0.0f + yOff, 0.0f), new Vector3f(0.625f, 1.5f + yOff, 0.125f)),
                        new AABB(new Vector3f(0.375f, 0.0f + yOff, 0.875f), new Vector3f(0.625f, 1.5f + yOff, 1.0f))
                );
            }
        }

        // Closed gate -> 1.5 height collision box prevents jumping over
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            return List.of(new AABB(new Vector3f(0.0f, 0.0f + yOff, 0.375f), new Vector3f(1.0f, 1.5f + yOff, 0.625f)));
        } else {
            return List.of(new AABB(new Vector3f(0.375f, 0.0f + yOff, 0.0f), new Vector3f(0.625f, 1.5f + yOff, 1.0f)));
        }
    }

    @Override
    public List<AABB> getHighlightBoxes(BlockState state) {
        boolean open = state.getValue(OPEN);
        boolean inWall = state.getValue(IN_WALL);
        float yMin = inWall ? -0.1875f : 0.0f;
        float yMax = inWall ? 0.8125f : 1.0f;
        Direction facing = state.getValue(FACING);

        if (!open) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                return List.of(new AABB(new Vector3f(0.0f, yMin, 0.375f), new Vector3f(1.0f, yMax, 0.625f)));
            } else {
                return List.of(new AABB(new Vector3f(0.375f, yMin, 0.0f), new Vector3f(0.625f, yMax, 1.0f)));
            }
        } else {
            if (facing == Direction.NORTH) {
                return List.of(
                        new AABB(new Vector3f(0.0f, yMin, 0.0625f), new Vector3f(0.1875f, yMax, 0.625f)),
                        new AABB(new Vector3f(0.8125f, yMin, 0.0625f), new Vector3f(1.0f, yMax, 0.625f))
                );
            } else if (facing == Direction.SOUTH) {
                return List.of(
                        new AABB(new Vector3f(0.0f, yMin, 0.375f), new Vector3f(0.1875f, yMax, 0.9375f)),
                        new AABB(new Vector3f(0.8125f, yMin, 0.375f), new Vector3f(1.0f, yMax, 0.9375f))
                );
            } else if (facing == Direction.WEST) {
                return List.of(
                        new AABB(new Vector3f(0.0625f, yMin, 0.0f), new Vector3f(0.625f, yMax, 0.1875f)),
                        new AABB(new Vector3f(0.0625f, yMin, 0.8125f), new Vector3f(0.625f, yMax, 1.0f))
                );
            } else {
                return List.of(
                        new AABB(new Vector3f(0.375f, yMin, 0.0f), new Vector3f(0.9375f, yMax, 0.1875f)),
                        new AABB(new Vector3f(0.375f, yMin, 0.8125f), new Vector3f(0.9375f, yMax, 1.0f))
                );
            }
        }
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        return getHighlightBoxes(state);
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, Block changedBlock) {
        super.onNeighborChanged(world, x, y, z, neighborPos, changedBlock);
        BlockState state = world.getBlockState(x, y, z);
        if (state != null && state.getBlock() == this) {
            Direction facing = state.getValue(FACING);
            boolean inWall = checkIfInWall(world, x, y, z, facing);
            if (inWall != state.getValue(IN_WALL)) {
                world.setBlockWithState(x, y, z, this, state.with(IN_WALL, inWall).getStateId(), true);
            }
        }
    }
}
