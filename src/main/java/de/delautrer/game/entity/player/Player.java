package de.delautrer.game.entity.player;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3d;

public class Player extends Entity {

    protected final PlayerInventory inventory;
    protected IInventory openedExternalInventory = null;
    protected GameMode gameMode = GameMode.SURVIVAL;

    protected final float maxHealth = 20.0f;
    protected float currentHealth = 20.0f;
    protected float eyeHeight = 1.62f;

    protected boolean isSneaking = false;
    public boolean isSneaking() { return isSneaking; }
    public void setSneaking(boolean isSneaking) { this.isSneaking = isSneaking; }
    protected boolean isFlying = false;
    protected boolean isSprinting = false;

    protected boolean swimLock = false;
    protected boolean isSwimming = false;
    protected boolean isInWater = false;
    protected boolean isHeadInWater = false;
    protected boolean wasInWater = false;

    protected float swimProgress = 0.0f;
    protected int tickCounter = 0;

    public Player(Vector3d spawnPosition) {
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

    @Override
    public void onTick(de.delautrer.game.world.World world) {
        tickCounter++;

        if (gameMode != GameMode.SPECTATOR && !isDead) {
            handleTickSuffocation(world);
            handleRegeneration();
        }
    }

    private void handleTickSuffocation(de.delautrer.game.world.World world) {
        int bx = (int) Math.floor(position.x);
        int byHead = (int) Math.floor(getEyePosition().y);
        int bz = (int) Math.floor(position.z);

        Block headBlock = world.getBlock(bx, byHead, bz);

        if (headBlock.isSolid && !headBlock.isTransparent) {
            if (tickCounter % 20 == 0) {
                damage(1.0f);
            }
        }
    }

    private void handleRegeneration() {
        if (currentHealth < maxHealth && !isDead) {
            // Alle 4 Sekunden (80 Ticks) einen halben Herzschlag (1 HP) heilen
            if (tickCounter % 80 == 0) {
                currentHealth = Math.min(maxHealth, currentHealth + 1.0f);
            }
        }
    }

    public PlayerInventory getInventory() {
        return inventory;
    }
    public Vector3d getEyePosition() {
        float currentEyeHeight = 1.62f + (0.4f - 1.62f) * swimProgress;
        return new Vector3d(position.x, position.y + currentEyeHeight, position.z);
    }
    public boolean isHeadInWater() {
        return isHeadInWater;
    }
    public float getHealth() { return currentHealth; }
    public float getMaxHealth() { return maxHealth; }
    public void damage(float amount) {
        if (isDead || amount <= 0) return;

        currentHealth -= amount;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isDead = true;
        }
    }
    public void openInventory(IInventory inventory) {
        this.openedExternalInventory = inventory;
    }
    public void closeInventory() {
        this.openedExternalInventory = null;
    }
    public IInventory getOpenedInventory() {
        return openedExternalInventory;
    }

    public void setCurrentHealth(float currentHealth) {
        this.currentHealth = currentHealth > maxHealth ? maxHealth : currentHealth;
    }
    public float getCurrentHealth() {
        return currentHealth;
    }
}