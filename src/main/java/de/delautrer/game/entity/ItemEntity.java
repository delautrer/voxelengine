package de.delautrer.game.entity;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
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
        if (age >= 300.0f) {
            setDead(true);
            return;
        }

        if (pickupDelay > 0) pickupDelay -= deltaTime;

        // 1. SCHWERKRAFT & KOLLISION
        int bx = (int) Math.floor(position.x);
        int by = (int) Math.floor(position.y + 0.1f);
        int bz = (int) Math.floor(position.z);
        BlockState blockState = world.getBlockState(bx, by, bz);
        de.delautrer.game.blocks.Block blockIn = blockState.getBlock();
        
        if (blockIn instanceof de.delautrer.game.blocks.WaterBlock wb) {
            int level = blockState.getValue(de.delautrer.game.blocks.WaterBlock.LEVEL);
            if (level == 8) {
                // Source: Slowly rise
                velocity.y += 1.5f * deltaTime;
                if (velocity.y > 0.5f) velocity.y = 0.5f;
            } else {
                // Flowing: Wash away
                Vector3f flow = wb.getFlowDirection(world, bx, by, bz);
                float speed = (float)level / 7.0f * 6.0f; // More level = more push
                velocity.x += flow.x * speed * deltaTime;
                velocity.z += flow.z * speed * deltaTime;
                velocity.y += flow.y * speed * deltaTime;
                
                // Buoyancy in flowing water
                velocity.y += 3.5f * deltaTime;
            }
            // Friction in water
            velocity.mul(0.92f);
        } else {
            // Air: Gravity
            velocity.y -= 20.0f * deltaTime;
        }

        double nextX = position.x + velocity.x * deltaTime;
        double nextY = position.y + velocity.y * deltaTime;
        double nextZ = position.z + velocity.z * deltaTime;

        Chunk c = cm.getChunkAtBlock((int) Math.floor(nextX), (int) Math.floor(nextY), (int) Math.floor(nextZ));
        if (c != null) {
            byte blockBelowId = c.getBlock(Math.floorMod((int) Math.floor(nextX), Chunk.SIZE), (int) Math.floor(nextY), Math.floorMod((int) Math.floor(nextZ), Chunk.SIZE));
            de.delautrer.game.blocks.Block blockBelow = BlockRegistry.get(blockBelowId);
            
            if (blockBelow.isSolid) {
                // NEU: Lande-Sound abspielen, wenn wir von oben kommen
                if (velocity.y < -1.0f) {
                    de.delautrer.engine.audio.SoundManager.playEvent(blockBelow.getSoundMaterialName(), "jump_land", 0.15f, 1.4f, 1.6f, (float)nextX, (float)nextY, (float)nextZ);
                }
                nextY = Math.floor(nextY) + 1.001f;
                velocity.y = 0;
                velocity.x *= 0.5f;
                velocity.z *= 0.5f;
            }
        }
        position.set(nextX, nextY, nextZ);

        // Update light sampling for rendering
        int lx = (int) Math.floor(nextX);
        int ly = (int) Math.floor(nextY);
        int lz = (int) Math.floor(nextZ);
        Chunk lightChunk = cm.getChunkAtBlock(lx, ly, lz);
        if (lightChunk != null) {
            int lxLocal = Math.floorMod(lx, Chunk.SIZE);
            int lzLocal = Math.floorMod(lz, Chunk.SIZE);
            int rawSky = lightChunk.getSkyLightAt(lxLocal, ly, lzLocal, cm);
            int rawBlock = lightChunk.getBlockLightAt(lxLocal, ly, lzLocal, cm);
            // Convert to brightness using same curve as shader (pow(v, 1.8) done in shader, here just normalize)
            float targetSky = rawSky / 15.0f;
            float targetBlock = rawBlock / 15.0f;
            // Smooth the transition to avoid flickering
            skyLightBrightness   = skyLightBrightness   + (targetSky   - skyLightBrightness)   * 0.2f;
            blockLightBrightness = blockLightBrightness + (targetBlock - blockLightBrightness) * 0.2f;
        }

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