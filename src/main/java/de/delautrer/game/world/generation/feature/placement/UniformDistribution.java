package de.delautrer.game.world.generation.feature.placement;

import java.util.Random;

public class UniformDistribution implements DistributionModel {
    private final int minY;
    private final int maxY;

    public UniformDistribution(int minY, int maxY) {
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    @Override
    public int getRandomY(Random rand) {
        if (maxY == minY) return minY;
        return minY + rand.nextInt((maxY - minY) + 1);
    }

    @Override
    public float getProbabilityAtY(int y) {
        if (y >= minY && y <= maxY) {
            return 1.0f;
        }
        return 0.0f;
    }
}
