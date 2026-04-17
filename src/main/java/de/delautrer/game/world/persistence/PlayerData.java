package de.delautrer.game.world.persistence;

public class PlayerData {
    public float x, y, z;
    public float yaw, pitch;

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