package de.delautrer.game.entity.player;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.IInteractable;
import de.delautrer.game.blocks.LogBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.events.BlockBreakEvent;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.World;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.items.ToolItem;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryChangeEvent;
import de.delautrer.game.events.PlayerItemDropEvent;
import de.delautrer.game.registry.Registries;

public class PlayerInteraction {



    private final World world;
    private final Camera camera;
    private final LocalPlayer player;
    private final EventBus eventBus;

    private Vector3i selectedBlockPos = null;
    private Vector3i adjacentBlockPos = null;

    private float interactTimer = 0.0f;
    private final float INTERACT_COOLDOWN = 0.2f;
    private float clickCooldown = 0.0f;

    private static class MiningState {
        float progress = 0.0f;
        float decayTimer = 0.0f;
        float progressSnapshot = 0.0f;
    }
    private final java.util.Map<Vector3i, MiningState> miningStates = new java.util.HashMap<>();
    private float miningSoundTimer = 0.0f;

    private static final float DECAY_START = 0.5f;
    private static final float DECAY_END   = 1.5f;

    // track held item to reset on hotbar switch
    private Item lastHeldItem = null;

    private float swingAnimationTimer = 0.0f;
    private static final float SWING_DURATION = 0.25f;

    public PlayerInteraction(World world, Camera camera, LocalPlayer player, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.eventBus = eventBus;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    private boolean wasInventoryOpen = false;

    public void update(InputManager input, float deltaTime) {
        if (swingAnimationTimer > 0) {
            swingAnimationTimer -= deltaTime;
        }

        boolean isInventoryOpen = player.getInventory().isOpen() || player.getOpenedInventory() != null;
        if (wasInventoryOpen && !isInventoryOpen) {
            // Inventory was just closed, trigger swing animation again
            swingAnimationTimer = SWING_DURATION;
        }
        wasInventoryOpen = isInventoryOpen;

        if (player.isDead() || isInventoryOpen || player.isChatOpen()) {
            selectedBlockPos = null;
            adjacentBlockPos = null;
            return;
        }

        if (clickCooldown > 0) {
            clickCooldown -= deltaTime;
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR)
            return;

        Block headBlock = player.getHeadBlock();

        // 1. Raycast (Kopf steckt fest oder normal)
        if (headBlock != Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air")) {
            org.joml.Vector3d eyePos = player.getEyePosition();

            selectedBlockPos = new org.joml.Vector3i(
                    (int) Math.floor(eyePos.x),
                    (int) Math.floor(eyePos.y),
                    (int) Math.floor(eyePos.z));

            // hitFace = BlockProperties.BlockFace.UP;
        } else {
            // 1.2 Sonst normaler raycast
            Raycaster.RaycastResult result = Raycaster.raycast(world, new Vector3f((float) camera.getPosition().x,
                    (float) camera.getPosition().y, (float) camera.getPosition().z), camera.getFront(), 6.0f);
            if (result != null) {
                selectedBlockPos = result.hitPos;
                adjacentBlockPos = result.adjacentPos;
            } else {
                selectedBlockPos = null;
                adjacentBlockPos = null;
            }
        }

        // if (!camera.isCursorCaptured()) return;

        // 2. Pick Block (Mittlere Maustaste)
        if (input.isActionJustPressed("PICK_BLOCK") && selectedBlockPos != null) {
            Block targetBlock = world.getBlock(selectedBlockPos);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack != null && stack.type instanceof BlockItem) {
                    if (((BlockItem) stack.type).block == targetBlock) {
                        player.getInventory().setSelectedSlot(i);
                        eventBus.publish(new HotbarSlotChangeEvent(i));
                        break;
                    }
                }
            }
        }

        // --- Hotbar change → hard reset ---
        ItemStack currentHeldStack = player.getInventory().getSelectedHotbarStack();
        Item currentHeldItem = (currentHeldStack != null) ? currentHeldStack.type : null;
        if (currentHeldItem != lastHeldItem) {
            lastHeldItem = currentHeldItem;
            miningStates.clear();
        }

        // 3. Interaktion (Abbauen / Bauen)
        if (interactTimer > 0)
            interactTimer -= deltaTime;
        
        if (input.isActionActive("INTERACT_BREAK")) {
            if (swingAnimationTimer <= 0) {
                swingAnimationTimer = SWING_DURATION;
            }
            if (player.getGameMode() == GameMode.CREATIVE) {
                miningStates.clear();

                if (interactTimer <= 0) {
                    handleMouseClick(true);
                    interactTimer = INTERACT_COOLDOWN;
                }
            } else if (player.getGameMode() == GameMode.SURVIVAL) {
                if (selectedBlockPos != null) {
                    Block targetBlock = world.getBlock(selectedBlockPos);

                    if (targetBlock != Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air")
                            && targetBlock.getHardness() >= 0) {

                        MiningState state = miningStates.computeIfAbsent(selectedBlockPos, k -> new MiningState());
                        state.decayTimer = 0.0f;

                        ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
                        ToolItem tool = (heldStack != null && heldStack.type instanceof ToolItem) ? (ToolItem) heldStack.type : null;

                        boolean isCorrectToolType = (tool != null) && tool.isCorrectToolFor(targetBlock);
                        boolean hasSufficientTier = false;
                        float toolEfficiency = 1.0f;

                        de.delautrer.game.items.ToolTier blockMinTier = targetBlock.getMinToolTier();

                        if (tool != null) {
                            de.delautrer.game.items.ToolTier toolTier = tool.getTier();
                            if (toolTier.getLevel() >= blockMinTier.getLevel()) hasSufficientTier = true;

                            if (isCorrectToolType && hasSufficientTier) {
                                toolEfficiency = tool.getEfficiency();
                            } else if (isCorrectToolType) {
                                toolEfficiency = tool.getEfficiency() * 0.3f;
                            }
                        }

                        boolean canHarvest = (blockMinTier == de.delautrer.game.items.ToolTier.HAND)
                                || (isCorrectToolType && hasSufficientTier);

                        // Time formula: hardness * 2.5 / efficiency  (correct tool)
                        //               hardness * 2.5               (hand on HAND-tier block)
                        //               hardness * 7.0               (wrong tier / wrong type)
                        float requiredTime;
                        if (canHarvest && tool != null && isCorrectToolType && hasSufficientTier) {
                            requiredTime = targetBlock.getHardness() * 2.5f / tool.getEfficiency();
                        } else if (blockMinTier == de.delautrer.game.items.ToolTier.HAND) {
                            if (tool != null && isCorrectToolType) {
                                requiredTime = targetBlock.getHardness() * 2.5f / toolEfficiency;
                            } else {
                                requiredTime = targetBlock.getHardness() * 2.5f;
                            }
                        } else {
                            requiredTime = targetBlock.getHardness() * 7.0f;
                        }

                        state.progress += deltaTime;

                        miningSoundTimer += deltaTime;
                        if (miningSoundTimer >= 0.25f) {
                            SoundManager.playEvent(targetBlock.getSoundMaterialName(), "walk", 0.3f, 0.7f, 0.9f, "Player");
                            miningSoundTimer = 0.0f;
                            
                            // PARTIKEL FÜR ABBAU-PROGRESS
                            de.delautrer.game.particle.ParticleSpawner.spawnBreaking(world, selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z, targetBlock);
                        }

                        if (state.progress >= requiredTime) {
                            handleSurvivalBreak(targetBlock, canHarvest);
                            miningStates.remove(selectedBlockPos);
                            interactTimer = INTERACT_COOLDOWN;
                        }
                    }
                }
            }
        }
        
        if (!input.isActionActive("INTERACT_BREAK")) {
            if (input.isActionActive("INTERACT_PLACE") && interactTimer <= 0) {
                boolean success = handleMouseClick(false);
                if (success) swingAnimationTimer = SWING_DURATION;
                interactTimer = INTERACT_COOLDOWN;
            } else if (!input.isActionActive("INTERACT_PLACE")) {
                interactTimer = 0.0f;
            }
        }

        // Apply decay to all blocks that are NOT currently being actively mined
        java.util.Iterator<java.util.Map.Entry<Vector3i, MiningState>> it = miningStates.entrySet().iterator();
        while(it.hasNext()) {
            java.util.Map.Entry<Vector3i, MiningState> entry = it.next();
            Vector3i pos = entry.getKey();
            MiningState state = entry.getValue();

            boolean activelyMiningThis = input.isActionActive("INTERACT_BREAK") && player.getGameMode() == GameMode.SURVIVAL && selectedBlockPos != null && pos.equals(selectedBlockPos);

            if (!activelyMiningThis) {
                if (state.decayTimer == 0.0f) {
                    state.progressSnapshot = state.progress;
                }
                state.decayTimer += deltaTime;
                if (state.decayTimer >= DECAY_START) {
                    float decayFraction = (state.decayTimer - DECAY_START) / (DECAY_END - DECAY_START);
                    state.progress = state.progressSnapshot * Math.max(0.0f, 1.0f - decayFraction);
                    if (state.decayTimer >= DECAY_END) {
                        it.remove();
                    }
                }
            }
        }
    }

    private void handleSurvivalBreak(Block block, boolean canHarvest) {
        if (selectedBlockPos == null)
            return;

        BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
        BlockBreakEvent breakEvent = new BlockBreakEvent(player, selectedBlockPos, state);

        eventBus.publish(breakEvent);

        if (!breakEvent.isCancelled()) {
            Block airBlock = de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
            world.setBlock(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z, airBlock);

            // Werkzeug-Haltbarkeit reduzieren
            ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
            if (heldStack != null && heldStack.type instanceof ToolItem) {
                heldStack.damage(1);
                if (heldStack.durability <= 0) {
                    player.getInventory().setStack(player.getInventory().getSelectedSlot(), null);
                    eventBus.publish(new InventoryChangeEvent());
                    SoundManager.playEvent("metal", "break", 1.0f, 1.0f, 1.0f, "Player");
                } else {
                    eventBus.publish(new InventoryChangeEvent());
                }
            }

            // Drops nur erzeugen, wenn mit dem richtigen Werkzeug und Tier abgebaut wurde
            if (!canHarvest) return;

            String lootPath = block.getLootTable();
            List<ItemStack> drops = new ArrayList<>();

            if (lootPath != null) {
                // 1. DATA-DRIVEN LOOT: Wir laden die Drops aus der JSON
                LootTable table = LootTableManager.load(lootPath);
                if (table != null) {
                    drops.addAll(table.generateLoot());
                }
            } else {
                // Kein LootTable definiert — Block droppt nichts.
            }

            // NEU: Wenn ein Baum-Stamm an seiner Basis (auf Erde/Gras/Sand) abgebaut wird, droppe 4 Saplings
            if (block instanceof LogBlock) {
                NamespacedKey logKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(block);
                if (logKey != null) {
                    String logName = logKey.getKey(); // z.B. "oak_log"
                    String baseName = logName.replace("_log", "");
                    
                    // Check if block below is soil
                    Block belowBlock = world.getBlock(selectedBlockPos.x, selectedBlockPos.y - 1, selectedBlockPos.z);
                    NamespacedKey belowKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(belowBlock);
                    if (belowKey != null) {
                        String belowName = belowKey.getKey();
                        boolean isSoil = belowName.equals("grass_block") || belowName.equals("dirt") || belowName.equals("sand") || belowName.equals("sandy_grass");
                        if (isSoil) {
                            // Check if there are leaves of the same tree type nearby (radius 6)
                            boolean leavesNearby = false;
                            String leavesName = baseName + "_leaves";
                            for (int dx = -6; dx <= 6 && !leavesNearby; dx++) {
                                for (int dy = -6; dy <= 6 && !leavesNearby; dy++) {
                                    for (int dz = -6; dz <= 6 && !leavesNearby; dz++) {
                                        Block nearbyBlock = world.getBlock(selectedBlockPos.x + dx, selectedBlockPos.y + dy, selectedBlockPos.z + dz);
                                        NamespacedKey nearbyKey = de.delautrer.game.registry.Registries.BLOCKS.getKey(nearbyBlock);
                                        if (nearbyKey != null && nearbyKey.getKey().equals(leavesName)) {
                                            leavesNearby = true;
                                        }
                                    }
                                }
                            }
                            
                            if (leavesNearby) {
                                Item saplingItem = ItemRegistry.get(Constants.NAMESPACE + ":" + baseName + "_sapling");
                                if (saplingItem != null) {
                                    drops.add(new ItemStack(saplingItem, 4));
                                }
                            }
                        }
                    }
                }
            }

            // 3. Spawne alle berechneten Items in der Welt
            for (ItemStack stack : drops) {
                Vector3d dropPos = new Vector3d(
                        selectedBlockPos.x + 0.5,
                        selectedBlockPos.y + 0.5,
                        selectedBlockPos.z + 0.5);

                Vector3f dropVel = new Vector3f(
                        (float) (Math.random() - 0.5) * 2.0f,
                        2.0f,
                        (float) (Math.random() - 0.5) * 2.0f);

                ItemEntity entity = new ItemEntity(stack, dropPos, dropVel);
                world.spawnEntity(entity);
            }
        }
    }

    private boolean handleMouseClick(boolean isBreak) {
        if (selectedBlockPos == null)
            return false;

        if (isBreak) {
            BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
            BlockBreakEvent breakEvent = new BlockBreakEvent(player, selectedBlockPos, state);

            eventBus.publish(breakEvent);

            if (!breakEvent.isCancelled()) {
                Block airBlock = de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
                world.setBlock(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z, airBlock);
                return true;
            }
            return false;
        } else {
            if (adjacentBlockPos == null)
                return false;

            if (!player.isSneaking) {
                Block clickedBlock = world.getBlock(selectedBlockPos);

                if (clickedBlock instanceof IInteractable interactable) {
                    boolean handled = interactable.onInteract(world, selectedBlockPos, player);
                    if (handled)
                        return true;
                }
            }

            // --- 2. BLOCK ODER ITEM VERWENDEN ---
            ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
            if (heldStack == null || heldStack.type == null)
                return false;

            // HIER DIE ÄNDERUNG: Wir speichern das Ergebnis
            boolean success = heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);

            // NEU: Item im Survival-Modus verbrauchen ODER Eimer tauschen
            if (success && player.getGameMode() == GameMode.SURVIVAL) {

                if (heldStack.type == Registries.ITEMS.get(Constants.NAMESPACE + ":" + "water_bucket")) {
                    // Voller Eimer wird ausgeleert -> Wir legen einen leeren Eimer in den Slot
                    player.getInventory().setStack(player.getInventory().getSelectedSlot(),
                            new ItemStack(Registries.ITEMS.get(Constants.NAMESPACE + ":" + "empty_bucket"), 1));
                    eventBus.publish(new InventoryChangeEvent());

                } else if (heldStack.type == Registries.ITEMS.get(Constants.NAMESPACE + ":" + "empty_bucket")) {
                    // Leerer Eimer wurde gefüllt -> Wir legen einen Wassereimer in den Slot
                    player.getInventory().setStack(player.getInventory().getSelectedSlot(),
                            new ItemStack(Registries.ITEMS.get(Constants.NAMESPACE + ":" + "water_bucket"), 1));
                    eventBus.publish(new InventoryChangeEvent());

                } else {
                    heldStack.amount -= 1;

                    if (heldStack.amount <= 0) {
                        player.getInventory().setStack(player.getInventory().getSelectedSlot(), null);
                    }
                    eventBus.publish(new InventoryChangeEvent());
                }
            }
            return success;
        }
    }

    public java.util.Map<Vector3i, Float> getAllMiningProgresses() {
        java.util.Map<Vector3i, Float> progresses = new java.util.HashMap<>();
        for (java.util.Map.Entry<Vector3i, MiningState> entry : miningStates.entrySet()) {
            Vector3i pos = entry.getKey();
            float progress = calculateProgressPercent(pos, entry.getValue().progress);
            if (progress > 0.0f) {
                progresses.put(pos, progress);
            }
        }
        return progresses;
    }

    public float getMiningProgressPercent() {
        if (selectedBlockPos == null) return 0.0f;
        MiningState state = miningStates.get(selectedBlockPos);
        if (state == null) return 0.0f;
        return calculateProgressPercent(selectedBlockPos, state.progress);
    }

    private float calculateProgressPercent(Vector3i pos, float currentProgress) {
        Block targetBlock = world.getBlock(pos);
        if (targetBlock == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air") || targetBlock.getHardness() < 0) {
            return 0.0f;
        }

        ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
        ToolItem tool = (heldStack != null && heldStack.type instanceof ToolItem) ? (ToolItem) heldStack.type : null;

        boolean isCorrectToolType = (tool != null) && tool.isCorrectToolFor(targetBlock);
        boolean hasSufficientTier = false;
        float toolEfficiency = 1.0f;

        de.delautrer.game.items.ToolTier blockMinTier = targetBlock.getMinToolTier();
        if (tool != null) {
            de.delautrer.game.items.ToolTier toolTier = tool.getTier();
            if (toolTier.getLevel() >= blockMinTier.getLevel()) hasSufficientTier = true;
            if (isCorrectToolType && hasSufficientTier) toolEfficiency = tool.getEfficiency();
            else if (isCorrectToolType) toolEfficiency = tool.getEfficiency() * 0.3f;
        }

        boolean canHarvest = (blockMinTier == de.delautrer.game.items.ToolTier.HAND) || (isCorrectToolType && hasSufficientTier);
        float requiredTime;
        if (canHarvest && tool != null && isCorrectToolType && hasSufficientTier) {
            requiredTime = targetBlock.getHardness() * 2.5f / tool.getEfficiency();
        } else if (blockMinTier == de.delautrer.game.items.ToolTier.HAND) {
            if (tool != null && isCorrectToolType) requiredTime = targetBlock.getHardness() * 2.5f / toolEfficiency;
            else requiredTime = targetBlock.getHardness() * 2.5f;
        } else {
            requiredTime = targetBlock.getHardness() * 7.0f;
        }

        return Math.min(1.0f, currentProgress / requiredTime);
    }
    
    public float getSwingProgress() {
        if (swingAnimationTimer <= 0) return 0.0f;
        return 1.0f - (swingAnimationTimer / SWING_DURATION);
    }

    public Vector3i getSelectedBlockPos() {
        return selectedBlockPos;
    }

    public Vector3i getAdjacentBlockPos() {
        return adjacentBlockPos;
    }

    public PlayerInventory getInventory() {
        return player.getInventory();
    }

    public void dropStack(ItemStack stack) {
        if (stack == null || stack.amount <= 0) return;

        swingAnimationTimer = SWING_DURATION;

        Vector3d spawnPos = new Vector3d(player.position).add(0, 1.5, 0);
        Vector3f lookDir = new Vector3f(player.getCamera().getFront());
        Vector3f throwVelocity = new Vector3f(lookDir).mul(5.0f).add(0, 1.5f, 0);

        ItemEntity itemEntity = new ItemEntity(stack, spawnPos, throwVelocity);
        world.spawnEntity(itemEntity);
        eventBus.publish(new PlayerItemDropEvent(player, stack));
    }

    public void dropFromSlot(int slotIndex, boolean fullStack) {
        ItemStack currentStack = player.getInventory().getStack(slotIndex);
        if (currentStack == null) return;

        int amount = fullStack ? currentStack.amount : 1;
        ItemStack dropStack = new ItemStack(currentStack.type, amount);
        dropStack.durability = currentStack.durability;

        currentStack.amount -= amount;
        if (currentStack.amount <= 0) {
            player.getInventory().setStack(slotIndex, null);
        }

        eventBus.publish(new InventoryChangeEvent());
        dropStack(dropStack);
    }



    public void resetCooldown() {
        this.clickCooldown = 0.5f;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
