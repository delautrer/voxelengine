package de.delautrer.game.world.generation.feature.placement;

import java.util.Random;

public class TrapezoidDistribution implements DistributionModel {
    private final int minY;
    private final int maxY;
    private final int peakY;

    public TrapezoidDistribution(int minY, int maxY, int peakY) {
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.peakY = Math.max(this.minY, Math.min(peakY, this.maxY));
    }

    @Override
    public int getRandomY(Random rand) {
        // Simple rejection sampling based on probability
        if (maxY == minY) return minY;
        
        while (true) {
            int y = minY + rand.nextInt((maxY - minY) + 1);
            float prob = getProbabilityAtY(y);
            if (rand.nextFloat() <= prob) {
                return y;
            }
        }
    }

    @Override
    public float getProbabilityAtY(int y) {
        if (y < minY || y > maxY) return 0.0f;
        
        if (y == peakY) return 1.0f;
        
        if (y < peakY) {
            int range = peakY - minY;
            if (range == 0) return 1.0f;
            return (float) (y - minY) / range;
        } else {
            int range = maxY - peakY;
            if (range == 0) return 1.0f;
            return 1.0f - ((float) (y - peakY) / range);
        }
    }
}
