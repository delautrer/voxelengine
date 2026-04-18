package de.delautrer.game.commands;

import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;

public class GameModeCommand implements ICommand {

    @Override
    public String getName() { return "gamemode"; }

    @Override
    public String getUsage() { return "/gamemode <0|1|2>"; }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length != 1) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }
        try {
            int modeId = Integer.parseInt(args[0]);
            GameMode mode = GameMode.fromId(modeId);
            player.setGameMode(mode);
            commandManager.sendMessageInChat("Gamemode set to " + mode.name());
        } catch (NumberFormatException e) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] modes = {"0", "1", "2"};
            for (String m : modes) {
                if (m.startsWith(args[0])) completions.add(m);
            }
        }
        return completions;
    }
}