package de.delautrer.game.blocks.state;

import java.util.Arrays;
import java.util.List;

public class EnumProperty<E extends Enum<E>> extends Property<E> {
    private final List<E> allowedValues;

    public EnumProperty(String name, Class<E> clazz) {
        super(name, clazz);
        this.allowedValues = Arrays.asList(clazz.getEnumConstants());
    }

    public static <E extends Enum<E>> EnumProperty<E> create(String name, Class<E> clazz) {
        return new EnumProperty<>(name, clazz);
    }

    @Override
    public List<E> getAllowedValues() {
        return allowedValues;
    }
}
