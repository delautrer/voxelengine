package de.delautrer.game.world.generation.structure;

import de.delautrer.game.registry.NamespacedKey;

import java.util.List;
import java.util.Random;

public class StructureSet {

    public static class WeightedStructure {
        public final Structure structure;
        public final int weight;

        public WeightedStructure(Structure structure, int weight) {
            this.structure = structure;
            this.weight = Math.max(1, weight);
        }
    }

    private final NamespacedKey key;
    private final List<WeightedStructure> structures;
    private final String placementType;
    private final int spacing;
    private final int separation;
    private final long salt;

    public StructureSet(NamespacedKey key, List<WeightedStructure> structures, String placementType, int spacing, int separation, long salt) {
        this.key = key;
        this.structures = structures;
        this.placementType = (placementType != null) ? placementType : "random_spread";
        this.spacing = Math.max(1, spacing);
        this.separation = Math.max(0, Math.min(this.spacing - 1, separation));
        this.salt = salt;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public List<WeightedStructure> getStructures() {
        return structures;
    }

    public int getSpacing() {
        return spacing;
    }

    public int getSeparation() {
        return separation;
    }

    public long getSalt() {
        return salt;
    }

    public boolean isOwnerChunk(int chunkX, int chunkZ, long worldSeed) {
        int gridX = Math.floorDiv(chunkX, spacing);
        int gridZ = Math.floorDiv(chunkZ, spacing);

        long gridSeed = worldSeed ^ ((long) gridX * 341873128712L ^ (long) gridZ * 132897987541L ^ salt);
        Random rand = new Random(gridSeed);

        int maxOffset = Math.max(1, spacing - separation);
        int offsetX = rand.nextInt(maxOffset);
        int offsetZ = rand.nextInt(maxOffset);

        int ownerChunkX = gridX * spacing + offsetX;
        int ownerChunkZ = gridZ * spacing + offsetZ;

        return chunkX == ownerChunkX && chunkZ == ownerChunkZ;
    }

    public Structure selectStructure(long seed, int chunkX, int chunkZ) {
        if (structures == null || structures.isEmpty()) return null;
        if (structures.size() == 1) return structures.get(0).structure;

        int totalWeight = 0;
        for (WeightedStructure ws : structures) totalWeight += ws.weight;

        long gridSeed = seed ^ ((long) chunkX * 7312345L ^ (long) chunkZ * 91612345L ^ salt);
        Random rand = new Random(gridSeed);
        int roll = rand.nextInt(totalWeight);

        int current = 0;
        for (WeightedStructure ws : structures) {
            current += ws.weight;
            if (roll < current) {
                return ws.structure;
            }
        }
        return structures.get(0).structure;
    }
}
