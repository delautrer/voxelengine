package de.delautrer.game.items;

import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.entity.player.Player;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

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

        BlockState newState = null;
        if (this.block == BlockRegistry.WATER) {
            newState = block.getDefaultState();
        } else {
            newState = block.getStateForPlacement(world, player, placePos, rayHit.hitFace, rayHit.exactHit);
        }

        if (newState == null) return;

        AABB pBox = player.getAABB();
        float epsilon = 0.02f;
        AABB safePlayerBox = new AABB(
                new Vector3f(pBox.min.x + epsilon, pBox.min.y + epsilon, pBox.min.z + epsilon),
                new Vector3f(pBox.max.x - epsilon, pBox.max.y - epsilon, pBox.max.z - epsilon)
        );

        boolean canPlace = true;
        if (this.block.isSolid) {
            List<AABB> blockBoxes = block.getBoundingBoxes(newState);
            for (AABB box : blockBoxes) {
                AABB worldBox = new AABB(
                        new Vector3f(box.min).add(placePos.x, placePos.y, placePos.z),
                        new Vector3f(box.max).add(placePos.x, placePos.y, placePos.z)
                );
                if (AABB.isColliding(safePlayerBox, worldBox)) {
                    canPlace = false;
                    break;
                }
            }
        }

        if (canPlace) {
            if (this.block == BlockRegistry.WATER) {
                world.setBlockWithState(placePos.x, placePos.y, placePos.z, block.getId(), (byte)8);
            } else {
                world.setBlockState(placePos.x, placePos.y, placePos.z, newState);
            }
        }
    }
}