package de.delautrer.game.commands;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;


public class TimeCommand implements ICommand {
    @Override
    public String getName() {
        return "time";
    }

    @Override
    public String getUsage() {
        return "/time <set|query> [day|night|<number>] - Set's or queries the time of day";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        if (args.length == 0) {
            manager.sendMessageInChat("Nutzung: /time <set|query> [day|night|<number>]");
            return;
        }

        if (args[0].equalsIgnoreCase("query")) {
            manager.sendMessageInChat("Current time: " + world.getSkyManager().getTimeOfDay());
        } else if (args[0].equalsIgnoreCase("set") && args.length > 1) {
            if (args[1].equalsIgnoreCase("day")) {
                world.getSkyManager().setTimeOfDay(8f); // Minecraft 0 Ticks = Sonnenaufgang/Morgen
                manager.sendMessageInChat("Time set day");
            } else if (args[1].equalsIgnoreCase("night")) {
                world.getSkyManager().setTimeOfDay(22f); // Minecraft 13000 Ticks
                manager.sendMessageInChat("Time set night");
            } else {
                try {
                    float t = Float.parseFloat(args[1]);
                    world.getSkyManager().setTimeOfDay(t % 24.0f);
                    manager.sendMessageInChat("Time was set to " + t + ".");
                } catch (NumberFormatException e) {
                    manager.sendMessageInChat("The fuck is this input bro.");
                }
            }
        } else {
            manager.sendMessageInChat("Usage: /time <set|query> [day|night|<number>]");
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("set");
            list.add("query");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            list.add("day");
            list.add("night");
            list.add("0");
        }
        return list;
    }
}