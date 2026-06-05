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
        public int durability;

        public SavedSlot(String id, int amount) {
            this.id = id;
            this.amount = amount;
            this.durability = -1;
        }

        public SavedSlot(String id, int amount, int durability) {
            this.id = id;
            this.amount = amount;
            this.durability = durability;
        }
    }
}