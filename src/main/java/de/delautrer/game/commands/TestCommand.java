package de.delautrer.game.commands;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.testing.GameTest;
import de.delautrer.game.testing.GameTestRegistry;
import de.delautrer.game.testing.GameTestResult;
import de.delautrer.game.testing.GameTestRunner;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCommand implements ICommand {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getUsage() {
        return "/test list | run <id|*> | run tag:<name>";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (!de.delautrer.Constants.IS_DEV) {
            commandManager.sendMessageInChat("The /test command is only available in dev mode.");
            return;
        }

        if (args.length == 0) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String sub = args[0].toLowerCase();
        if ("list".equalsIgnoreCase(sub)) {
            handleList(commandManager);
        } else if ("run".equalsIgnoreCase(sub)) {
            if (args.length < 2) {
                commandManager.sendMessageInChat("Usage: /test run <id|*> | run tag:<name>");
                return;
            }
            handleRun(player, world, args[1], commandManager);
        } else {
            commandManager.sendMessageInChat("Usage: " + getUsage());
        }
    }

    private void handleList(CommandManager manager) {
        Collection<GameTest> tests = GameTestRegistry.getTests();
        if (tests.isEmpty()) {
            manager.sendMessageInChat("No GameTests registered.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Registered GameTests (").append(tests.size()).append("): ");
        int idx = 0;
        for (GameTest test : tests) {
            if (idx > 0) sb.append(", ");
            sb.append(test.getId());
            if (!test.getTags().isEmpty()) {
                sb.append(" ").append(test.getTags());
            }
            idx++;
        }
        manager.sendMessageInChat(sb.toString());
    }

    private void handleRun(LocalPlayer player, World world, String query, CommandManager manager) {
        Collection<GameTest> allTests = GameTestRegistry.getTests();
        List<GameTest> matchingTests = new ArrayList<>();

        if ("*".equals(query)) {
            matchingTests.addAll(allTests);
        } else if (query.toLowerCase().startsWith("tag:")) {
            String tagName = query.substring(4).trim();
            for (GameTest test : allTests) {
                if (test.hasTag(tagName)) {
                    matchingTests.add(test);
                }
            }
        } else {
            String keyStr = query.contains(":") ? query : "veinstride:" + query;
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            GameTest test = GameTestRegistry.getTest(key);
            if (test != null) {
                matchingTests.add(test);
            }
        }

        if (matchingTests.isEmpty()) {
            manager.sendMessageInChat("No matching GameTests found for query: " + query);
            return;
        }

        manager.sendMessageInChat("Running " + matchingTests.size() + " GameTest(s)...");
        int passedCount = 0;
        int failedCount = 0;

        for (GameTest test : matchingTests) {
            Vector3i origin = GameTestRunner.calculateOrigin(player, test.getOriginMode());

            // Ensure chunk coverage around test origin in live world
            ensureChunkCoverage(world, origin);

            // Pre-test cleanup: fill 16x8x16 around origin with air
            clearRegion(world, origin, 16, 8, 16);

            GameTestResult result = GameTestRunner.run(world, player, test);
            if (result.isPassed()) {
                passedCount++;
                manager.sendMessageInChat("[GameTest] PASS: " + test.getId());
                // Post-pass optional air cleanup
                clearRegion(world, origin, 16, 8, 16);
            } else {
                failedCount++;
                manager.sendMessageInChat("[GameTest] FAIL: " + test.getId() + " - " + result.getMessage());
                // Do NOT clear area on fail to preserve for debugging
            }
        }

        manager.sendMessageInChat("GameTests completed: " + passedCount + " passed, " + failedCount + " failed.");
    }

    private void ensureChunkCoverage(World world, Vector3i origin) {
        int minCX = (origin.x - 16) >> 4;
        int maxCX = (origin.x + 32) >> 4;
        int minCZ = (origin.z - 16) >> 4;
        int maxCZ = (origin.z + 32) >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (world.getChunkManager().getChunk(cx, cz) == null) {
                    Chunk c = new Chunk(cx, cz);
                    c.setPalette(world.getBlockPalette());
                    world.getChunkManager().addChunk(c);
                }
            }
        }
    }

    private void clearRegion(World world, Vector3i origin, int sizeX, int sizeY, int sizeZ) {
        Block air = Registries.BLOCKS.get("veinstride:air");
        int halfX = sizeX / 2;
        int halfZ = sizeZ / 2;

        for (int x = origin.x - halfX; x < origin.x - halfX + sizeX; x++) {
            for (int y = origin.y - 1; y < origin.y - 1 + sizeY; y++) {
                for (int z = origin.z - halfZ; z < origin.z - halfZ + sizeZ; z++) {
                    world.setBlock(x, y, z, air, (byte) 0);
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        if (!de.delautrer.Constants.IS_DEV) {
            return new ArrayList<>();
        }
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("list".startsWith(prefix)) completions.add("list");
            if ("run".startsWith(prefix)) completions.add("run");
        } else if (args.length == 2 && "run".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase();
            if ("*".startsWith(prefix)) completions.add("*");

            Set<String> tags = new HashSet<>();
            for (GameTest test : GameTestRegistry.getTests()) {
                String idStr = test.getId().getKey(); // short name without veinstride:
                if (idStr.toLowerCase().startsWith(prefix)) completions.add(idStr);
                if (test.getId().toString().toLowerCase().startsWith(prefix)) completions.add(test.getId().toString());

                for (String t : test.getTags()) {
                    tags.add("tag:" + t);
                }
            }

            for (String tagOption : tags) {
                if (tagOption.toLowerCase().startsWith(prefix)) {
                    completions.add(tagOption);
                }
            }
        }
        return completions;
    }
}
