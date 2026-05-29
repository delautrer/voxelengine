package de.delautrer.game.commands;

import de.delautrer.Constants;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;

public class GiveCommand implements ICommand {

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getUsage() {
        return "/give <item_id> [amount]";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length < 1 || args.length > 2) {
            commandManager.sendMessageInChat("Usage: " + getUsage());
            return;
        }

        String itemIdInput = args[0];
        String fullId;
        try {
            // NamespacedKey.fromString handles the default namespace (Constants.NAMESPACE) if none is provided
            fullId = de.delautrer.game.registry.NamespacedKey.fromString(itemIdInput).toString();
        } catch (IllegalArgumentException e) {
            commandManager.sendMessageInChat("Invalid item ID format: " + itemIdInput);
            return;
        }

        Item item = ItemRegistry.get(fullId);
        if (item == null) {
            commandManager.sendMessageInChat("Unknown item: " + itemIdInput);
            return;
        }

        int amount = 1;
        if (args.length == 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    commandManager.sendMessageInChat("Amount must be positive.");
                    return;
                }
                
                if (amount > item.getMaxStackSize()) {
                    amount = item.getMaxStackSize();
                    commandManager.sendMessageInChat("Capping amount to max stack size: " + amount);
                }
            } catch (NumberFormatException e) {
                commandManager.sendMessageInChat("Invalid amount: " + args[1]);
                return;
            }
        }

        ItemStack stack = new ItemStack(item, amount);
        int remaining = player.getInventory().addItem(stack);

        if (remaining == 0) {
            commandManager.sendMessageInChat("Gave " + amount + "x " + item.getName() + " to you.");
        } else if (remaining < amount) {
            commandManager.sendMessageInChat("Gave " + (amount - remaining) + "x " + item.getName() + " to you (Inventory full).");
        } else {
            commandManager.sendMessageInChat("Inventory is full!");
        }
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String key : ItemRegistry.getAll().keySet()) {
                String lowerKey = key.toLowerCase();
                
                // Full ID match (e.g. "engine:dirt" or "mod:item")
                if (lowerKey.startsWith(input)) {
                    completions.add(key);
                }
                
                // Short ID match (only for default namespace)
                if (key.startsWith(Constants.NAMESPACE + ":")) {
                    String shortKey = key.substring(Constants.NAMESPACE.length() + 1);
                    if (shortKey.toLowerCase().startsWith(input)) {
                        // Avoid duplicates if input matches both full and short ID
                        if (!completions.contains(shortKey)) {
                            completions.add(shortKey);
                        }
                    }
                }
            }
        } else if (args.length == 2) {
            String itemIdInput = args[0];
            String fullId;
            try {
                fullId = de.delautrer.game.registry.NamespacedKey.fromString(itemIdInput).toString();
                Item item = ItemRegistry.get(fullId);
                if (item != null) {
                    String maxStack = String.valueOf(item.getMaxStackSize());
                    if (maxStack.startsWith(args[1])) {
                        completions.add(maxStack);
                    }
                    if ("1".startsWith(args[1]) && !maxStack.equals("1")) {
                        completions.add("1");
                    }
                }
            } catch (Exception ignored) {}
        }
        return completions;
    }
}
