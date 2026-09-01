package de.delautrer.game.loot;

import com.google.gson.Gson;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class LootTableManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, LootTable> CACHE = new HashMap<>();

    public static LootTable load(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String fullPath = "assets/data/veinstride/loot_tables/" + cleanPath;
        if (!fullPath.endsWith(".json")) fullPath += ".json";

        try {
            Reader reader = ResourceUtils.readResourceToReader(fullPath);
            LootTable table = GSON.fromJson(reader, LootTable.class);

            if (table == null || table.pools == null) {
                System.err.println("[LootTableManager] Failed to parse JSON (empty table): " + fullPath);
            } else {
                CACHE.put(path, table);
            }
            return table;
        } catch (Exception e) {
            System.err.println("[LootTableManager] Error loading loot table: " + fullPath);
            if (!Constants.IS_DEV) {
                CACHE.put(path, null);
            }
            return null;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}