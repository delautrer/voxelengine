package de.delautrer.game.commands;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseSampler;
import java.util.ArrayList;
import java.util.List;

public class FindCommand implements ICommand {
    @Override
    public String getName() { return "find"; }

    @Override
    public String getUsage() { return "/find biome <biome_name>"; }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("biome")) {
            manager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String targetBiome = args[1].toUpperCase();
        MultiNoiseSampler sampler = world.getChunkManager().getWorldGenerator().getTerrainGenerator().getSampler();
        
        int startX = (int) Math.floor(player.position.x);
        int startZ = (int) Math.floor(player.position.z);

        manager.sendMessageInChat("Searching for biome: " + targetBiome + " (Background thread started)...");

        // Wir starten die Suche in einem Hintergrund-Thread, um das Spiel nicht einzufrieren
        new Thread(() -> {
            int range = 10000; // Riesiger Suchradius
            int step = 32;

            for (int r = step; r <= range; r += step) {
                // Wir suchen in konzentrischen Quadraten
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
                
                // Kurze Pause, um die CPU nicht zu 100% zu blockieren
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

    private void teleportAndCopy(int x, int z, String biomeName, CommandManager manager) {
        int y = 95; 
        String tpCommand = "/tp " + x + " " + y + " " + z;
        
        // Da wir in einem Background-Thread sind, müssen wir vorsichtig sein mit GLFW
        // glfwSetClipboardString ist im Hauptthread oder Threads mit Context sicher?
        // Meistens nur Hauptthread. Wir versuchen es, ansonsten geben wir nur die Coords aus.
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

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            if ("biome".startsWith(current)) completions.add("biome");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("biome")) {
            for (Biome b : MultiNoiseBiomeRegistry.getBiomes()) {
                if (b.getName().toLowerCase().startsWith(current)) {
                    completions.add(b.getName().toLowerCase());
                }
            }
        }
        return completions;
    }
}
