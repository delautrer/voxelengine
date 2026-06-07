package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.models.BlockModelData;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.*;
import de.delautrer.game.items.ToolTier;

public abstract class Block {
    private byte internalId;
    private BlockModelData model;

    protected int lightEmission = 0;

    protected float hardness = 0.42f;

    public final boolean isSolid;
    public final boolean isTransparent;
    public final boolean isPassable;
    public final boolean isRaycastable;

    protected String lootTable = null;
    protected String category = "misc";
    protected ToolTier minToolTier = ToolTier.HAND;

    private BlockState defaultState;
    private BlockState[] stateArray;

    private String soundMaterialName;

    public Block(boolean isSolid, boolean isTransparent) {
        this(isSolid, isTransparent, false, true);
    }

    public Block(boolean isSolid, boolean isTransparent, boolean isPassable) {
        this(isSolid, isTransparent, isPassable, true);
    }
    @SuppressWarnings("this-escape")
    public Block(boolean isSolid, boolean isTransparent, boolean isPassable,  boolean isRaycastable) {
        this.isSolid = isSolid;
        this.isTransparent = isTransparent;
        this.isPassable = isPassable;
        this.isRaycastable = isRaycastable;

        generateBlockStates();
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

    public int getLightEmission(BlockState state) {
        return getLightEmission();
    }

    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        if (neighborBlock.getId() == 0) return true;
        if (this.isTransparent && this.getId() == neighborBlock.getId()) return false;
        return neighborBlock.isTransparent;
    }

    public abstract void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm);

    /**
     * Wird aufgerufen, wenn sich direkt neben diesem Block ein anderer Block ändert.
     * (z.B. Spieler baut etwas ab, Wasser fließt daneben, etc.)
     */
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {}

    /**
     * Wird aufgerufen, wenn der Block in der Welt platziert wurde.
     */
    public void onBlockPlaced(World world, Vector3i pos, BlockState state, Player player) {}

    /**
     * Wird aufgerufen, bevor der Block aus der Welt entfernt wird.
     */
    public void onBlockRemoved(World world, Vector3i pos, BlockState state) {}

    /**
     * Wird vom TickScheduler aufgerufen, wenn die geplante Zeit abgelaufen ist.
     */
    public void scheduledTick(World world, int x, int y, int z) {}

    /**
     * Blöcke können diese Methode überschreiben, um ihre Properties anzumelden.
     * (z.B. Wasser fügt hier das LEVEL hinzu)
     */
    protected void appendProperties(List<Property<?>> properties) {
        // Standardmäßig hat ein normaler Block (wie Erde/Stein) keine Properties
    }

    private void generateBlockStates() {
        List<Property<?>> properties = new ArrayList<>();
        appendProperties(properties);

        // 1. Alle möglichen Kombinationen berechnen
        List<Map<Property<?>, Comparable<?>>> combinations = new ArrayList<>();
        combinations.add(new HashMap<>()); // Start mit einer leeren Map

        for (Property<?> prop : properties) {
            List<Map<Property<?>, Comparable<?>>> newCombinations = new ArrayList<>();
            for (Map<Property<?>, Comparable<?>> comp : combinations) {
                for (Comparable<?> value : prop.getAllowedValues()) {
                    Map<Property<?>, Comparable<?>> newComp = new HashMap<>(comp);
                    newComp.put(prop, value);
                    newCombinations.add(newComp);
                }
            }
            combinations = newCombinations;
        }

        // 2. BlockState Objekte erstellen und ID zuweisen
        stateArray = new BlockState[combinations.size()];
        Map<Map<Property<?>, Comparable<?>>, BlockState> registry = new HashMap<>();

        for (int i = 0; i < combinations.size(); i++) {
            byte stateId = (byte) i; // Dies ist das rohe Byte, das später im Chunk-Array landet!
            BlockState state = new BlockState(this, stateId, combinations.get(i));
            stateArray[i] = state;
            registry.put(combinations.get(i), state);
        }

        // 3. Registry verlinken und Default-State setzen
        for (BlockState state : stateArray) {
            state.linkRegistry(registry);
        }

        // Der Standardzustand ist immer der erste (z.B. Wasser Level 0, Tür zu, Treppe Nord)
        this.defaultState = stateArray.length > 0 ? stateArray[0] : new BlockState(this, (byte)0, Collections.emptyMap());
    }

    public BlockState getDefaultState() {
        return defaultState;
    }
    public BlockState getStateForId(byte id) {
        if (id >= 0 && id < stateArray.length) return stateArray[id];
        return defaultState;
    }
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit){
        return getDefaultState();
    }
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return false;
    }
    public List<AABB> getBoundingBoxes(BlockState state) {
        return List.of(new AABB(new Vector3f(0,0,0), new Vector3f(1,1,1)));
    }

    /**
     * Gibt die Bounding-Boxen für den Highlighter zurück (Standard: gleiche wie BoundingBoxes)
     */
    public List<AABB> getHighlightBoxes(BlockState state) {
        return getBoundingBoxes(state);
    }

    /**
     * Gibt die Bounding-Boxen für die Kollision zurück (Standard: gleiche wie BoundingBoxes)
     */
    public List<AABB> getCollisionBoxes(BlockState state) {
        return getBoundingBoxes(state);
    }

    // Highlighter generiert sich jetzt AUTOMATISCH aus den Highlight-Boxes!
    public float[] getHighlightVertices(BlockState state) {
        java.util.List<AABB> boxes = getHighlightBoxes(state);
        float[] verts = new float[boxes.size() * 24];
        int idx = 0;
        for (AABB b : boxes) {
            float[] boxVerts = {
                    b.min.x, b.min.y, b.min.z,  b.max.x, b.min.y, b.min.z,  b.max.x, b.max.y, b.min.z,  b.min.x, b.max.y, b.min.z,
                    b.min.x, b.min.y, b.max.z,  b.max.x, b.min.y, b.max.z,  b.max.x, b.max.y, b.max.z,  b.min.x, b.max.y, b.max.z
            };
            System.arraycopy(boxVerts, 0, verts, idx, 24);
            idx += 24;
        }
        return verts;
    }
    public int[] getHighlightIndices(BlockState state) {
        java.util.List<AABB> boxes = getHighlightBoxes(state);
        int[] inds = new int[boxes.size() * 24];
        int idx = 0;
        for (int i = 0; i < boxes.size(); i++) {
            int offset = i * 8;
            int[] boxInds = {
                    offset, offset+1, offset+1, offset+2, offset+2, offset+3, offset+3, offset,
                    offset+4, offset+5, offset+5, offset+6, offset+6, offset+7, offset+7, offset+4,
                    offset, offset+4, offset+1, offset+5, offset+2, offset+6, offset+3, offset+7
            };
            System.arraycopy(boxInds, 0, inds, idx, 24);
            idx += 24;
        }
        return inds;
    }
    public boolean canWaterFlowInto() {
        return false;
    }
    public void setModel(BlockModelData model) {
        this.model = model;
    }
    public BlockModelData getModel() {
        return model;
    }
    public boolean hasBlockEntity() { return false; }
    public BlockEntity createBlockEntity(World world, Vector3i pos) { return null; }
    public float getHardness() {
        return hardness;
    }
    public Block setHardness(float hardness) {
        this.hardness = hardness;
        return this;
    }
    public Block setLootTable(String path) {
        this.lootTable = path;
        return this;
    }
    public String getLootTable() { return lootTable; }

    public Block setSoundMaterialName(String soundMaterialName) {
        if (this.soundMaterialName != null) return this;
        this.soundMaterialName = soundMaterialName;
        return this;
    }

    public String getSoundMaterialName() {
        return soundMaterialName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setMinToolTier(ToolTier minToolTier) {
        this.minToolTier = minToolTier;
    }

    public ToolTier getMinToolTier() {
        return minToolTier;
    }

    protected int opacityValue = -1;

    public void setOpacity(int opacity) {
        this.opacityValue = opacity;
    }

    public int getOpacity(BlockState state) {
        if (opacityValue >= 0) return opacityValue;
        return isTransparent ? 0 : 15;
    }
}


