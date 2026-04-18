package de.delautrer.game.commands;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;

public class TeleportCommand implements ICommand {
    @Override
    public String getName() { return "tp"; }

    @Override
    public String getUsage() { return "/tp <x> <y> <z>"; }

    // Hilfsmethode für das Rechnen mit ~
    private float parseCoordinate(String arg, float currentPos) throws NumberFormatException {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return currentPos; // Nur "~" -> aktuelle Position
            return currentPos + Float.parseFloat(arg.substring(1)); // "~-10" -> Pos - 10
        }
        return Float.parseFloat(arg);
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length != 3) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }
        try {
            float x = parseCoordinate(args[0], player.position.x);
            float y = parseCoordinate(args[1], player.position.y);
            float z = parseCoordinate(args[2], player.position.z);

            player.position.set(x, y, z);
            player.velocity.set(0);
            commandManager.sendMessageInChat("Teleported to: " + String.format("%.2f, %.2f, %.2f", x, y, z));

        } catch (NumberFormatException e) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("~");
            completions.add(String.valueOf(Math.round(player.position.x)));
        } else if (args.length == 2) {
            completions.add("~");
            completions.add(String.valueOf(Math.round(player.position.y)));
        } else if (args.length == 3) {
            completions.add("~");
            completions.add(String.valueOf(Math.round(player.position.z)));
        }

        String currentArg = args[args.length - 1];
        List<String> filtered = new ArrayList<>();
        for (String c : completions) {
            if (c.startsWith(currentArg)) filtered.add(c);
        }
        return filtered;
    }
}