package de.delautrer.game.world.persistence;

public class PlayerData {
    // Position & Rotation
    public float x, y, z;
    public float yaw, pitch;

    // Inventar
    public int selectedHotbarSlot;
    public SavedSlot[] inventory;

    // Eine kleine Hilfsklasse für Gson, damit nur ID und Anzahl gespeichert werden
    public static class SavedSlot {
        public String id;
        public int amount;

        public SavedSlot(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }
    }
}