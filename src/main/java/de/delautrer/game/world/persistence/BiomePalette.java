package de.delautrer.game.world.persistence;

import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.generation.biome.Biome;
import java.util.*;

public final class BiomePalette {
    public static final NamespacedKey DEFAULT_BIOME = NamespacedKey.fromString("veinstride:plains");

    private final List<NamespacedKey> keys = new ArrayList<>();
    private final Map<NamespacedKey, Integer> keyToIndex = new HashMap<>();
    private final Set<NamespacedKey> missingLogged = new HashSet<>();

    public BiomePalette() {
        addKey(DEFAULT_BIOME);
    }

    private void addKey(NamespacedKey key) {
        if (!keyToIndex.containsKey(key)) {
            keyToIndex.put(key, keys.size());
            keys.add(key);
        }
    }

    public static BiomePalette createFreshFromRegistry() {
        BiomePalette palette = new BiomePalette();
        List<NamespacedKey> allKeys = new ArrayList<>(Registries.BIOMES.keys());
        allKeys.sort(Comparator.comparing(NamespacedKey::toString));
        for (NamespacedKey key : allKeys) {
            if (!key.equals(DEFAULT_BIOME)) {
                palette.addKey(key);
            }
        }
        return palette;
    }

    public static BiomePalette fromKeyList(List<String> rawKeys) {
        BiomePalette palette = new BiomePalette();
        if (rawKeys != null) {
            for (String k : rawKeys) {
                NamespacedKey key = NamespacedKey.fromString(k);
                if (!key.equals(DEFAULT_BIOME)) {
                    palette.addKey(key);
                }
            }
        }
        return palette;
    }

    public short getIndex(NamespacedKey key) {
        if (key == null) return 0;
        Integer idx = keyToIndex.get(key);
        if (idx != null) return idx.shortValue();

        if (missingLogged.add(key)) {
            System.err.println("[BiomePalette] Unknown key: " + key + " mapped to DEFAULT_BIOME (index 0)");
        }
        return 0;
    }

    public short getOrAppend(NamespacedKey key) {
        if (key == null) return 0;
        Integer idx = keyToIndex.get(key);
        if (idx != null) return idx.shortValue();

        int newIdx = keys.size();
        keyToIndex.put(key, newIdx);
        keys.add(key);
        return (short) newIdx;
    }

    public NamespacedKey getKey(int index) {
        if (index < 0 || index >= keys.size()) {
            return DEFAULT_BIOME;
        }
        return keys.get(index);
    }

    public Biome getBiome(int index) {
        NamespacedKey key = getKey(index);
        Biome biome = Registries.BIOMES.get(key);
        if (biome == null) {
            biome = Registries.BIOMES.get(DEFAULT_BIOME);
        }
        return biome;
    }

    public int size() {
        return keys.size();
    }

    public List<String> toKeyList() {
        List<String> result = new ArrayList<>();
        for (NamespacedKey key : keys) {
            result.add(key.toString());
        }
        return result;
    }
}