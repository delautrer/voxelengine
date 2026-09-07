package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.BooleanProperty;
import de.delautrer.game.registry.Tag;
import de.delautrer.game.registry.TagRegistry;
import de.delautrer.game.world.World;

public class ConnectedState {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");

    public static boolean connectsToward(World world, int x, int y, int z, Direction dir, String tagName, boolean connectSolid) {
        int dx = 0, dy = 0, dz = 0;
        switch (dir) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case EAST -> dx = 1;
            case WEST -> dx = -1;
            case UP -> dy = 1;
            case DOWN -> dy = -1;
        }

        int nx = x + dx;
        int ny = y + dy;
        int nz = z + dz;

        Block neighborBlock = world.getBlock(nx, ny, nz);
        if (neighborBlock == null || neighborBlock.isAir()) return false;

        Block currentBlock = world.getBlock(x, y, z);
        if (currentBlock != null && neighborBlock == currentBlock) {
            return true;
        }

        if (connectSolid && neighborBlock.isSolid && !neighborBlock.isTransparent) {
            return true;
        }

        if (tagName != null) {
            Tag<Block> tag = TagRegistry.getBlockTag(tagName);
            if (tag != null && tag.contains(neighborBlock)) {
                return true;
            }
        }

        return false;
    }

    public static BlockState updateConnections(World world, int x, int y, int z, BlockState state, String tagName, boolean connectSolid) {
        if (state == null) return state;

        boolean n = connectsToward(world, x, y, z, Direction.NORTH, tagName, connectSolid);
        boolean e = connectsToward(world, x, y, z, Direction.EAST, tagName, connectSolid);
        boolean s = connectsToward(world, x, y, z, Direction.SOUTH, tagName, connectSolid);
        boolean w = connectsToward(world, x, y, z, Direction.WEST, tagName, connectSolid);

        BlockState newState = state;
        if (state.contains(NORTH)) newState = newState.with(NORTH, n);
        if (state.contains(EAST)) newState = newState.with(EAST, e);
        if (state.contains(SOUTH)) newState = newState.with(SOUTH, s);
        if (state.contains(WEST)) newState = newState.with(WEST, w);

        return newState;
    }
}
