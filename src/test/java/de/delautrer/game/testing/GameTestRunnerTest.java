package de.delautrer.game.testing;

import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.World;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GameTestRunnerTest {

    @BeforeAll
    public static void setup() {
        Registries.init();
    }

    @Test
    public void testBlockPlacementGameTest() {
        World world = WorldFixture.create();
        GameTest test = GameTestRegistry.getTest(NamespacedKey.fromString("veinstride:block_placement_test"));
        Assertions.assertNotNull(test, "block_placement_test must be registered!");

        GameTestResult result = GameTestRunner.run(world, null, test);
        Assertions.assertTrue(result.isPassed(), "block_placement_test failed: " + result.getMessage());
    }

    @Test
    public void testChestLootBeGameTest() {
        World world = WorldFixture.create();
        GameTest test = GameTestRegistry.getTest(NamespacedKey.fromString("veinstride:chest_loot_be_test"));
        Assertions.assertNotNull(test, "chest_loot_be_test must be registered!");

        GameTestResult result = GameTestRunner.run(world, null, test);
        Assertions.assertTrue(result.isPassed(), "chest_loot_be_test failed: " + result.getMessage());
    }

    @Test
    public void testSandFallsGameTest() {
        World world = WorldFixture.create();
        GameTest test = GameTestRegistry.getTest(NamespacedKey.fromString("veinstride:sand_falls_onto_stone"));
        Assertions.assertNotNull(test, "sand_falls_onto_stone must be registered!");

        GameTestResult result = GameTestRunner.run(world, null, test);
        Assertions.assertTrue(result.isPassed(), "sand_falls_onto_stone failed: " + result.getMessage());
    }
}
