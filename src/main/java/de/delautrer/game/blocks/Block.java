package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public abstract class Block {
    private byte internalId;

    protected int lightEmission = 0;

    public final boolean isSolid;
    public final boolean isTransparent;
    public final boolean isPassable;
    public final boolean isRaycastable;

    public Block(boolean isSolid, boolean isTransparent) {
        this(isSolid, isTransparent, false, true);
    }

    public Block(boolean isSolid, boolean isTransparent, boolean isPassable) {
        this(isSolid, isTransparent, isPassable, true);
    }
    public Block(boolean isSolid, boolean isTransparent, boolean isPassable,  boolean isRaycastable) {
        this.isSolid = isSolid;
        this.isTransparent = isTransparent;
        this.isPassable = isPassable;
        this.isRaycastable = isRaycastable;
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
        if (neighborBlock.getId() == 0) return true;
        if (this.isTransparent && this.getId() == neighborBlock.getId()) return false;
        return neighborBlock.isTransparent;
    }

    public float[] getHighlightVertices() {
        return new float[]{ 0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1 };
    }
    public int[] getHighlightIndices() {
        return new int[]{ 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };
    }

    public abstract void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm);

    /**
     * Wird aufgerufen, wenn sich direkt neben diesem Block ein anderer Block ändert.
     * (z.B. Spieler baut etwas ab, Wasser fließt daneben, etc.)
     */
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {}

    /**
     * Wird vom TickScheduler aufgerufen, wenn die geplante Zeit abgelaufen ist.
     */
    public void scheduledTick(World world, int x, int y, int z) {}
}