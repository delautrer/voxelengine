package de.delautrer.game.commands;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;

import java.util.ArrayList;
import java.util.List;

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

        byte airId = BlockRegistry.AIR.getId();
        // Nimm am besten Gras oder Erde als Boden, damit die Pflanzen darauf überleben können!
        byte floorId = BlockRegistry.GRASS_BLOCK.getId();

        // 1. ZUERST DEN BODEN BAUEN
        for (int gx = -1; gx < gridSize * 2; gx++) {
            for (int gz = -1; gz < gridSize * 2; gz++) {
                world.setBlock(startX + gx, targetY - 1, startZ + gz, floorId);
            }
        }

        // 2. DANACH DIE BLÖCKE PLATZIEREN
        int blockIndex = 0;
        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                int bx = startX + (gx * 2);
                int bz = startZ + (gz * 2);

                // Platz über dem Boden schaffen
                world.setBlock(bx, targetY, bz, airId);
                world.setBlock(bx, targetY + 1, bz, airId);
                world.setBlock(bx, targetY + 2, bz, airId);

                // Block aus der Registry platzieren
                if (blockIndex < count) {
                    world.setBlock(bx, targetY, bz, allBlocks[blockIndex].getId());
                    blockIndex++;
                }
            }
        }

        manager.sendMessageInChat("Debug-Blockgrid (" + count + " Blocks) generated!");
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        return new ArrayList<>();
    }
}