package de.delautrer.game.items;

public enum ToolTier {
    HAND(0),
    WOOD(1),
    STONE(2),
    COPPER(3),
    IRON(4),
    GOLD(5),
    DIAMOND(6);

    private final int level;

    ToolTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
