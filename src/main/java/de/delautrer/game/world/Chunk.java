package de.delautrer.game.world;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.engine.graphics.ChunkMesher;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.persistence.WorldPalette;

public class Chunk {
    public static final int SIZE = 16;
    public static final int MIN_Y = -128;
    public static final int MAX_Y = 384;
    public static final int HEIGHT = MAX_Y - MIN_Y; // 512
    public static final int NUM_SECTIONS = HEIGHT / ChunkSection.SIZE; // 32

    private final Biome[] biomeMap = new Biome[SIZE * SIZE];
    private final ChunkSection[] sections = new ChunkSection[NUM_SECTIONS];

    private final int worldX, worldZ;
    private WorldPalette palette;
    private boolean isDirty = false;
    private boolean needsMeshUpdate = false;
    private long lastAccessedTime;
    private final java.util.Map<org.joml.Vector3i, de.delautrer.game.nbt.CompoundTag> blockEntityTags = new java.util.concurrent.ConcurrentHashMap<>();

    public void setBlockEntityTag(int lx, int y, int lz, de.delautrer.game.nbt.CompoundTag tag) {
        if (tag != null) {
            int localX = Math.floorMod(lx, SIZE);
            int localZ = Math.floorMod(lz, SIZE);
            int wx = this.worldX * SIZE + localX;
            int wz = this.worldZ * SIZE + localZ;
            blockEntityTags.put(new org.joml.Vector3i(wx, y, wz), tag);
        }
    }

    public de.delautrer.game.nbt.CompoundTag getBlockEntityTag(org.joml.Vector3i pos) {
        return blockEntityTags.get(pos);
    }

    public java.util.Map<org.joml.Vector3i, de.delautrer.game.nbt.CompoundTag> getBlockEntityTags() {
        return blockEntityTags;
    }

    private static final float[] highlightVertices = { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 };
    private static final int[] highlightIndices = { 0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };

    public Chunk(int worldX, int worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.lastAccessedTime = System.currentTimeMillis();
    }

    public void setPalette(WorldPalette palette) {
        this.palette = palette;
    }

    public WorldPalette getPalette() {
        return palette;
    }

    public ChunkSection[] getSections() {
        return sections;
    }

    private ChunkSection getOrCreateSection(int y) {
        if (y < MIN_Y || y >= MAX_Y) return null;
        int idx = (y - MIN_Y) >> 4;
        if (sections[idx] == null) sections[idx] = new ChunkSection();
        return sections[idx];
    }

    private ChunkSection getSection(int y) {
        if (y < MIN_Y || y >= MAX_Y) return null;
        return sections[(y - MIN_Y) >> 4];
    }

    public Biome[] getBiomeMap() {
        return biomeMap;
    }

    private final java.util.Set<org.joml.Vector3i> structureVoidPositions = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public java.util.Set<org.joml.Vector3i> getStructureVoidPositions() {
        return structureVoidPositions;
    }

    public void rebuildStructureVoidIndex() {
        structureVoidPositions.clear();
        for (int sIdx = 0; sIdx < sections.length; sIdx++) {
            ChunkSection sec = sections[sIdx];
            if (sec == null || sec.isAir()) continue;
            int startY = MIN_Y + (sIdx << 4);
            for (int ly = 0; ly < 16; ly++) {
                int y = startY + ly;
                for (int lz = 0; lz < SIZE; lz++) {
                    for (int lx = 0; lx < SIZE; lx++) {
                        Block b = getBlock(lx, y, lz);
                        if (b instanceof de.delautrer.game.blocks.StructureVoidBlock) {
                            structureVoidPositions.add(new org.joml.Vector3i(worldX * SIZE + lx, y, worldZ * SIZE + lz));
                        }
                    }
                }
            }
        }
    }

    public void setBlock(int x, int y, int z, int paletteIndex, byte state) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return;
        Block oldBlock = getBlock(x, y, z);
        ChunkSection sec = getOrCreateSection(y);
        if (sec != null) {
            sec.setBlock(x, (y - MIN_Y) & 15, z, paletteIndex, state);
            this.isDirty = true;
            this.needsMeshUpdate = true;

            Block newBlock = getBlock(x, y, z);
            boolean oldIsVoid = oldBlock instanceof de.delautrer.game.blocks.StructureVoidBlock;
            boolean newIsVoid = newBlock instanceof de.delautrer.game.blocks.StructureVoidBlock;
            if (oldIsVoid && !newIsVoid) {
                structureVoidPositions.remove(new org.joml.Vector3i(worldX * SIZE + x, y, worldZ * SIZE + z));
            } else if (newIsVoid && !oldIsVoid) {
                structureVoidPositions.add(new org.joml.Vector3i(worldX * SIZE + x, y, worldZ * SIZE + z));
            }
        }
    }

    public void setBlock(int x, int y, int z, int paletteIndex) {
        setBlock(x, y, z, paletteIndex, (byte) 0);
    }

    public void setBlock(int x, int y, int z, Block block, byte state, WorldPalette palette) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE || block == null) return;
        WorldPalette usePalette = (palette != null) ? palette : this.palette;
        if (usePalette == null) return;
        short pIdx = usePalette.getOrAppend(de.delautrer.game.registry.Registries.BLOCKS.getKey(block));
        setBlock(x, y, z, pIdx, state);
    }

    public void setBlock(int x, int y, int z, Block block) {
        setBlock(x, y, z, block, (byte) 0, this.palette);
    }

    public Block getBlock(int x, int y, int z, WorldPalette palette) {
        if (y < MIN_Y || y >= MAX_Y) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            ChunkSection sec = getSection(y);
            if (sec == null) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
            int pIdx = sec.getBlockIndex(x, (y - MIN_Y) & 15, z);
            WorldPalette usePalette = (palette != null) ? palette : this.palette;
            if (usePalette != null) {
                return usePalette.getBlock(pIdx);
            }
            return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        }
        return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
    }

    public Block getBlock(int x, int y, int z) {
        return getBlock(x, y, z, this.palette);
    }

    public int getTopBlockY(int x, int z) {
        for (int y = MAX_Y - 1; y >= MIN_Y; y--) {
            Block b = getBlock(x, y, z);
            if (b != null && !b.isAir()) {
                return y;
            }
        }
        return MIN_Y;
    }

    public Block getBlock(int x, int y, int z, ChunkManager cm) {
        if (y < MIN_Y || y >= MAX_Y) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        WorldPalette palette = (cm != null && cm.getWorld() != null) ? cm.getWorld().getBlockPalette() : null;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            return getBlock(x, y, z, palette);
        }
        if (cm == null) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        return neighbor.getBlock((worldX * SIZE + x) & 15, y, (worldZ * SIZE + z) & 15, palette);
    }

    public int getBlockPaletteIndex(int x, int y, int z) {
        if (y < MIN_Y || y >= MAX_Y || x < 0 || x >= SIZE || z < 0 || z >= SIZE) return 0;
        ChunkSection sec = getSection(y);
        return sec == null ? 0 : sec.getBlockIndex(x, (y - MIN_Y) & 15, z);
    }

    public byte getStateAt(int x, int y, int z, ChunkManager cm) {
        if (y < MIN_Y || y >= MAX_Y) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            ChunkSection sec = getSection(y);
            return sec == null ? 0 : sec.getState(x, (y - MIN_Y) & 15, z);
        }
        if (cm == null) return 0;
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return 0;
        return neighbor.getState((worldX * SIZE + x) & 15, y, (worldZ * SIZE + z) & 15);
    }

    public byte getState(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return 0;
        ChunkSection sec = getSection(y);
        return sec == null ? 0 : sec.getState(x, (y - MIN_Y) & 15, z);
    }

    public BlockState getBlockState(int x, int y, int z) {
        return getBlockState(x, y, z, (WorldPalette) null);
    }

    public BlockState getBlockState(int x, int y, int z, WorldPalette palette) {
        Block block = getBlock(x, y, z, palette);
        byte stateId = getState(x, y, z);
        return block.getStateForId(stateId);
    }

    public void setBlockLight(int x, int y, int z, int val) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return;
        ChunkSection sec = getOrCreateSection(y);
        if (sec != null) sec.setBlockLight(x, (y - MIN_Y) & 15, z, val);
    }

    public void setSkyLight(int x, int y, int z, int val) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return;
        ChunkSection sec = getOrCreateSection(y);
        if (sec != null) sec.setSkyLight(x, (y - MIN_Y) & 15, z, val);
    }

    public int getBlockLight(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return 0;
        ChunkSection sec = getSection(y);
        return sec == null ? 0 : sec.getBlockLight(x, (y - MIN_Y) & 15, z);
    }

    public int getSkyLight(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < MIN_Y || y >= MAX_Y || z < 0 || z >= SIZE) return 15;
        ChunkSection sec = getSection(y);
        return sec == null ? 15 : sec.getSkyLight(x, (y - MIN_Y) & 15, z);
    }

    public int getSkyLightAt(int x, int y, int z, ChunkManager cm) {
        if (y >= MAX_Y) return 15;
        if (y < MIN_Y) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return getSkyLight(x, y, z);
        if (cm != null) {
            Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
            if (neighbor != null) return neighbor.getSkyLight((worldX * SIZE + x) & 15, y, (worldZ * SIZE + z) & 15);
        }
        int clampX = Math.max(0, Math.min(SIZE - 1, x));
        int clampZ = Math.max(0, Math.min(SIZE - 1, z));
        return getSkyLight(clampX, y, clampZ);
    }

    public int getBlockLightAt(int x, int y, int z, ChunkManager cm) {
        if (y < MIN_Y || y >= MAX_Y) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return getBlockLight(x, y, z);
        if (cm != null) {
            Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
            if (neighbor != null) return neighbor.getBlockLight((worldX * SIZE + x) & 15, y, (worldZ * SIZE + z) & 15);
        }
        int clampX = Math.max(0, Math.min(SIZE - 1, x));
        int clampZ = Math.max(0, Math.min(SIZE - 1, z));
        return getBlockLight(clampX, y, clampZ);
    }

    public float getSmoothSkyLight(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        float center = lightToBrightness(getSkyLightAt(x, y, z, cm));
        float side1 = lightToBrightness(getSkyLightAt(x + dx1, y + dy1, z + dz1, cm));
        float side2 = lightToBrightness(getSkyLightAt(x + dx2, y + dy2, z + dz2, cm));
        float corner = lightToBrightness(getSkyLightAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm));
        return (center + side1 + side2 + corner) / 4.0f;
    }

    public float getSmoothBlockLight(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        float center = lightToBrightness(getBlockLightAt(x, y, z, cm));
        float side1 = lightToBrightness(getBlockLightAt(x + dx1, y + dy1, z + dz1, cm));
        float side2 = lightToBrightness(getBlockLightAt(x + dx2, y + dy2, z + dz2, cm));
        float corner = lightToBrightness(getBlockLightAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm));
        return (center + side1 + side2 + corner) / 4.0f;
    }

    public float getAO(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        int o1 = getOpacityAt(x + dx1, y + dy1, z + dz1, cm);
        int o2 = getOpacityAt(x + dx2, y + dy2, z + dz2, cm);
        int oC = getOpacityAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm);

        boolean side1 = o1 >= 15;
        boolean side2 = o2 >= 15;
        boolean corner = oC >= 15;

        if (side1 && side2) return 0.5f;
        int count = (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
        return switch (count) {
            case 0 -> 1.0f;
            case 1 -> 0.8f;
            case 2 -> 0.65f;
            default -> 0.5f;
        };
    }

    private int getOpacityAt(int x, int y, int z, ChunkManager cm) {
        if (y < MIN_Y || y >= MAX_Y) return 0;
        WorldPalette palette = (cm != null && cm.getWorld() != null) ? cm.getWorld().getBlockPalette() : null;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            return getBlock(x, y, z, palette).getOpacity(getBlockState(x, y, z, palette));
        }
        if (cm != null) {
            Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
            if (neighbor != null) {
                int lx = (worldX * SIZE + x) & 15;
                int lz = (worldZ * SIZE + z) & 15;
                return neighbor.getBlock(lx, y, lz, palette).getOpacity(neighbor.getBlockState(lx, y, lz, palette));
            }
        }
        return 0;
    }

    private float lightToBrightness(float lightLevel) {
        if (lightLevel <= 0) return 0.0f;
        return (float) Math.pow(0.8f, 15.0f - lightLevel);
    }

    public void recalculateSunlightColumn(int x, int z, LightEngine lightEngine) {
        int currentLight = 15;
        for (int y = MAX_Y - 1; y >= MIN_Y; y--) {
            BlockState state = getBlockState(x, y, z);
            int oldLight = getSkyLight(x, y, z);
            int opacity = state.getBlock().getOpacity(state);

            if (opacity > 0) {
                // Opaque or semi-transparent block: attenuate
                currentLight = Math.max(0, currentLight - Math.max(1, opacity));
            }
            // opacity == 0 (air/transparent): no loss, currentLight stays the same

            if (currentLight != oldLight) {
                setSkyLight(x, y, z, currentLight);
                if (lightEngine != null) {
                    int globalX = this.worldX * SIZE + x;
                    int globalZ = this.worldZ * SIZE + z;
                    if (currentLight < oldLight) {
                        lightEngine.addSkyLightRemoval(globalX, y, globalZ, oldLight);
                    } else {
                        lightEngine.addSkyLightUpdate(globalX, y, globalZ);
                    }
                }
            }
        }
    }

    public void calculateSunlight() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                recalculateSunlightColumn(x, z, null);
            }
        }
    }

    public void setBiome(int x, int z, Biome biome) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) return;
        this.biomeMap[(x << 4) | z] = biome;
    }

    public Biome getBiome(int x, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) return null;
        return this.biomeMap[(x << 4) | z];
    }

    public void requestMeshUpdate() { this.needsMeshUpdate = true; }
    public ChunkMesher.ChunkMeshResult generateMeshData(ChunkManager cm) { return ChunkMesher.generateMeshData(this, cm); }

    public void addFace(float x0, float y0, float z0, float ao0, float x1, float y1, float z1, float ao1, float x2, float y2, float z2, float ao2, float x3, float y3, float z3, float ao3, float u0, float v0, float u1, float v1, float texLayer, float directionalLight, Block block, float sl0, float sl1, float sl2, float sl3, float bl0, float bl1, float bl2, float bl3) {
        addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3, u0, v1, u1, v1, u1, v0, u0, v0, texLayer, directionalLight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3, 1.0f, 1.0f, 1.0f);
    }

    public void addFace(float x0, float y0, float z0, float ao0, float x1, float y1, float z1, float ao1, float x2, float y2, float z2, float ao2, float x3, float y3, float z3, float ao3, float u0, float v0, float u1, float v1, float texLayer, float directionalLight, Block block, float sl0, float sl1, float sl2, float sl3, float bl0, float bl1, float bl2, float bl3, float tintR, float tintG, float tintB) {
        addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3, u0, v1, u1, v1, u1, v0, u0, v0, texLayer, directionalLight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3, tintR, tintG, tintB);
    }

    public void addFace(float x0, float y0, float z0, float ao0, float x1, float y1, float z1, float ao1, float x2, float y2, float z2, float ao2, float x3, float y3, float z3, float ao3, float uv0_u, float uv0_v, float uv1_u, float uv1_v, float uv2_u, float uv2_v, float uv3_u, float uv3_v, float texLayer, float directionalLight, Block block, float sl0, float sl1, float sl2, float sl3, float bl0, float bl1, float bl2, float bl3) {
        ChunkMesher.addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3, uv0_u, uv0_v, uv1_u, uv1_v, uv2_u, uv2_v, uv3_u, uv3_v, texLayer, directionalLight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3, 1.0f, 1.0f, 1.0f);
    }

    public void addFace(float x0, float y0, float z0, float ao0, float x1, float y1, float z1, float ao1, float x2, float y2, float z2, float ao2, float x3, float y3, float z3, float ao3, float uv0_u, float uv0_v, float uv1_u, float uv1_v, float uv2_u, float uv2_v, float uv3_u, float uv3_v, float texLayer, float directionalLight, Block block, float sl0, float sl1, float sl2, float sl3, float bl0, float bl1, float bl2, float bl3, float tintR, float tintG, float tintB) {
        ChunkMesher.addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3, uv0_u, uv0_v, uv1_u, uv1_v, uv2_u, uv2_v, uv3_u, uv3_v, texLayer, directionalLight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3, tintR, tintG, tintB);
    }

    public void clearMeshCache() { this.needsMeshUpdate = true; }
    public static float[] getHighlightVertices() { return highlightVertices; }
    public static int[] getHighlightIndices() { return highlightIndices; }
    public int getWorldX() { return worldX; }
    public int getWorldZ() { return worldZ; }
    public void markDirty() { this.isDirty = true; this.needsMeshUpdate = true; }
    public boolean isDirty() { return isDirty; }
    public void clearDirty() { this.isDirty = false; }
    public boolean needsMeshUpdate() { return needsMeshUpdate; }
    public void clearMeshUpdate() { this.needsMeshUpdate = false; }
    public void access() { this.lastAccessedTime = System.currentTimeMillis(); }
    public long getLastAccessedTime() { return lastAccessedTime; }
}
