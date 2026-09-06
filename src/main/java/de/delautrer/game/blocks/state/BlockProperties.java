package de.delautrer.game.blocks.state;

public class BlockProperties {
    public enum Axis { X, Y, Z }
    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN;

        public Direction getOpposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
                case UP -> DOWN;
                case DOWN -> UP;
            };
        }
    }
    public enum Half { BOTTOM, TOP }
    public enum SlabType { BOTTOM, TOP, DOUBLE }
    public enum StairShape { STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT }
    public enum BlockFace { UP, DOWN, NORTH, SOUTH, EAST, WEST }
    public enum DoorHinge { LEFT, RIGHT }
}