package de.delautrer.game.commands;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.World;

import java.util.ArrayList;
import java.util.List;

public class ClearCommand implements ICommand {
    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getUsage() {
        return "/clear - Clears your inventory";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.TOTAL_SIZE; i++) {
            inv.setStack(i, null);
        }
        manager.sendMessageInChat("Your inventory has been cleared.");
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        return new ArrayList<>();
    }
}