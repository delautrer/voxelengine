package de.delautrer.game.entity.player;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public class Player extends Entity {

    protected final PlayerInventory inventory;
    protected float eyeHeight = 1.62f;
    protected boolean isSneaking = false;
    protected boolean isFlying = false;
    protected boolean isSprinting = false;

    protected boolean swimLock = false;
    protected boolean isSwimming = false;
    protected boolean isInWater = false;
    protected boolean isHeadInWater = false;

    // NEU: Animations-Fortschritt (0.0 = Stehen, 1.0 = Schwimmen)
    protected float swimProgress = 0.0f;

    public Player(Vector3f spawnPosition) {
        super(spawnPosition);
        this.inventory = new PlayerInventory();
    }

    @Override
    public void update(float deltaTime, ChunkManager chunkManager) {
        if (!isFlying && !isInWater) {
            velocity.y += gravity * deltaTime;
        }
        moveAndCollide(chunkManager, deltaTime, isSneaking);
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public Vector3f getEyePosition() {
        float currentEyeHeight = 1.62f + (0.4f - 1.62f) * swimProgress;
        return new Vector3f(position.x, position.y + currentEyeHeight, position.z);
    }

    public boolean isHeadInWater() {
        return isHeadInWater;
    }
}