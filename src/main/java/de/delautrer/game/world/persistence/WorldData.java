package de.delautrer.game.world.persistence;

import org.joml.Vector3f;

public class WorldData {
    public String worldName;
    public long seed;
    public float timeOfDay;
    public boolean allowCheats;
    public Vector3f worldSpawnpoint;
    public String weather;
    
    // Metadata
    public long creationDate;
    public long lastOpenedDate;
    public long lastSavedDate;
    public String creationVersion;
    public String lastOpenedVersion;

    // World Generation Configuration
    public String generatorType = "DEFAULT";
    public String generatorOptions = "";

    // Save format & migration
    public int saveFormatVersion = 1;
    public java.util.List<String> blockPalette;
    public java.util.List<String> biomePalette;
    public java.util.Map<String, String> namespaceAliases;
    public long currentTick = 0;
}