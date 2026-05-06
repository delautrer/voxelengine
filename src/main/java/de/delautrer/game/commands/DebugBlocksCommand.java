package de.delautrer.game.commands;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

public class DebugBlocksCommand implements ICommand {
    @Override
    public String getName() {
        return "debugblocks";
    }

    @Override
    public String getUsage() {
        return "/debugblocks - Generates a debug block palette";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager manager) {
        int startX = (int) Math.floor(player.position.x);
        int startZ = (int) Math.floor(player.position.z);
        int targetY = 128;

        player.position.set(startX, targetY + 2.0f, startZ);
        player.velocity.set(0);

        Block[] allBlocks = BlockRegistry.getAll().values().toArray(new Block[0]);
        int count = allBlocks.length;

        int gridSize = (int) Math.ceil(Math.sqrt(count));

        byte airId = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air").getId();
        byte floorId = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "grass_block").getId();

        // 1. ZUERST DAS KOMPLETTE RASTER LEEREN UND DEN BODEN BAUEN
        // (Das verhindert, dass spätere Block-Updates beim Platzieren fehlschlagen)
        for (int gx = -1; gx <= gridSize * 2; gx++) {
            for (int gz = -1; gz <= gridSize * 2; gz++) {
                int bx = startX + gx;
                int bz = startZ + gz;

                // 3 Blöcke hoch Luft machen, damit wirklich nichts im Weg ist
                world.setBlock(bx, targetY, bz, airId);
                world.setBlock(bx, targetY + 1, bz, airId);
                world.setBlock(bx, targetY + 2, bz, airId);

                // Darunter einen massiven Grasboden ziehen
                world.setBlock(bx, targetY - 1, bz, floorId);
            }
        }

        // 2. JETZT ERST DIE BLÖCKE PLATZIEREN
        int blockIndex = 0;
        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                if (blockIndex >= count) break; // Nicht weiterlaufen, wenn wir alle haben

                int bx = startX + (gx * 2);
                int bz = startZ + (gz * 2);

                // Block aus der Registry platzieren
                System.out.println(allBlocks[blockIndex]);
                world.setBlock(bx, targetY, bz, allBlocks[blockIndex].getId());
                blockIndex++;
            }
        }

        manager.sendMessageInChat("Debug-Blockgrid (" + count + " Blocks) generated!");
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        return new ArrayList<>();
    }
}
