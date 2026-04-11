package de.delautrer.game.world;

public enum BlockType {
    // ID, isSolid, isTransparent, texTop, texSide, texBottom
    AIR((byte)0, false, true, 0, 0, 0),
    GRASS((byte)1, true, false, 0, 1, 2), // Top: 0, Side: 1, Bottom: 2
    DIRT((byte)2, true, false, 2, 2, 2),
    STONE((byte)3, true, false, 3, 3, 3),
    WATER((byte)4, false, true, 4, 4, 4),
    GLASS((byte)5, true, true, 5, 5, 5),   // Glas
    LEAVES((byte)6, true, true, 6, 6, 6);  // Blätter

    public final byte id;
    public final boolean isSolid;
    public final boolean isTransparent; // Steuert, ob man durch den Block schauen kann
    public final int texTop, texSide, texBottom;

    BlockType(byte id, boolean isSolid, boolean isTransparent, int top, int side, int bottom) {
        this.id = id;
        this.isSolid = isSolid;
        this.isTransparent = isTransparent;
        this.texTop = top;
        this.texSide = side;
        this.texBottom = bottom;
    }

    public static BlockType getById(byte id) {
        for (BlockType b : values()) if (b.id == id) return b;
        return AIR;
    }
}