package de.delautrer.game.world.generation.structure;

import com.google.gson.Gson;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.TagIo;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.WorldGenerator;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseChunkGenerator;
import de.delautrer.game.world.generation.structure.dto.*;
import de.delautrer.game.world.generation.structure.processor.*;
import de.delautrer.game.world.persistence.WorldPalette;

import java.io.Reader;
import java.util.*;

public class StructureRegistry {
    private static final Gson GSON = new Gson();
    private static final Map<NamespacedKey, StructureTemplate> TEMPLATES = new HashMap<>();
    private static final Map<NamespacedKey, Structure> STRUCTURES = new HashMap<>();
    private static final List<StructureSet> STRUCTURE_SETS = new ArrayList<>();
    private static boolean frozen = false;

    public static void init() {
        TEMPLATES.clear();
        STRUCTURES.clear();
        STRUCTURE_SETS.clear();
        frozen = false;

        loadTemplates();
        loadStructures();
        loadStructureSets();

        if (TEMPLATES.isEmpty() || STRUCTURES.isEmpty() || STRUCTURE_SETS.isEmpty()) {
            throw new IllegalStateException("Failed to load structures: templates, structures, and structure sets must not be empty!");
        }

        System.out.println("Loaded " + TEMPLATES.size() + " structure templates, " + STRUCTURES.size() + " structures, " + STRUCTURE_SETS.size() + " structure sets.");
    }

    public static void freeze() {
        frozen = true;
    }

    public static int getTemplatesCount() {
        return TEMPLATES.size();
    }

    public static int getStructuresCount() {
        return STRUCTURES.size();
    }

    public static int getStructureSetsCount() {
        return STRUCTURE_SETS.size();
    }

    public static Map<NamespacedKey, Structure> getStructures() {
        return Collections.unmodifiableMap(STRUCTURES);
    }

    public static List<StructureSet> getStructureSets() {
        return Collections.unmodifiableList(STRUCTURE_SETS);
    }

    private static void loadTemplates() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/structure/template", ".json");
        for (String file : files) {
            String id = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            NamespacedKey key = NamespacedKey.fromString("veinstride:" + id);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/structure/template/" + file);
                StructureTemplateDTO dto = GSON.fromJson(reader, StructureTemplateDTO.class);
                if (dto == null || dto.palette == null || dto.blocks == null || dto.size == null || dto.size.length < 3) {
                    throw new IllegalStateException("Invalid structure template file: " + file);
                }

                Block[] paletteBlocks = new Block[dto.palette.size()];
                for (int i = 0; i < dto.palette.size(); i++) {
                    String blockKeyStr = dto.palette.get(i);
                    Block b = Registries.BLOCKS.get(blockKeyStr);
                    if (b == null) {
                        throw new IllegalStateException("Template " + file + " references unknown block '" + blockKeyStr + "'");
                    }
                    paletteBlocks[i] = b;
                }

                List<StructureTemplate.StructureBlock> blocks = new ArrayList<>();
                for (StructureTemplateDTO.BlockElementDTO elem : dto.blocks) {
                    if (elem.pos == null || elem.pos.length < 3) continue;
                    int dx = elem.pos[0];
                    int dy = elem.pos[1];
                    int dz = elem.pos[2];
                    int stateIdx = elem.state;
                    if (stateIdx < 0 || stateIdx >= paletteBlocks.length) {
                        throw new IllegalStateException("Template " + file + " block pos [" + dx + "," + dy + "," + dz + "] has out-of-range state index " + stateIdx);
                    }
                    Block block = paletteBlocks[stateIdx];
                    byte stateByte = (byte) stateIdx;
                    CompoundTag nbt = null;
                    if (elem.nbt != null && !elem.nbt.isJsonNull()) {
                        nbt = (CompoundTag) TagIo.fromJson(elem.nbt);
                    }
                    blocks.add(new StructureTemplate.StructureBlock(dx, dy, dz, block, stateByte, nbt));
                }

                TEMPLATES.put(key, new StructureTemplate(key, dto.size[0], dto.size[1], dto.size[2], blocks));
            } catch (Exception e) {
                System.err.println("[StructureRegistry] Failed to load template: " + file);
                throw new IllegalStateException("Failed to load template: " + file, e);
            }
        }
    }

    private static void loadStructures() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/structure", ".json");
        for (String file : files) {
            if (file.startsWith("template/") || file.contains("/template/")) {
                continue;
            }
            String id = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            NamespacedKey key = NamespacedKey.fromString("veinstride:" + id);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/structure/" + file);
                StructureDTO dto = GSON.fromJson(reader, StructureDTO.class);
                if (dto == null) {
                    throw new IllegalStateException("Structure file " + file + " is empty!");
                }

                String templateKeyStr = dto.getTemplate();
                if (templateKeyStr == null) {
                    throw new IllegalStateException("Structure " + file + " missing required field 'template'");
                }
                NamespacedKey templateKey = templateKeyStr.contains(":") ? NamespacedKey.fromString(templateKeyStr) : NamespacedKey.fromString("veinstride:" + templateKeyStr);
                StructureTemplate template = TEMPLATES.get(templateKey);
                if (template == null) {
                    throw new IllegalStateException("Structure " + file + " references unknown template '" + templateKeyStr + "'");
                }

                Set<NamespacedKey> biomes = new HashSet<>();
                List<String> rawBiomes = dto.getBiomesList();
                for (String bStr : rawBiomes) {
                    if (bStr.startsWith("#")) {
                        throw new IllegalStateException("Structure " + file + " references biome tag '" + bStr + "' which is not supported until Phase 1b!");
                    }
                    NamespacedKey bKey = bStr.contains(":") ? NamespacedKey.fromString(bStr) : NamespacedKey.fromString("veinstride:" + bStr);
                    if (Registries.BIOMES.get(bKey) == null) {
                        throw new IllegalStateException("Structure " + file + " references unknown biome '" + bStr + "'");
                    }
                    biomes.add(bKey);
                }

                List<StructureProcessor> processors = new ArrayList<>();
                List<StructureProcessorDTO> procDTOs = dto.getProcessors();
                if (procDTOs != null) {
                    for (StructureProcessorDTO pDTO : procDTOs) {
                        if (pDTO.type == null) continue;
                        String pType = pDTO.type.toLowerCase();
                        if (pType.endsWith("integrity")) {
                            processors.add(new IntegrityProcessor(pDTO.integrity));
                        } else if (pType.endsWith("gravity") || pType.endsWith("on_ground")) {
                            processors.add(new GravityProcessor(pDTO.heightmap));
                        } else if (pType.endsWith("rule") || pType.endsWith("replace")) {
                            Block inB = pDTO.input_block != null ? Registries.BLOCKS.get(pDTO.input_block) : null;
                            Block outB = pDTO.output_block != null ? Registries.BLOCKS.get(pDTO.output_block) : null;
                            processors.add(new RuleProcessor(inB, outB, pDTO.probability));
                        }
                    }
                }

                STRUCTURES.put(key, new Structure(key, template, dto.getStep(), biomes, processors));
            } catch (Exception e) {
                System.err.println("[StructureRegistry] Failed to load structure: " + file);
                throw new IllegalStateException("Failed to load structure: " + file, e);
            }
        }
    }

    private static void loadStructureSets() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/worldgen/structure_set", ".json");
        for (String file : files) {
            String id = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            NamespacedKey key = NamespacedKey.fromString("veinstride:" + id);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/worldgen/structure_set/" + file);
                StructureSetDTO dto = GSON.fromJson(reader, StructureSetDTO.class);
                if (dto == null) {
                    throw new IllegalStateException("Structure set file " + file + " is empty!");
                }

                List<StructureSet.WeightedStructure> weightedList = new ArrayList<>();
                List<StructureSetDTO.StructureEntryDTO> entries = dto.getStructureEntries();
                for (StructureSetDTO.StructureEntryDTO entry : entries) {
                    String sKeyStr = entry.structure;
                    if (sKeyStr == null || sKeyStr.isEmpty()) continue;
                    NamespacedKey sKey = sKeyStr.contains(":") ? NamespacedKey.fromString(sKeyStr) : NamespacedKey.fromString("veinstride:" + sKeyStr);
                    Structure s = STRUCTURES.get(sKey);
                    if (s == null) {
                        throw new IllegalStateException("Structure set " + file + " references unknown structure '" + sKeyStr + "'");
                    }
                    weightedList.add(new StructureSet.WeightedStructure(s, entry.weight));
                }

                StructureSetDTO.PlacementDTO pDTO = dto.getPlacement();
                STRUCTURE_SETS.add(new StructureSet(key, weightedList, pDTO.type, pDTO.spacing, pDTO.separation, pDTO.salt));
            } catch (Exception e) {
                System.err.println("[StructureRegistry] Failed to load structure set: " + file);
                throw new IllegalStateException("Failed to load structure set: " + file, e);
            }
        }
    }

    public static void generateStructures(Chunk chunk, long seed, WorldGenerator wg) {
        if (chunk == null || wg == null) return;
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        int chunkMinX = chunkX * Chunk.SIZE;
        int chunkMaxX = chunkMinX + Chunk.SIZE - 1;
        int chunkMinZ = chunkZ * Chunk.SIZE;
        int chunkMaxZ = chunkMinZ + Chunk.SIZE - 1;

        for (int ownerCX = chunkX - 2; ownerCX <= chunkX + 2; ownerCX++) {
            for (int ownerCZ = chunkZ - 2; ownerCZ <= chunkZ + 2; ownerCZ++) {
                for (StructureSet set : STRUCTURE_SETS) {
                    if (!set.isOwnerChunk(ownerCX, ownerCZ, seed)) continue;

                    Structure structure = set.selectStructure(seed, ownerCX, ownerCZ);
                    if (structure == null) continue;

                    StructureTemplate template = structure.getTemplate();
                    if (template == null) continue;

                    StructurePlacement.OriginResult origin = StructurePlacement.computeOrigin(wg, chunk, structure, template, ownerCX, ownerCZ, seed, set.getSalt());
                    if (origin == null) continue;

                    int originX = origin.originX;
                    int originY = origin.originY;
                    int originZ = origin.originZ;

                    int width = template.getSizeX();
                    int depth = template.getSizeZ();

                    int structMinX = originX;
                    int structMaxX = originX + width - 1;
                    int structMinZ = originZ;
                    int structMaxZ = originZ + depth - 1;

                    if (structMaxX < chunkMinX || structMinX > chunkMaxX || structMaxZ < chunkMinZ || structMinZ > chunkMaxZ) {
                        continue;
                    }

                    Random rand = new Random(seed ^ ((long) ownerCX * 1234567L ^ (long) ownerCZ * 7654321L ^ set.getSalt()));

                    for (StructureTemplate.StructureBlock sb : template.getBlocks()) {
                        int worldX = originX + sb.dx;
                        int worldY = originY + sb.dy;
                        int worldZ = originZ + sb.dz;

                        StructureProcessor.ProcessedBlock processed = new StructureProcessor.ProcessedBlock(sb.block, sb.state, sb.nbt);
                        for (StructureProcessor proc : structure.getProcessors()) {
                            processed = proc.process(processed, worldX, worldY, worldZ, rand);
                            if (processed == null) break;
                        }

                        if (processed != null) {
                            setBlockIfInChunk(chunk, wg, worldX, worldY, worldZ, processed.block, processed.state, processed.nbt);
                        }
                    }
                }
            }
        }
    }

    private static void setBlockIfInChunk(Chunk chunk, WorldGenerator wg, int worldX, int worldY, int worldZ, Block block, byte state, CompoundTag nbt) {
        if (block != null && block.isStructureVoid()) {
            block = Registries.BLOCKS.get("veinstride:air");
            nbt = null;
            state = 0;
        }

        int lx = worldX - chunk.getWorldX() * Chunk.SIZE;
        int lz = worldZ - chunk.getWorldZ() * Chunk.SIZE;
        WorldPalette palette = (wg != null) ? wg.getBlockPalette() : null;

        byte blockStateId = (byte) 0;

        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE && worldY >= Chunk.MIN_Y && worldY < Chunk.MAX_Y) {
            chunk.setBlock(lx, worldY, lz, block, blockStateId, palette);
            if (nbt != null) {
                chunk.setBlockEntityTag(lx, worldY, lz, nbt);
            }
        } else if (wg != null) {
            wg.addPendingBlock(worldX, worldY, worldZ, block, blockStateId, nbt);
        }
    }
}
