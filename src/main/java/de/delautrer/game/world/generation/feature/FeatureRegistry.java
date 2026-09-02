package de.delautrer.game.world.generation.feature;

import com.google.gson.Gson;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.feature.config.*;
import de.delautrer.game.world.generation.feature.placement.DistributionModel;
import de.delautrer.game.world.generation.feature.placement.PlacementModifier;
import de.delautrer.game.world.generation.feature.placement.TrapezoidDistribution;
import de.delautrer.game.world.generation.feature.placement.UniformDistribution;

import de.delautrer.game.world.generation.feature.config.PlacementDTO;
import de.delautrer.game.world.generation.biome.TreeFeature;

import java.io.Reader;
import java.util.*;
import java.util.Objects;
import de.delautrer.game.world.WorldGenerator;

public class FeatureRegistry {
    private static final Map<NamespacedKey, ConfiguredFeature> CONFIGURED_FEATURES = new HashMap<>();
    private static final Map<NamespacedKey, PlacedFeature> PLACED_FEATURES = new HashMap<>();
    private static final List<PlacedFeature> FEATURES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static synchronized void init() {
        if (isInitialized) return;

        loadConfiguredFeatures();
        loadPlacedFeatures();

        System.out.println("Loaded " + CONFIGURED_FEATURES.size() + " configured features and " + FEATURES.size() + " placed features.");
        isInitialized = true;
    }

    private static void loadConfiguredFeatures() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/configured_feature", ".json");
        for (String file : files) {
            String id = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            NamespacedKey key = NamespacedKey.fromString("veinstride:" + id);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/configured_feature/" + file);
                ConfiguredFeatureDTO dto = GSON.fromJson(reader, ConfiguredFeatureDTO.class);
                ConfiguredFeature feature = parseConfiguredFeature(dto, file);
                if (feature != null) {
                    CONFIGURED_FEATURES.put(key, feature);
                }
            } catch (Exception e) {
                System.err.println("[FeatureRegistry] Failed to load configured feature: " + file);
                throw new IllegalStateException("Failed to load configured feature: " + file, e);
            }
        }
    }

    private static void loadPlacedFeatures() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/placed_feature", ".json");
        for (String file : files) {
            String id = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/placed_feature/" + file);
                PlacedFeatureDTO dto = GSON.fromJson(reader, PlacedFeatureDTO.class);
                if (dto != null) {
                    DistributionDTO distDTO = dto.getDistribution();
                    ModifiersDTO modDTO = dto.getModifiers();
                    int count = dto.getCount();

                    String featureKeyStr = (dto.feature != null) ? dto.feature : ("veinstride:" + id);
                    ConfiguredFeature configured = CONFIGURED_FEATURES.get(NamespacedKey.fromString(featureKeyStr));
                    if (configured == null) {
                        throw new IllegalStateException("Placed feature '" + id + "' in file " + file + " references unknown configured feature '" + featureKeyStr + "'");
                    }
                    DistributionModel distribution = parseDistribution(distDTO);
                    PlacementModifier modifier = new PlacementModifier(
                            modDTO != null ? modDTO.getTargetBlocksList() : null,
                            modDTO != null ? modDTO.air_exposure_chance : 0.0,
                            modDTO != null ? modDTO.getBiomesList() : null
                    );
                    PlacedFeature pf = new PlacedFeature(id, configured, count, distribution, modifier);
                    FEATURES.add(pf);
                    PLACED_FEATURES.put(NamespacedKey.fromString("veinstride:" + id), pf);
                }
            } catch (Exception e) {
                System.err.println("[FeatureRegistry] Failed to load placed feature: " + file);
                throw new IllegalStateException("Failed to load placed feature: " + file, e);
            }
        }
    }

    public static ConfiguredFeature getConfiguredFeature(NamespacedKey key) {
        return CONFIGURED_FEATURES.get(key);
    }

    public static PlacedFeature getPlacedFeature(NamespacedKey key) {
        return PLACED_FEATURES.get(key);
    }

    public static int getPlacedFeaturesCount() {
        return FEATURES.size();
    }

    private static ConfiguredFeature parseConfiguredFeature(ConfiguredFeatureDTO dto, String file) {
        if (dto == null) {
            throw new IllegalStateException("Configured feature file " + file + " is empty or null!");
        }

        if ("standard_vein".equalsIgnoreCase(dto.type)) {
            Block block = getBlock(dto.block);
            if (block == null) {
                throw new IllegalStateException("Configured feature " + file + " references missing block '" + dto.block + "'");
            }
            return new StandardVeinFeature(block, dto.size > 0 ? dto.size : 8);
        } else if ("mega_vein".equalsIgnoreCase(dto.type)) {
            Block block = getBlock(dto.block);
            if (block == null) {
                throw new IllegalStateException("Configured feature " + file + " references missing ore block '" + dto.block + "'");
            }
            Block carrier = getBlock(dto.carrier);
            if (carrier == null) carrier = block;
            return new MegaVeinFeature(block, carrier, dto.ore_chance > 0 ? dto.ore_chance : 0.1);
        } else if ("tree".equalsIgnoreCase(dto.type)) {
            Block log = getBlock(dto.log);
            if (log == null) {
                throw new IllegalStateException("Tree configured feature " + file + " references missing log block '" + dto.log + "'");
            }
            Block leaves = getBlock(dto.leaves);
            if (leaves == null) {
                throw new IllegalStateException("Tree configured feature " + file + " references missing leaves block '" + dto.leaves + "'");
            }

            if (dto.shape == null || dto.shape.trim().isEmpty()) {
                throw new IllegalStateException("Tree configured feature " + file + " is missing required 'shape' property!");
            }
            TreeFeature.TreeShape shape;
            try {
                shape = TreeFeature.TreeShape.valueOf(dto.shape.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid tree shape '" + dto.shape + "' in configured feature " + file, e);
            }
            return new TreeConfiguredFeature(shape, log, leaves, dto.baseHeight > 0 ? dto.baseHeight : 4, dto.heightVariation > 0 ? dto.heightVariation : 3);
        }
        throw new IllegalStateException("Unknown configured_feature type '" + dto.type + "' in file " + file);
    }

    private static DistributionModel parseDistribution(de.delautrer.game.world.generation.feature.config.DistributionDTO dto) {
        if (dto == null) return new UniformDistribution(0, 128);

        if ("trapezoid".equalsIgnoreCase(dto.type)) {
            return new TrapezoidDistribution(dto.min_y, dto.max_y, dto.peak_y);
        }
        return new UniformDistribution(dto.min_y, dto.max_y);
    }

    private static Block getBlock(String name) {
        if (name == null) return null;
        return Registries.BLOCKS.get(NamespacedKey.fromString(name));
    }

    public static void generateOres(Chunk chunk, long seed, WorldGenerator wg) {
        Objects.requireNonNull(wg, "WorldGenerator cannot be null");
        if (!isInitialized) init();

        de.delautrer.game.world.generation.biome.Climate.TargetPoint[] chunkClimates = null;
        if (wg.getTerrainGenerator() != null && wg.getTerrainGenerator().getSampler() != null) {
            de.delautrer.game.world.generation.biome.MultiNoiseSampler sampler = wg.getTerrainGenerator().getSampler();
            chunkClimates = new de.delautrer.game.world.generation.biome.Climate.TargetPoint[Chunk.SIZE * Chunk.SIZE];
            int chunkX = chunk.getWorldX();
            int chunkZ = chunk.getWorldZ();
            for (int lx = 0; lx < Chunk.SIZE; lx++) {
                for (int lz = 0; lz < Chunk.SIZE; lz++) {
                    int worldX = chunkX * Chunk.SIZE + lx;
                    int worldZ = chunkZ * Chunk.SIZE + lz;
                    chunkClimates[lx * Chunk.SIZE + lz] = sampler.sample(worldX, worldZ);
                }
            }
        }

        for (PlacedFeature feature : FEATURES) {
            feature.generate(chunk, seed, chunkClimates);
        }
    }
}
