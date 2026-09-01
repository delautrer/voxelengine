package de.delautrer.game.world.systems;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.events.PlayerItemPickupEvent;
import de.delautrer.game.events.InventoryChangeEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;

public class EntitySystem implements WorldSystem {

    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Entity> entitiesToAdd = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Entity> entitiesToRemove = new ConcurrentLinkedQueue<>();

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        ChunkManager chunkManager = world.getChunkManager();

        // --- ENTITIES SYNCHRONISIEREN (Neue hinzufügen, alte löschen) ---
        Entity toAdd;
        while ((toAdd = entitiesToAdd.poll()) != null) {
            entities.add(toAdd);
        }
        
        Entity toRemove;
        while ((toRemove = entitiesToRemove.poll()) != null) {
            entities.remove(toRemove);
        }

        // --- ENTITIES UPDATEN ---
        for (Entity entity : entities) {
            if (entity.isDead()) {
                entitiesToRemove.add(entity);
                continue;
            }

            if (entity.position.y < Chunk.MIN_Y - 10) {
                entity.setDead(true);
                entitiesToRemove.add(entity);
                continue;
            }

            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.update(deltaTime, chunkManager, world);

                // Item Magnet-Effekt & Aufsammeln
                if (itemEntity.pickupDelay <= 0) {
                    double dx = localPlayer.position.x - itemEntity.position.x;
                    double dy = (localPlayer.position.y + 0.8) - itemEntity.position.y; // Ziel: Brusthöhe
                    double dz = localPlayer.position.z - itemEntity.position.z;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    // 1. MAGNET (Zwei Phasen: Driften & Hochgeschwindigkeits-Staubsauger)
                    if (distSq < 8.0 * 8.0) {
                        double dist = Math.sqrt(distSq);
                        float speed;
                        float damping;

                        if (dist < 3.0) {
                            // INNERER RING: "Staubsauger" (Jetzt deutlich schneller!)
                            speed = 65.0f * deltaTime;
                            damping = 0.85f; 
                        } else {
                            // ÄUSSERER RING: Schnellerer Drift
                            speed = 10.0f * deltaTime;
                            damping = 0.96f; 
                        }

                        itemEntity.velocity.x += (float) (dx / dist * speed);
                        itemEntity.velocity.y += (float) (dy / dist * speed);
                        itemEntity.velocity.z += (float) (dz / dist * speed);

                        itemEntity.velocity.mul(damping);
                    }

                    // 2. AUFSAMMELN (Radius vergrößert für sofortiges Pickup beim Ankommen)
                    if (distSq < 1.2 * 1.2) {
                        PlayerItemPickupEvent event = new PlayerItemPickupEvent(localPlayer, itemEntity.stack);
                        world.getEventBus().publish(event);

                        if (!event.isCancelled()) {
                            // Sound abspielen (vom Block unter dem Spieler)
                            int bx = (int) Math.floor(localPlayer.position.x);
                            int by = (int) Math.floor(localPlayer.position.y - 0.1);
                            int bz = (int) Math.floor(localPlayer.position.z);
                            de.delautrer.game.blocks.Block groundBlock = world.getBlock(bx, by, bz);
                            String mat = groundBlock != null ? groundBlock.getSoundMaterialName() : null;
                            // de.delautrer.engine.audio.SoundManager.playEvent(mat, "jump_start", 0.15f, 1.3f, 1.6f);

                            int leftover = localPlayer.getInventory().addItem(itemEntity.stack);
                            world.getEventBus().publish(new InventoryChangeEvent());

                            if (leftover == 0) {
                                itemEntity.setDead(true);
                            } else {
                                itemEntity.stack.amount = leftover;
                            }
                        }
                    }
                }
            } else if (entity instanceof de.delautrer.game.entity.FallingBlockEntity fallingBlock) {
                fallingBlock.update(deltaTime, chunkManager, world);
            } else {
                entity.update(deltaTime, chunkManager);
            }
        }
    }

    @Override
    public void onTick(World world, LocalPlayer localPlayer) {
        localPlayer.onTick(world);
        
        for (Entity entity : entities) {
            if (!entity.isDead()) {
                entity.onTick(world);
            }
        }
    }

    public void spawnEntity(Entity entity) {
        entitiesToAdd.add(entity);
    }

    public void removeEntity(Entity entity) {
        entitiesToRemove.add(entity);
    }

    public List<Entity> getEntities() {
        return entities;
    }
}
