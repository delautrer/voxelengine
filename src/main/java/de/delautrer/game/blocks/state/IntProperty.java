package de.delautrer.game.blocks.state;

import java.util.ArrayList;
import java.util.List;

public class IntProperty extends Property<Integer> {
    private final List<Integer> values = new ArrayList<>();

    public static IntProperty create(String name, int min, int max) {
        return new IntProperty(name, min, max);
    }

    private IntProperty(String name, int min, int max) {
        super(name, Integer.class);
        for (int i = min; i <= max; i++) {
            values.add(i);
        }
    }

    @Override
    public List<Integer> getAllowedValues() {
        return values;
    }
}