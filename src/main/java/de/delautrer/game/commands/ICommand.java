package de.delautrer.game.commands;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import java.util.List;

public interface ICommand {
    String getName();
    String getUsage();
    void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager);
    List<String> getTabCompletions(LocalPlayer player, String[] args);
}