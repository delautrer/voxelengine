package de.delautrer.game.testing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;

import java.io.Reader;
import java.util.*;

public class GameTestRegistry {
    private static final Gson GSON = new Gson();
    private static final Map<NamespacedKey, GameTest> TESTS = new LinkedHashMap<>();

    public static void init() {
        TESTS.clear();
        if (!de.delautrer.Constants.IS_DEV) {
            System.out.println("GameTest framework disabled (production mode).");
            return;
        }

        List<String> files = ResourceUtils.listResources("assets/data/veinstride/tests", ".json");
        for (String file : files) {
            String idFromFilename = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/tests/" + file);
                JsonObject jsonObj = JsonParser.parseReader(reader).getAsJsonObject();
                if (jsonObj == null) {
                    throw new IllegalStateException("GameTest JSON file is empty: " + file);
                }

                String idStr = jsonObj.has("id") ? jsonObj.get("id").getAsString() : "veinstride:" + idFromFilename;
                if (!idStr.contains(":")) {
                    idStr = "veinstride:" + idStr;
                }
                NamespacedKey testKey = NamespacedKey.fromString(idStr);

                if (TESTS.containsKey(testKey)) {
                    throw new IllegalStateException("Duplicate GameTest ID registered: " + testKey);
                }

                int timeoutTicks = jsonObj.has("timeout_ticks") ? jsonObj.get("timeout_ticks").getAsInt() : 40;

                List<String> tags = new ArrayList<>();
                if (jsonObj.has("tags") && jsonObj.get("tags").isJsonArray()) {
                    jsonObj.getAsJsonArray("tags").forEach(elem -> tags.add(elem.getAsString()));
                }

                String origin = jsonObj.has("origin") ? jsonObj.get("origin").getAsString() : "player";

                List<GameTestStep> steps = new ArrayList<>();
                if (jsonObj.has("steps") && jsonObj.get("steps").isJsonArray()) {
                    jsonObj.getAsJsonArray("steps").forEach(elem -> {
                        JsonObject sObj = elem.getAsJsonObject();
                        GameTestStep step = GSON.fromJson(sObj, GameTestStep.class);
                        if (sObj.has("type") && (step.be_type == null || step.be_type.isEmpty())) {
                            // Support "type": "chest" inside assert_be step if "type" property was used for be_type
                            String typeVal = sObj.get("type").getAsString();
                            if (!typeVal.equals("assert_be") && !typeVal.equals("set_block") && !typeVal.equals("fill") &&
                                !typeVal.equals("tick") && !typeVal.equals("place_template") && !typeVal.equals("set_be_tag") &&
                                !typeVal.equals("assert_block") && !typeVal.equals("assert_air") && !typeVal.equals("assert_loot")) {
                                step.be_type = typeVal;
                                step.type = "assert_be";
                            }
                        }
                        validateStepFailFast(file, testKey, step);
                        steps.add(step);
                    });
                }

                GameTest test = new GameTest(testKey, timeoutTicks, tags, origin, steps);
                TESTS.put(testKey, test);
            } catch (Exception e) {
                System.err.println("[GameTestRegistry] Failed to load test: " + file);
                throw new IllegalStateException("Failed to load GameTest: " + file, e);
            }
        }

        System.out.println("Loaded " + TESTS.size() + " GameTest(s).");
    }

    private static void validateStepFailFast(String file, NamespacedKey testKey, GameTestStep step) {
        if (step == null || step.type == null || step.type.trim().isEmpty()) {
            throw new IllegalStateException("GameTest " + testKey + " (" + file + ") step missing required 'type' property!");
        }

        String type = step.type.toLowerCase();
        switch (type) {
            case "set_block":
            case "fill": {
                if (step.block == null || step.block.trim().isEmpty()) {
                    throw new IllegalStateException("GameTest " + testKey + " step '" + type + "' missing required 'block' property!");
                }
                NamespacedKey bKey = step.block.contains(":") ? NamespacedKey.fromString(step.block) : NamespacedKey.fromString("veinstride:" + step.block);
                Block b = Registries.BLOCKS.get(bKey);
                if (b == null) {
                    throw new IllegalStateException("GameTest " + testKey + " step '" + type + "' references unknown block: " + step.block);
                }
                break;
            }
            case "place_template": {
                if (step.template == null || step.template.trim().isEmpty()) {
                    throw new IllegalStateException("GameTest " + testKey + " step 'place_template' missing required 'template' property!");
                }
                NamespacedKey tKey = step.template.contains(":") ? NamespacedKey.fromString(step.template) : NamespacedKey.fromString("veinstride:" + step.template);
                StructureTemplate template = StructureRegistry.getTemplate(tKey);
                if (template == null) {
                    throw new IllegalStateException("GameTest " + testKey + " step 'place_template' references unknown structure template: " + step.template);
                }
                break;
            }
            case "place_block": {
                if (step.block == null || step.block.trim().isEmpty()) {
                    throw new IllegalStateException("GameTest " + testKey + " step 'place_block' missing required 'block' property!");
                }
                NamespacedKey bKey = step.block.contains(":") ? NamespacedKey.fromString(step.block) : NamespacedKey.fromString("veinstride:" + step.block);
                Block b = Registries.BLOCKS.get(bKey);
                if (b == null) {
                    throw new IllegalStateException("GameTest " + testKey + " step 'place_block' references unknown block: " + step.block);
                }
                break;
            }
            case "tick":
            case "set_be_tag":
            case "assert_block":
            case "assert_air":
            case "assert_be":
            case "assert_loot":
            case "assert_state":
            case "interact":
            case "set_slot":
            case "assert_item":
            case "assert_empty_inv":
            case "assert_entity":
            case "assert_no_entity":
            case "call_scheduled":
            case "assert_sapling_staged_or_grown":
            case "sapling_grows_or_stages":
                break;
            default:
                throw new IllegalStateException("GameTest " + testKey + " has unknown step type: '" + step.type + "'");
        }
    }

    public static Collection<GameTest> getTests() {
        return Collections.unmodifiableCollection(TESTS.values());
    }

    public static GameTest getTest(NamespacedKey key) {
        return TESTS.get(key);
    }
}
