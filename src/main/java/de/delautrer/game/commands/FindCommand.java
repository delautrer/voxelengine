package de.delautrer.game.commands;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseSampler;
import de.delautrer.game.world.generation.structure.Structure;
import de.delautrer.game.world.generation.structure.StructurePlacement;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureSet;
import de.delautrer.game.world.generation.structure.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FindCommand implements ICommand {
    @Override
    public String getName() { return "find"; }

    @Override
    public String getUsage() { return "/find <biome|structure> <name>"; }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        if (args.length < 2) {
            manager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("biome")) {
            findBiome(player, world, args[1], manager);
        } else if (subCommand.equals("structure")) {
            findStructure(player, world, args[1], manager);
        } else {
            manager.sendMessageInChat("Usage: " + getUsage());
        }
    }

    private void findBiome(LocalPlayer player, World world, String targetBiomeInput, CommandManager manager) {
        String targetBiome = targetBiomeInput.toUpperCase();
        MultiNoiseSampler sampler = world.getChunkManager().getWorldGenerator().getTerrainGenerator().getSampler();

        int startX = (int) Math.floor(player.position.x);
        int startZ = (int) Math.floor(player.position.z);

        manager.sendMessageInChat("Searching for biome: " + targetBiome + " (Background thread started)...");

        new Thread(() -> {
            int range = 10000;
            int step = 32;

            for (int r = step; r <= range; r += step) {
                for (int x = -r; x <= r; x += step) {
                    if (checkBiome(sampler, startX + x, startZ + r, targetBiome)) {
                        teleportAndCopy(startX + x, startZ + r, targetBiome, manager);
                        return;
                    }
                    if (checkBiome(sampler, startX + x, startZ - r, targetBiome)) {
                        teleportAndCopy(startX + x, startZ - r, targetBiome, manager);
                        return;
                    }
                }
                for (int z = -r + step; z <= r - step; z += step) {
                    if (checkBiome(sampler, startX + r, startZ + z, targetBiome)) {
                        teleportAndCopy(startX + r, startZ + z, targetBiome, manager);
                        return;
                    }
                    if (checkBiome(sampler, startX - r, startZ + z, targetBiome)) {
                        teleportAndCopy(startX - r, startZ + z, targetBiome, manager);
                        return;
                    }
                }

                if (r % 512 == 0) {
                    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                }
            }

            manager.sendMessageInChat("Biome " + targetBiome + " not found even within " + range + " blocks!");
        }).start();
    }

    private boolean checkBiome(MultiNoiseSampler sampler, int x, int z, String target) {
        Climate.TargetPoint climate = sampler.sample(x, z);
        Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);
        return biome != null && biome.getName().equalsIgnoreCase(target);
    }

    private void findStructure(LocalPlayer player, World world, String targetStructureInput, CommandManager manager) {
        String targetStructure = targetStructureInput.toLowerCase();
        if (targetStructure.startsWith("veinstride:")) {
            targetStructure = targetStructure.substring("veinstride:".length());
        }

        de.delautrer.game.world.WorldGenerator wg = world.getChunkManager().getWorldGenerator();
        long seed = world.getSeed();

        int startChunkX = (int) Math.floor(player.position.x) >> 4;
        int startChunkZ = (int) Math.floor(player.position.z) >> 4;

        manager.sendMessageInChat("Searching for structure: " + targetStructure + " (Background thread started)...");

        final String searchName = targetStructure;
        new Thread(() -> {
            int maxChunkRadius = 500; // ~8000 blocks
            for (int r = 0; r <= maxChunkRadius; r++) {
                for (int cx = startChunkX - r; cx <= startChunkX + r; cx++) {
                    int cz1 = startChunkZ + r;
                    if (checkStructureChunk(wg, seed, cx, cz1, searchName, manager)) return;
                    int cz2 = startChunkZ - r;
                    if (r > 0 && checkStructureChunk(wg, seed, cx, cz2, searchName, manager)) return;
                }
                for (int cz = startChunkZ - r + 1; cz <= startChunkZ + r - 1; cz++) {
                    int cx1 = startChunkX + r;
                    if (checkStructureChunk(wg, seed, cx1, cz, searchName, manager)) return;
                    int cx2 = startChunkX - r;
                    if (checkStructureChunk(wg, seed, cx2, cz, searchName, manager)) return;
                }

                if (r % 32 == 0 && r > 0) {
                    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                }
            }

            manager.sendMessageInChat("Structure " + searchName + " not found within " + (maxChunkRadius * 16) + " blocks!");
        }).start();
    }

    private boolean checkStructureChunk(de.delautrer.game.world.WorldGenerator wg, long seed, int chunkX, int chunkZ, String targetStructure, CommandManager manager) {
        for (StructureSet set : StructureRegistry.getStructureSets()) {
            if (!set.isOwnerChunk(chunkX, chunkZ, seed)) continue;

            Structure structure = set.selectStructure(seed, chunkX, chunkZ);
            if (structure == null) continue;

            String keyStr = structure.getKey().getKey().toLowerCase();
            String fullStr = structure.getKey().toString().toLowerCase();

            if (keyStr.equals(targetStructure) || fullStr.equals(targetStructure)) {
                StructureTemplate template = structure.getTemplate();
                if (template == null) continue;

                StructurePlacement.OriginResult origin = StructurePlacement.computeOrigin(wg, null, structure, template, chunkX, chunkZ, seed, set.getSalt());
                if (origin == null) {
                    // Structure would be skipped by generate (water/tilt/biome) - keep scanning!
                    continue;
                }

                int originX = origin.originX;
                int originY = origin.originY;
                int originZ = origin.originZ;

                int sizeX = template.getSizeX();
                int sizeY = template.getSizeY();
                int sizeZ = template.getSizeZ();

                int tpX = originX + sizeX / 2;
                int tpY = originY + sizeY + 2; // Stand on top of the roof!
                int tpZ = originZ + sizeZ / 2;

                teleportAndCopy(tpX, tpY, tpZ, originX, originY, originZ, structure.getKey().toString(), manager);
                return true;
            }
        }
        return false;
    }

    private void teleportAndCopy(int x, int z, String biomeName, CommandManager manager) {
        int y = 95;
        String tpCommand = "/tp " + x + " " + y + " " + z;

        try {
            if (InputManager.INSTANCE != null) {
                InputManager.INSTANCE.setClipboardString(tpCommand);
                manager.sendMessageInChat("Found " + biomeName + " at " + x + ", " + z + ". TP command copied!");
            } else {
                manager.sendMessageInChat("Found " + biomeName + " at " + x + ", " + z + ". Coords: " + x + " " + y + " " + z);
            }
        } catch (Exception e) {
            manager.sendMessageInChat("Found " + biomeName + " at " + x + ", " + z + ". Coords: " + x + " " + y + " " + z);
        }
    }

    private void teleportAndCopy(int tpX, int tpY, int tpZ, int originX, int originY, int originZ, String name, CommandManager manager) {
        String tpCommand = "/tp " + tpX + " " + tpY + " " + tpZ;

        try {
            if (InputManager.INSTANCE != null) {
                InputManager.INSTANCE.setClipboardString(tpCommand);
                manager.sendMessageInChat("Found " + name + " at origin (" + originX + ", " + originY + ", " + originZ + "). TP command copied!");
            } else {
                manager.sendMessageInChat("Found " + name + " at origin (" + originX + ", " + originY + ", " + originZ + "). Coords: " + tpX + " " + tpY + " " + tpZ);
            }
        } catch (Exception e) {
            manager.sendMessageInChat("Found " + name + " at origin (" + originX + ", " + originY + ", " + originZ + "). Coords: " + tpX + " " + tpY + " " + tpZ);
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            if ("biome".startsWith(current)) completions.add("biome");
            if ("structure".startsWith(current)) completions.add("structure");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("biome")) {
                for (Biome b : MultiNoiseBiomeRegistry.getBiomes()) {
                    if (b.getName().toLowerCase().startsWith(current)) {
                        completions.add(b.getName().toLowerCase());
                    }
                }
            } else if (args[0].equalsIgnoreCase("structure")) {
                for (Map.Entry<NamespacedKey, Structure> entry : StructureRegistry.getStructures().entrySet()) {
                    String shortName = entry.getKey().getKey().toLowerCase();
                    String fullName = entry.getKey().toString().toLowerCase();
                    if (shortName.startsWith(current)) {
                        completions.add(shortName);
                    } else if (fullName.startsWith(current)) {
                        completions.add(fullName);
                    }
                }
            }
        }
        return completions;
    }
}
