package de.delautrer.game.blocks.state;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BooleanProperty extends Property<Boolean> {
    private static final List<Boolean> VALUES = Arrays.asList(false, true);

    protected BooleanProperty(String name) {
        super(name, Boolean.class);
    }

    public static BooleanProperty create(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public Collection<Boolean> getAllowedValues() {
        return VALUES;
    }
}