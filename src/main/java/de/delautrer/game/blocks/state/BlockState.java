package de.delautrer.game.blocks.state;

import de.delautrer.game.blocks.Block;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class BlockState {
    private final Block block;
    private final byte stateId; // Das ist das byte, das im Chunk-Array landet!
    private final Map<Property<?>, Comparable<?>> properties;

    // Interne Lookup-Tabelle für extrem schnelle Zustandswechsel ohne 'new'
    private Map<Map<Property<?>, Comparable<?>>, BlockState> stateRegistry;

    public BlockState(Block block, byte stateId, Map<Property<?>, Comparable<?>> properties) {
        this.block = block;
        this.stateId = stateId;
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    // Wird von der Block-Klasse bei der Generierung aufgerufen
    public void linkRegistry(Map<Map<Property<?>, Comparable<?>>, BlockState> registry) {
        this.stateRegistry = registry;
    }

    public Block getBlock() { return block; }
    public byte getStateId() { return stateId; }

    // Holt den Wert einer Eigenschaft, z.B.: int level = state.getValue(WaterBlock.LEVEL);
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T getValue(Property<T> property) {
        if (!properties.containsKey(property)) {
            throw new IllegalArgumentException("Property " + property.getName() + " existiert in " + block.getClass().getSimpleName() + " nicht!");
        }
        return (T) properties.get(property);
    }

    // Wechselt einen Zustand und gibt das vorberechnete Objekt zurück
    // z.B.: BlockState newState = state.with(WaterBlock.LEVEL, 7);
    public <T extends Comparable<T>> BlockState with(Property<T> property, T value) {
        if (!properties.containsKey(property)) {
            throw new IllegalArgumentException("Block " + block.getClass().getSimpleName() + " hat keine Eigenschaft " + property.getName());
        }
        if (properties.get(property).equals(value)) return this; // Nichts geändert

        // Suchen der neuen Kombination in der Registry
        Map<Property<?>, Comparable<?>> searchMap = new HashMap<>(this.properties);
        searchMap.put(property, value);

        BlockState result = stateRegistry.get(searchMap);
        if (result == null) {
            throw new IllegalStateException("Unbekannte BlockState-Kombination!");
        }
        return result;
    }
}