package de.delautrer.game.world.persistence;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import java.util.*;

public final class WorldPalette {
    public static final NamespacedKey AIR = NamespacedKey.fromString("veinstride:air");

    private final List<NamespacedKey> keys = new ArrayList<>();
    private final Map<NamespacedKey, Integer> keyToIndex = new HashMap<>();
    private final Set<NamespacedKey> missingLogged = new HashSet<>();

    public WorldPalette() {
        addKey(AIR);
    }

    private void addKey(NamespacedKey key) {
        if (!keyToIndex.containsKey(key)) {
            keyToIndex.put(key, keys.size());
            keys.add(key);
        }
    }

    public static WorldPalette createFreshFromRegistry() {
        WorldPalette palette = new WorldPalette();
        List<NamespacedKey> allKeys = new ArrayList<>(Registries.BLOCKS.keys());
        allKeys.sort(Comparator.comparing(NamespacedKey::toString));
        for (NamespacedKey key : allKeys) {
            if (!key.equals(AIR)) {
                palette.addKey(key);
            }
        }
        return palette;
    }

    public static WorldPalette fromKeyList(List<String> rawKeys) {
        WorldPalette palette = new WorldPalette();
        if (rawKeys != null) {
            for (String k : rawKeys) {
                NamespacedKey key = NamespacedKey.fromString(k);
                if (!key.equals(AIR)) {
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
            System.err.println("[WorldPalette] Unknown key: " + key + " mapped to AIR (index 0)");
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
            return AIR;
        }
        return keys.get(index);
    }

    public Block getBlock(int index) {
        NamespacedKey key = getKey(index);
        Block block = Registries.BLOCKS.get(key);
        if (block == null) {
            block = Registries.BLOCKS.get(AIR);
        }
        return block;
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