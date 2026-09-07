package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import org.joml.Vector3i;

public interface IPairedBlock {
    Vector3i getPartnerPos(Vector3i pos, BlockState state);
    Vector3i getPrimaryPos(Vector3i pos, BlockState state);
    boolean isValidPartner(BlockState self, BlockState other);
}
