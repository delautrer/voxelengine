package de.delautrer.game.loot;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.ItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    public class LootEntry {
        public String item;
        public float chance;
        public int min = 1;
        public int max = 1;
    }

    public class LootPool {
        public int rolls = 1;
        public List<LootEntry> entries = new ArrayList<>();
    }

    public List<LootPool> pools = new ArrayList<>();

    public List<ItemStack> generateLoot() {
        List<ItemStack> drops = new ArrayList<>();
        Random rand = new Random();

        for (LootPool pool : pools) {
            for (int i = 0; i < pool.rolls; i++) {
                for (LootEntry entry : pool.entries) {
                    if (rand.nextFloat() <= entry.chance) {
                        int amount = entry.min + rand.nextInt(entry.max - entry.min + 1);
                        Item item = ItemRegistry.get(entry.item);
                        if (item != null) {
                            drops.add(new ItemStack(item, amount));
                        }
                    }
                }
            }
        }
        return drops;
    }
}
