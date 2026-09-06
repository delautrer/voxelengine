package de.delautrer.game.worldgen.pool;

import com.google.gson.Gson;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.generation.structure.StructureRegistry;

import java.io.Reader;
import java.util.*;

public class TemplatePoolRegistry {

    private static final Map<NamespacedKey, TemplatePool> POOLS = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        isInitialized = true;

        POOLS.clear();
        loadPoolsFromAssets();
        validatePools();

        System.out.println("Loaded " + POOLS.size() + " template pools.");
    }

    private static void loadPoolsFromAssets() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/template_pool", ".json");
        for (String file : files) {
            String path = file.substring(0, file.length() - 5).replace('\\', '/');
            NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/template_pool/" + file);
                TemplatePoolDTO dto = GSON.fromJson(reader, TemplatePoolDTO.class);
                if (dto != null) {
                    NamespacedKey poolKey = dto.name != null && !dto.name.trim().isEmpty() 
                            ? NamespacedKey.fromString(dto.name) : key;
                    NamespacedKey fallbackKey = dto.fallback != null && !dto.fallback.trim().isEmpty()
                            ? NamespacedKey.fromString(dto.fallback) : new NamespacedKey(Constants.NAMESPACE, "empty");

                    List<TemplatePool.PoolElement> poolElements = new ArrayList<>();
                    if (dto.elements != null) {
                        for (TemplatePoolDTO.ElementEntryDTO entry : dto.elements) {
                            if (entry != null && entry.element != null && entry.element.template != null) {
                                String tStr = entry.element.template;
                                NamespacedKey tKey = tStr.contains(":") ? NamespacedKey.fromString(tStr) : new NamespacedKey(Constants.NAMESPACE, tStr);
                                String elType = entry.element.element_type != null ? entry.element.element_type : "single_pool_element";
                                String proj = entry.element.projection != null ? entry.element.projection : "rigid";
                                poolElements.add(new TemplatePool.PoolElement(entry.weight, elType, tKey, proj));
                            }
                        }
                    }
                    TemplatePool pool = new TemplatePool(poolKey, fallbackKey, poolElements);
                    POOLS.put(poolKey, pool);
                }
            } catch (Exception e) {
                System.err.println("[TemplatePoolRegistry] Error loading template pool " + file + ": " + e.getMessage());
            }
        }
    }

    private static void validatePools() {
        for (TemplatePool pool : POOLS.values()) {
            for (TemplatePool.PoolElement el : pool.getElements()) {
                if (StructureRegistry.getTemplate(el.getTemplateKey()) == null) {
                    throw new IllegalStateException("TemplatePool '" + pool.getKey() + "' references unknown structure template '" + el.getTemplateKey() + "'!");
                }
            }
        }
    }

    public static TemplatePool getPool(NamespacedKey key) {
        if (key == null) return null;
        return POOLS.get(key);
    }

    public static TemplatePool pickWeighted(NamespacedKey key, Random random) {
        TemplatePool pool = getPool(key);
        return pool;
    }

    public static Set<NamespacedKey> getAllKeys() {
        return Collections.unmodifiableSet(POOLS.keySet());
    }

    public static Map<NamespacedKey, TemplatePool> getPools() {
        return Collections.unmodifiableMap(POOLS);
    }
}
