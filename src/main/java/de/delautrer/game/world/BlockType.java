package de.delautrer.game.world;

public enum BlockType {
    AIR((byte)0, false, 0, 0, 0),
    GRASS((byte)1, true, 0, 1, 2), // Top, Side, Bottom Layer
    DIRT((byte)2, true, 2, 2, 2),
    STONE((byte)3, true, 3, 3, 3),
    WATER((byte)4, false, 4, 4, 4);

    public final byte id;
    public final boolean isSolid;
    public final int texTop, texSide, texBottom;

    BlockType(byte id, boolean isSolid, int top, int side, int bottom) {
        this.id = id;
        this.isSolid = isSolid;
        this.texTop = top;
        this.texSide = side;
        this.texBottom = bottom;
    }

    public static BlockType getById(byte id) {
        for (BlockType b : values()) if (b.id == id) return b;
        return AIR;
    }
}