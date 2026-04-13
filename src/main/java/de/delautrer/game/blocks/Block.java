package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

import java.util.*;

public abstract class Block {
    private byte internalId;

    protected int lightEmission = 0;

    public final boolean isSolid;
    public final boolean isTransparent;
    public final boolean isPassable;
    public final boolean isRaycastable;

    private BlockState defaultState;
    private BlockState[] stateArray;

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

    // Wandelt das rohe Byte aus dem Chunk zurück in ein smartes Objekt
    public BlockState getStateForId(byte id) {
        if (id >= 0 && id < stateArray.length) return stateArray[id];
        return defaultState;
    }
}