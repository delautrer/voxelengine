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
    public Map<Property<?>, Comparable<?>> getProperties() { return properties; }
    public boolean contains(Property<?> property) {
        return properties.containsKey(property);
    }

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

    @SuppressWarnings("unchecked")
    public BlockState rotateY(int quarters) {
        quarters = (quarters % 4 + 4) % 4;
        if (quarters == 0) return this;

        BlockState newState = this;
        for (Map.Entry<Property<?>, Comparable<?>> entry : properties.entrySet()) {
            Property<?> prop = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof BlockProperties.Direction dir) {
                BlockProperties.Direction newDir = dir;
                for (int i = 0; i < quarters; i++) {
                    newDir = newDir.rotateYClockwise();
                }
                if (newDir != dir) {
                    newState = newState.with((Property<BlockProperties.Direction>) prop, newDir);
                }
            } else if (value instanceof de.delautrer.game.blocks.TorchBlock.TorchAttach attach) {
                de.delautrer.game.blocks.TorchBlock.TorchAttach newAttach = attach;
                for (int i = 0; i < quarters; i++) {
                    newAttach = switch (newAttach) {
                        case NORTH -> de.delautrer.game.blocks.TorchBlock.TorchAttach.EAST;
                        case EAST -> de.delautrer.game.blocks.TorchBlock.TorchAttach.SOUTH;
                        case SOUTH -> de.delautrer.game.blocks.TorchBlock.TorchAttach.WEST;
                        case WEST -> de.delautrer.game.blocks.TorchBlock.TorchAttach.NORTH;
                        case FLOOR -> de.delautrer.game.blocks.TorchBlock.TorchAttach.FLOOR;
                    };
                }
                if (newAttach != attach) {
                    newState = newState.with((Property<de.delautrer.game.blocks.TorchBlock.TorchAttach>) prop, newAttach);
                }
            } else if (value instanceof BlockProperties.Axis axis) {
                if (quarters % 2 != 0) {
                    BlockProperties.Axis newAxis = switch (axis) {
                        case X -> BlockProperties.Axis.Z;
                        case Z -> BlockProperties.Axis.X;
                        case Y -> BlockProperties.Axis.Y;
                    };
                    if (newAxis != axis) {
                        newState = newState.with((Property<BlockProperties.Axis>) prop, newAxis);
                    }
                }
            }
        }
        return newState;
    }
}