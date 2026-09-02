package de.delautrer.game.world.generation.feature;

import de.delautrer.game.registry.Registries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FeatureRegistryTest {

    public static void main(String[] args) throws Exception {
        FeatureRegistryTest test = new FeatureRegistryTest();
        test.testPlacedFeaturesLoading();
        System.out.println("FeatureRegistryTest: 8 placed features loaded successfully!");
    }

    @Test
    public void testPlacedFeaturesLoading() {
        Registries.init();
        FeatureRegistry.init();
        Assertions.assertFalse(FeatureRegistry.getPlacedFeaturesCount() == 0, "Placed features list should not be empty!");
        Assertions.assertEquals(8, FeatureRegistry.getPlacedFeaturesCount(), "Should load exactly 8 placed features!");
    }
}
