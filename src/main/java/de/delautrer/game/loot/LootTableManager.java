package de.delautrer.game.loot;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class LootTableManager {
    private static final Gson GSON = new Gson();
    // Cache: Verhindert, dass wir dieselbe Datei 1000x laden müssen
    private static final Map<String, LootTable> CACHE = new HashMap<>();

    public static LootTable load(String path) {
        // 1. Im Cache nachschauen
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        try {
            // 2. Datei aus dem src/main/resources Verzeichnis laden
            // Der Pfad, den wir übergeben, sieht z.B. so aus: "blocks/leaves.json"
            InputStream is = LootTableManager.class.getResourceAsStream("/assets/loot_tables/" + path);

            if (is == null) {
                System.err.println("LootTable nicht gefunden: /assets/loot_tables/" + path);
                CACHE.put(path, null); // Null cachen, damit wir nicht bei jedem Abbau neu suchen
                return null;
            }

            // 3. JSON in unsere Java-Klasse parsen
            Reader reader = new InputStreamReader(is);
            LootTable table = GSON.fromJson(reader, LootTable.class);
            reader.close();

            // 4. In den Cache packen und zurückgeben
            CACHE.put(path, table);
            return table;

        } catch (Exception e) {
            System.err.println("Fehler beim Laden der LootTable: " + path);
            e.printStackTrace();
            CACHE.put(path, null);
            return null;
        }
    }

    // Optional: Falls du später mal LootTables zur Laufzeit neu laden willst (z.B. per Command)
    public static void clearCache() {
        CACHE.clear();
    }
}