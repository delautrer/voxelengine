package de.delautrer.game.entity.player;

public enum GameMode {
    SURVIVAL(0),
    CREATIVE(1),
    SPECTATOR(2);

    private final int id;

    GameMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static GameMode fromId(int id) {
        for (GameMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return SURVIVAL;
    }
}