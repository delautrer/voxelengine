package de.delautrer.game.loot;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LootTableManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, LootTable> CACHE = new HashMap<>();

    public static LootTable load(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        // Pfad normalisieren
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String fullPath = "/assets/loot_tables/" + cleanPath;

        // Nutze den ClassLoader direkt, das ist oft stabiler in EXEn
        try (InputStream is = LootTableManager.class.getResourceAsStream(fullPath)) {

            if (is == null) {
                // In der IDE ist das ein Fehler, in der EXE wissen wir jetzt bescheid
                System.err.println("[LootTableManager] File NOT found: " + fullPath);
                CACHE.put(path, null);
                return null;
            }

            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                LootTable table = GSON.fromJson(reader, LootTable.class);

                if (table == null || table.pools == null) {
                    System.err.println("[LootTableManager] Failed to parse JSON (empty table): " + fullPath);
                } else {
                }

                CACHE.put(path, table);
                return table;
            }

        } catch (Exception e) {
            System.err.println("[LootTableManager] Critical error loading: " + fullPath);
            e.printStackTrace();
            CACHE.put(path, null);
            return null;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}