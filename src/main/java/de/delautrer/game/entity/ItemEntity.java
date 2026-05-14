package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class ItemEntity extends Entity {

    public ItemStack stack;
    public float pickupDelay = 1.0f;
    public AABB boundingBox;
    public int renderPhase;

    private float age = 0.0f;
    private float stackCheckTimer = 0.0f;

    public ItemEntity(ItemStack stack, Vector3d spawnPos, Vector3f initialVelocity) {
        super(spawnPos);
        this.stack = stack;
        this.velocity = new Vector3f(initialVelocity);
        this.boundingBox = new AABB(new Vector3f(-0.125f, 0, -0.125f), new Vector3f(0.125f, 0.25f, 0.125f));
        this.renderPhase = (int)(Math.random() * 4);
    }

    @Override
    public void update(float deltaTime, ChunkManager cm) {
        // Diese Methode wird momentan nur vom World-Code OHNE World-Referenz aufgerufen.
        // Wir brauchen für das Stacking aber die World-Referenz.
        // Da du im World.java die Schleife hast, überladen wir die Update Methode unten!
    }

    // NEUE UPDATE METHODE: Wird von World.java aufgerufen
    public void update(float deltaTime, ChunkManager cm, World world) {
        if (isDead) return;

        age += deltaTime;
        if (pickupDelay > 0) pickupDelay -= deltaTime;

        // 1. SCHWERKRAFT & KOLLISION
        velocity.y -= 20.0f * deltaTime;

        double nextX = position.x + velocity.x * deltaTime;
        double nextY = position.y + velocity.y * deltaTime;
        double nextZ = position.z + velocity.z * deltaTime;

        Chunk c = cm.getChunkAtBlock((int) Math.floor(nextX), (int) Math.floor(nextY), (int) Math.floor(nextZ));
        if (c != null) {
            byte blockBelowId = c.getBlock(Math.floorMod((int) Math.floor(nextX), Chunk.SIZE), (int) Math.floor(nextY), Math.floorMod((int) Math.floor(nextZ), Chunk.SIZE));
            de.delautrer.game.blocks.Block blockBelow = BlockRegistry.get(blockBelowId);
            
            if (blockBelow.isSolid) {
                nextY = Math.floor(nextY) + 1.001f;
                velocity.y = 0;
                velocity.x *= 0.5f;
                velocity.z *= 0.5f;
            }
        }
        position.set(nextX, nextY, nextZ);

        // 2. STACKING LOGIK
        stackCheckTimer += deltaTime;
        if (stackCheckTimer >= 0.2f && velocity.lengthSquared() < 0.1f) { // Nur checken, wenn wir relativ still liegen
            stackCheckTimer = 0.0f;
            tryStacking(world);
        }
    }

    private void tryStacking(World world) {
        // Wenn wir schon voll sind, brauchen wir nicht suchen
        if (stack.amount >= stack.type.getMaxStackSize()) return;

        double mergeRadius = 1.2; // Wie nah Items beieinander liegen müssen

        for (Entity e : world.getEntities()) {
            if (e == this || e.isDead()) continue;

            if (e instanceof ItemEntity otherItem) {

                if (ItemRegistry.getId(this.stack.type).equals(ItemRegistry.getId(otherItem.stack.type))) {
                    double dist = this.position.distance(otherItem.position);
                    if (dist < mergeRadius) {

                        // Derjenige mit mehr Items (oder der Ältere) überlebt und zieht die Items an sich
                        boolean iShouldMerge = (this.stack.amount > otherItem.stack.amount) ||
                                (this.stack.amount == otherItem.stack.amount && this.age > otherItem.age);

                        if (iShouldMerge) {
                            int spaceLeft = this.stack.type.getMaxStackSize() - this.stack.amount;
                            int amountToTake = Math.min(spaceLeft, otherItem.stack.amount);

                            if (amountToTake > 0) {
                                this.stack.amount += amountToTake;
                                otherItem.stack.amount -= amountToTake;

                                // Kleine visuelle "Hüpf"-Animation beim Verschmelzen
                                this.velocity.y = 1.5f;
                                if (otherItem.stack.amount <= 0) {
                                    otherItem.stack.amount = 0;
                                    otherItem.setDead(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}