package de.delautrer.game.blocks;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public interface IInteractable {
    /**
     * Wird aufgerufen, wenn der Spieler einen Rechtsklick auf den Block macht.
     * @return true, wenn die Interaktion erfolgreich war (es wird dann z.B. kein Block platziert).
     */
    boolean onInteract(World world, Vector3i pos, LocalPlayer player);
}