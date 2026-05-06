package de.delautrer.game.world.systems;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.events.PlayerItemPickupEvent;
import de.delautrer.game.events.InventoryChangeEvent;

import java.util.ArrayList;
import java.util.List;

public class EntitySystem implements WorldSystem {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> entitiesToAdd = new ArrayList<>();
    private final List<Entity> entitiesToRemove = new ArrayList<>();

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        ChunkManager chunkManager = world.getChunkManager();

        // --- ENTITIES SYNCHRONISIEREN (Neue hinzufügen, alte löschen) ---
        if (!entitiesToAdd.isEmpty()) {
            entities.addAll(entitiesToAdd);
            entitiesToAdd.clear();
        }
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove);
            entitiesToRemove.clear();
        }

        // --- ENTITIES UPDATEN ---
        for (Entity entity : entities) {
            if (entity.isDead()) {
                entitiesToRemove.add(entity);
                continue;
            }

            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.update(deltaTime, chunkManager, world);

                // Item aufsammeln
                if (itemEntity.pickupDelay <= 0) {
                    double dist = localPlayer.position.distance(itemEntity.position);
                    if (dist < 1.5) {
                        PlayerItemPickupEvent event = new PlayerItemPickupEvent(localPlayer, itemEntity.stack);
                        world.getEventBus().publish(event);

                        if (!event.isCancelled()) {
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
            } else {
                entity.update(deltaTime, chunkManager);
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
