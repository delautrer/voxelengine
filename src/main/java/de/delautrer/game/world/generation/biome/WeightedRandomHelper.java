package de.delautrer.game.world.generation.biome;

import java.util.Map;
import java.util.Random;

public class WeightedRandomHelper {
    public static String getRandom(Map<String, Integer> weights, Random random) {
        if (weights == null || weights.isEmpty()) return null;

        int totalWeight = 0;
        for (int weight : weights.values()) {
            totalWeight += weight;
        }

        if (totalWeight == 0) return null;

        int value = random.nextInt(totalWeight);
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            value -= entry.getValue();
            if (value < 0) return entry.getKey();
        }
        return null;
    }
}