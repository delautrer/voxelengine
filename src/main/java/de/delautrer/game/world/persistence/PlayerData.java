package de.delautrer.game.world.persistence;

import de.delautrer.game.entity.player.GameMode;

public class PlayerData {
    public float x, y, z;
    public float yaw, pitch;

    public float currentHealth;
    public boolean isDead;

    public GameMode gamemode;
    public int selectedHotbarSlot;
    public SavedSlot[] inventory;

    public static class SavedSlot {
        public String id;
        public int amount;

        public SavedSlot(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }
    }
}