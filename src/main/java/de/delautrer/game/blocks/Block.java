package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public abstract class Block {
    private byte internalId; // Wird von der Registry zugewiesen!

    protected int lightEmission = 0;

    public final boolean isSolid;
    public final boolean isTransparent;

    public Block(boolean isSolid, boolean isTransparent) {
        this.isSolid = isSolid;
        this.isTransparent = isTransparent;
    }

    public byte getId() { return internalId; }
    public void setId(byte id) { this.internalId = id; }

    public Block setLightEmission(int level) {
        this.lightEmission = Math.max(0, Math.min(15, level));
        return this;
    }
    public int getLightEmission() {
        return lightEmission;
    }

    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock.getId() == 0) return true; // Gegen Luft
        if (this.isTransparent && this.getId() == neighborBlock.getId()) return false;
        return neighborBlock.isTransparent;
    }

    public abstract void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm);
}