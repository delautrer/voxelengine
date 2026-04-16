package de.delautrer.game.items;

import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class BlockItem extends Item {
    public final Block block;

    public BlockItem(String name, int iconIndex, Block block) {
        super(name, iconIndex);
        this.block = block;
    }

    @Override
    public void onUseRightClick(World world, LocalPlayer player, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (adjacentBlock == null) return;

        Raycaster.RaycastResult rayHit = Raycaster.raycast(world, player.getCamera().getPosition(), player.getCamera().getFront(), 6.0f);
        if (rayHit == null) return;

        Vector3i placePos;
        BlockState clickedState = world.getBlockState(targetBlock.x, targetBlock.y, targetBlock.z);

        if (clickedState.getBlock().canBeReplaced(clickedState, this, rayHit.hitFace, rayHit.exactHit)) {
            placePos = targetBlock;
        } else {
            placePos = adjacentBlock;
        }

        AABB blockBB = new AABB(
                new Vector3f(placePos.x, placePos.y, placePos.z),
                new Vector3f(placePos.x + 1, placePos.y + 1, placePos.z + 1)
        );

        if (!AABB.isColliding(player.getAABB(), blockBB)) {
            if (this.block == BlockRegistry.WATER) {
                world.setBlockWithState(placePos.x, placePos.y, placePos.z, block.getId(), (byte)8);
            } else {
                BlockState newState = block.getStateForPlacement(world, player, placePos, rayHit.hitFace, rayHit.exactHit);
                world.setBlockState(placePos.x, placePos.y, placePos.z, newState);
            }
        }
    }
}