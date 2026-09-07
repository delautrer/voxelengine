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

    @Test
    public void testDoorGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("door") || test.getId().getKey().startsWith("door_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 6, "Expected at least 6 door tests to run, but ran: " + ranCount);
    }

    @Test
    public void testCarpetGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("carpet") || test.getId().getKey().startsWith("carpet_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 3, "Expected at least 3 carpet tests to run, but ran: " + ranCount);
    }

    @Test
    public void testLayerGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("layer") || test.getId().getKey().startsWith("layer_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 3, "Expected at least 3 layer tests to run, but ran: " + ranCount);
    }

    @Test
    public void testPaneGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("pane") || test.getId().getKey().startsWith("pane_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 4, "Expected at least 4 pane tests to run, but ran: " + ranCount);
    }

    @Test
    public void testFenceGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("fence") || test.getId().getKey().startsWith("fence_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 3, "Expected at least 3 fence tests to run, but ran: " + ranCount);
    }

    @Test
    public void testWallGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("wall") || test.getId().getKey().startsWith("wall_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 4, "Expected at least 4 wall tests to run, but ran: " + ranCount);
    }

    @Test
    public void testGateGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("gate") || test.hasTag("fence_gate") || test.getId().getKey().startsWith("gate_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 3, "Expected at least 3 gate tests to run, but ran: " + ranCount);
    }

    @Test
    public void testHedgeGameTests() {
        World world = WorldFixture.create();
        int ranCount = 0;
        for (GameTest test : GameTestRegistry.getTests()) {
            if (test.hasTag("hedge") || test.getId().getKey().startsWith("hedge_")) {
                ranCount++;
                GameTestResult result = GameTestRunner.run(world, null, test);
                Assertions.assertTrue(result.isPassed(), test.getId() + " failed: " + result.getMessage());
            }
        }
        Assertions.assertTrue(ranCount >= 2, "Expected at least 2 hedge tests to run, but ran: " + ranCount);
    }
}
