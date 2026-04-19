package de.delautrer.game.entity.player;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public class Player extends Entity {

    protected final Inventory inventory;
    protected float eyeHeight = 1.62f;
    protected boolean isSneaking = false;
    protected boolean isFlying = false;
    protected boolean isSprinting = false;

    public Player(Vector3f spawnPosition) {
        super(spawnPosition);
        this.inventory = new Inventory();
    }

    @Override
    public void update(float deltaTime, ChunkManager chunkManager) {
        if(!isFlying)   velocity.y += gravity * deltaTime;

        moveAndCollide(chunkManager, deltaTime, isSneaking);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Vector3f getEyePosition() {
        return new Vector3f(position.x, position.y + eyeHeight, position.z);
    }
}