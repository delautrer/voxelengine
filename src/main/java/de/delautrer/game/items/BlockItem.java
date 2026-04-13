package de.delautrer.game.items;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.engine.player.Player;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.interaction.PlayerInteraction;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class BlockItem extends Item {
    public final Block block;

    public BlockItem(String name, int iconIndex, Block block) {
        super(name, iconIndex);
        this.block = block;
    }

    @Override
    public void onUseRightClick(World world, Player player, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (adjacentBlock == null) return;

        AABB blockBB = new AABB(
                new Vector3f(adjacentBlock.x, adjacentBlock.y, adjacentBlock.z),
                new Vector3f(adjacentBlock.x + 1, adjacentBlock.y + 1, adjacentBlock.z + 1)
        );

        if (!AABB.isColliding(player.getAABB(), blockBB)) {
            Chunk placeChunk = world.getChunkManager().getChunkAtBlock(adjacentBlock.x, adjacentBlock.y, adjacentBlock.z);
            if (placeChunk != null) {
                int lx = Math.floorMod(adjacentBlock.x, Chunk.SIZE);
                int lz = Math.floorMod(adjacentBlock.z, Chunk.SIZE);

                if (block == BlockRegistry.WATER) placeChunk.setBlock(lx, adjacentBlock.y, lz, block.getId(), (byte)8);
                else placeChunk.setBlock(lx, adjacentBlock.y, lz, block.getId());

                //interaction.updateChunkMesh(placeChunk, lx, lz);
            }
        }
    }
}