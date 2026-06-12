package de.delautrer.game.world.generation.feature;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.feature.config.ConfiguredFeatureDTO;
import de.delautrer.game.world.generation.feature.config.PlacedFeatureDTO;
import de.delautrer.game.world.generation.feature.placement.DistributionModel;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;
import de.delautrer.game.world.generation.feature.placement.TrapezoidDistribution;
import de.delautrer.game.world.generation.feature.placement.UniformDistribution;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FeatureRegistry {
    private static final List<PlacedFeature> FEATURES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static synchronized void init() {
        if (isInitialized) return;

        try (InputStream is = FeatureRegistry.class.getResourceAsStream("/assets/world/ores.json")) {
            if (is == null) {
                System.out.println("No ores.json found, skipping data-driven ore generation.");
                isInitialized = true;
                return;
            }

            List<PlacedFeatureDTO> dtos = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    new TypeToken<List<PlacedFeatureDTO>>() {}.getType()
            );

            if (dtos != null) {
                for (PlacedFeatureDTO dto : dtos) {
                    PlacedFeature feature = parseFeature(dto);
                    if (feature != null) {
                        FEATURES.add(feature);
                    }
                }
                System.out.println("Loaded " + FEATURES.size() + " data-driven features.");
            }
        } catch (Exception e) {
            System.err.println("Error loading ores.json:");
            e.printStackTrace();
        }

        isInitialized = true;
    }

    private static PlacedFeature parseFeature(PlacedFeatureDTO dto) {
        if (dto.feature == null || dto.placement == null) {
            System.err.println("Missing feature or placement in DTO for id: " + dto.id);
            return null;
        }

        ConfiguredFeature configured = parseConfiguredFeature(dto.feature);
        if (configured == null) {
            System.err.println("Failed to parse ConfiguredFeature for id: " + dto.id);
            return null;
        }

        DistributionModel distribution = parseDistribution(dto.placement.distribution);
        if (distribution == null) return null;

        PlacementModifier modifier = new PlacementModifier(
                dto.placement.modifiers != null ? dto.placement.modifiers.target_blocks : null,
                dto.placement.modifiers != null ? dto.placement.modifiers.air_exposure_chance : 0.0,
                dto.placement.modifiers != null ? dto.placement.modifiers.biomes : null
        );

        return new PlacedFeature(dto.id, configured, dto.placement.count, distribution, modifier);
    }

    private static ConfiguredFeature parseConfiguredFeature(ConfiguredFeatureDTO dto) {
        byte blockId = getBlockId(dto.block);
        if (blockId == 0) {
            System.err.println("Invalid block for feature: " + dto.block);
            return null; // Invalid block
        }

        if ("standard_vein".equalsIgnoreCase(dto.type)) {
            return new StandardVeinFeature(blockId, dto.size > 0 ? dto.size : 8);
        } else if ("mega_vein".equalsIgnoreCase(dto.type)) {
            byte carrierId = getBlockId(dto.carrier);
            if (carrierId == 0) carrierId = blockId; // Fallback
            return new MegaVeinFeature(blockId, carrierId, dto.ore_chance > 0 ? dto.ore_chance : 0.1);
        }
        System.err.println("Unknown feature type: " + dto.type);
        return null;
    }

    private static DistributionModel parseDistribution(de.delautrer.game.world.generation.feature.config.DistributionDTO dto) {
        if (dto == null) return new UniformDistribution(0, 128); // Fallback

        if ("trapezoid".equalsIgnoreCase(dto.type)) {
            return new TrapezoidDistribution(dto.min_y, dto.max_y, dto.peak_y);
        }
        return new UniformDistribution(dto.min_y, dto.max_y);
    }

    private static byte getBlockId(String name) {
        if (name == null) return 0;
        var block = BlockRegistry.get(name);
        if (block != null && block.getId() != 0) return block.getId();
        block = BlockRegistry.get(de.delautrer.Constants.NAMESPACE + ":" + name);
        if (block != null && block.getId() != 0) return block.getId();
        return 0;
    }

    public static void generateOres(Chunk chunk, long seed) {
        if (!isInitialized) init();
        
        if (FEATURES.isEmpty()) {
            System.err.println("Warning: generateOres called, but FEATURES list is empty!");
            return;
        }
        
        for (PlacedFeature feature : FEATURES) {
            feature.generate(chunk, seed, null);
        }
    }
}
