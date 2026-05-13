package de.delautrer.game.commands;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.biome.Biome;
import de.delautrer.game.world.generation.biome.Climate;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseSampler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBiomesCommand implements ICommand {
    @Override
    public String getName() { return "testbiomes"; }

    @Override
    public String getUsage() { return "/testbiomes - Analyzes biome distribution around the player and copies to clipboard"; }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        MultiNoiseSampler sampler = world.getChunkManager().getWorldGenerator().getTerrainGenerator().getSampler();
        List<Biome> allBiomes = MultiNoiseBiomeRegistry.getBiomes();
        
        Map<String, Integer> counts = new HashMap<>();
        for (Biome b : allBiomes) counts.put(b.getName(), 0);

        int range = 2000;
        int step = 16;
        int totalPoints = 0;

        manager.sendMessageInChat("Sampling distribution (Range: " + range + ")...");

        StringBuilder sb = new StringBuilder();
        sb.append("--- Biome Distribution Result ---\n");

        for (int x = -range; x <= range; x += step) {
            for (int z = -range; z <= range; z += step) {
                Climate.TargetPoint climate = sampler.sample((int)player.position.x + x, (int)player.position.z + z);
                Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);
                if (biome != null) {
                    counts.put(biome.getName(), counts.get(biome.getName()) + 1);
                }
                totalPoints++;
            }
        }

        manager.sendMessageInChat("--- Result ---");
        for (Biome b : allBiomes) {
            int count = counts.get(b.getName());
            float percent = (count * 100.0f) / totalPoints;
            String line = String.format("%s: %.2f%% (%d points)", b.getName(), percent, count);
            manager.sendMessageInChat(line);
            sb.append(line).append("\n");
        }

        if (InputManager.INSTANCE != null) {
            InputManager.INSTANCE.setClipboardString(sb.toString());
            manager.sendMessageInChat("Results copied to clipboard!");
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        return List.of();
    }
}
