package de.delautrer.game.world;

public class ChunkSection {
    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE;
    
    private final byte[] blocks;
    private final byte[] states;
    private final byte[] lightMap;
    
    private boolean isAir = true;

    public ChunkSection() {
        this.blocks = new byte[VOLUME];
        this.states = new byte[VOLUME];
        this.lightMap = new byte[VOLUME];
    }

    private int getIndex(int x, int y, int z) {
        return (x << 8) | (z << 4) | y;
    }

    public void setBlock(int x, int y, int z, byte type, byte state) {
        int idx = getIndex(x, y, z);
        blocks[idx] = type;
        states[idx] = state;
        if (type != 0) isAir = false;
    }

    public byte getBlock(int x, int y, int z) {
        if (isAir) return 0;
        return blocks[getIndex(x, y, z)];
    }

    public byte getState(int x, int y, int z) {
        if (isAir) return 0;
        return states[getIndex(x, y, z)];
    }

    public void setBlockLight(int x, int y, int z, int val) {
        int idx = getIndex(x, y, z);
        lightMap[idx] = (byte) ((lightMap[idx] & 0xF0) | (val & 0x0F));
    }

    public void setSkyLight(int x, int y, int z, int val) {
        int idx = getIndex(x, y, z);
        lightMap[idx] = (byte) ((lightMap[idx] & 0x0F) | ((val & 0x0F) << 4));
    }

    public int getBlockLight(int x, int y, int z) {
        return lightMap[getIndex(x, y, z)] & 0x0F;
    }

    public int getSkyLight(int x, int y, int z) {
        return (lightMap[getIndex(x, y, z)] >> 4) & 0x0F;
    }
    
    public void recalculateAir() {
        isAir = true;
        for (int i = 0; i < VOLUME; i++) {
            if (blocks[i] != 0) {
                isAir = false;
                break;
            }
        }
    }

    public boolean isAir() {
        return isAir;
    }
    
    public byte[] getBlocks() { return blocks; }
    public byte[] getStates() { return states; }
    public byte[] getLightMap() { return lightMap; }
}
