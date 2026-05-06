package de.delautrer.game.items;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.BlockPlaceEvent;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.entity.player.PlayerInteraction;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;


public class BlockItem extends Item {
    public final Block block;

    public BlockItem(String name, String textureName, Block block) {
        super(name, textureName);
        this.block = block;
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer player, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
        if (adjacentBlock == null) return false;

        Vector3d camPos = player.getCamera().getPosition();
        Raycaster.RaycastResult rayHit = Raycaster.raycast(world, new Vector3f((float)camPos.x, (float)camPos.y, (float)camPos.z), player.getCamera().getFront(), 6.0f);
        if (rayHit == null) return false;

        Vector3i placePos;
        BlockState clickedState = world.getBlockState(targetBlock.x, targetBlock.y, targetBlock.z);

        if (clickedState.getBlock().canBeReplaced(clickedState, this, rayHit.hitFace, rayHit.exactHit)) {
            placePos = targetBlock;
        } else {
            placePos = adjacentBlock;
        }

        BlockState newState = null;
        if (this.block == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water")) {
            newState = block.getDefaultState();
        } else {
            newState = block.getStateForPlacement(world, player, placePos, rayHit.hitFace, rayHit.exactHit);
        }

        if (newState == null) return false;

        AABB pBox = player.getAABB();
        float epsilon = 0.02f;
        AABB safePlayerBox = new AABB(
                new Vector3f(pBox.min.x + epsilon, pBox.min.y + epsilon, pBox.min.z + epsilon),
                new Vector3f(pBox.max.x - epsilon, pBox.max.y - epsilon, pBox.max.z - epsilon)
        );

        boolean canPlace = true;
        List<AABB> blockBoxes = block.getBoundingBoxes(newState);

        if (blockBoxes != null && !blockBoxes.isEmpty()) {
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
            BlockPlaceEvent placeEvent = new BlockPlaceEvent(player, placePos, newState);
            interaction.getEventBus().publish(placeEvent);

            if (!placeEvent.isCancelled()) {
                if (this.block == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "water")) {
                    world.setBlockWithState(placePos.x, placePos.y, placePos.z, block.getId(), (byte)8);
                } else {
                    world.setBlockState(placePos.x, placePos.y, placePos.z, newState);
                }

                SoundManager.playEvent(this.block.getSoundMaterialName(), "walk", 0.4f);


                return true;
            }
        }

        return false;
    }

    public Block getBlock() {
        return block;
    }
}
