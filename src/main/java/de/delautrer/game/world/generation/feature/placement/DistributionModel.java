package de.delautrer.game.world.generation.feature.placement;

import java.util.Random;

public interface DistributionModel {
    int getRandomY(Random rand);
    float getProbabilityAtY(int y);
}
