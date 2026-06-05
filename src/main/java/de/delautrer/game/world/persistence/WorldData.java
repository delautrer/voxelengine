package de.delautrer.game.world.persistence;

import org.joml.Vector3f;

public class WorldData {
    public String worldName;
    public long seed;
    public float timeOfDay;
    public Vector3f worldSpawnpoint;
    public String weather;
    
    // Metadata
    public long creationDate;
    public long lastOpenedDate;
    public long lastSavedDate;
    public String creationVersion;
    public String lastOpenedVersion;
}