package de.delautrer.game.entity.player;

import de.delautrer.engine.audio.SoundManager;
import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.graphics.Camera;
import de.delautrer.engine.physics.Raycaster;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.IInteractable;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.events.BlockBreakEvent;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.loot.LootTable;
import de.delautrer.game.loot.LootTableManager;
import de.delautrer.game.world.World;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.BlockItem;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.game.events.HotbarSlotChangeEvent;
import de.delautrer.game.events.InventoryChangeEvent;
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

    private Vector3i currentlyMiningPos = null;
    private float miningProgress = 0.0f;
    private float miningSoundTimer = 0.0f;

    public PlayerInteraction(World world, Camera camera, LocalPlayer player, EventBus eventBus) {
        this.world = world;
        this.camera = camera;
        this.player = player;
        this.eventBus = eventBus;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public void update(InputManager input, float deltaTime) {
        if (player.isDead() || player.getInventory().isOpen() || player.isChatOpen()
                || player.getOpenedInventory() != null) {
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
            byte targetId = world.getBlockAt(selectedBlockPos);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack != null && stack.type instanceof BlockItem) {
                    if (((BlockItem) stack.type).block.getId() == targetId) {
                        player.getInventory().setSelectedSlot(i);
                        eventBus.publish(new HotbarSlotChangeEvent(i));
                        break;
                    }
                }
            }
        }

        // 3. Interaktion (Abbauen / Bauen)
        if (interactTimer > 0)
            interactTimer -= deltaTime;

        if (input.isActionActive("INTERACT_BREAK")) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                miningProgress = 0.0f;
                currentlyMiningPos = null;

                if (interactTimer <= 0) {
                    if (selectedBlockPos != null) {
                        // 1. Block-Info holen, SOLANGE ER NOCH DA IST
                        byte blockId = world.getBlockAt(selectedBlockPos);
                        Block targetBlock = BlockRegistry.get(blockId);

                    }

                    // 3. Erst jetzt den Block löschen
                    handleMouseClick(true);
                    interactTimer = INTERACT_COOLDOWN;
                }
            } else if (player.getGameMode() == GameMode.SURVIVAL) {
                // Im Survival Mode: Härte und Zeit berechnen
                if (selectedBlockPos != null) {
                    // Prüfen, ob wir immer noch denselben Block anschauen
                    if (currentlyMiningPos == null || !currentlyMiningPos.equals(selectedBlockPos)) {
                        currentlyMiningPos = new Vector3i(selectedBlockPos);
                        miningProgress = 0.0f; // Reset, wenn man wegschaut
                    }

                    byte blockId = world.getBlockAt(selectedBlockPos);
                    Block targetBlock = BlockRegistry.get(blockId);

                    if (targetBlock != Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air")
                            && targetBlock.getHardness() >= 0) {
                        miningProgress += deltaTime;

                        // Formel: Wie lange dauert der Abbau? (Base-Härte * 1.5 Sekunden als Richtwert)
                        // Später kannst du hier Werkzeuge einberechnen (z.B. miningProgress +=
                        // deltaTime * toolMultiplier)
                        float requiredTime = targetBlock.getHardness() * 1.5f;

                        // Abbau-Sound (periodisch)
                        miningSoundTimer += deltaTime;
                        if (miningSoundTimer >= 0.25f) {
                            SoundManager.playEvent(targetBlock.getSoundMaterialName(), "walk", 0.3f, 0.7f, 0.9f, "Player");
                            miningSoundTimer = 0.0f;
                        }

                        if (miningProgress >= requiredTime) {
                            handleSurvivalBreak(targetBlock, blockId);
                            miningProgress = 0.0f;
                            currentlyMiningPos = null;
                            interactTimer = INTERACT_COOLDOWN;
                        }
                    } else {
                        miningProgress = 0.0f; // Unzerstörbar (Bedrock) oder Luft
                    }
                } else {
                    miningProgress = 0.0f;
                    currentlyMiningPos = null;
                }
            }
        } else {
            // Maustaste losgelassen: Alles zurücksetzen
            miningProgress = 0.0f;
            currentlyMiningPos = null;

            // Platzieren-Logik (unverändert)
            if (input.isActionActive("INTERACT_PLACE") && interactTimer <= 0) {
                handleMouseClick(false);
                interactTimer = INTERACT_COOLDOWN;
            } else if (!input.isActionActive("INTERACT_PLACE")) {
                interactTimer = 0.0f;
            }
        }
    }

    private void handleSurvivalBreak(Block block, byte blockId) {
        if (selectedBlockPos == null)
            return;

        BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
        BlockBreakEvent breakEvent = new BlockBreakEvent(player, selectedBlockPos, state);

        eventBus.publish(breakEvent);

        if (!breakEvent.isCancelled()) {
            world.setBlock(selectedBlockPos, (byte) 0);

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

    private void handleMouseClick(boolean isBreak) {
        if (selectedBlockPos == null)
            return;

        if (isBreak) {
            BlockState state = world.getBlockState(selectedBlockPos.x, selectedBlockPos.y, selectedBlockPos.z);
            BlockBreakEvent breakEvent = new BlockBreakEvent(player, selectedBlockPos, state);

            eventBus.publish(breakEvent);

            if (!breakEvent.isCancelled()) {
                world.setBlock(selectedBlockPos, (byte) 0);
            }
        } else {
            if (adjacentBlockPos == null)
                return;

            if (!player.isSneaking) {
                Block clickedBlock = BlockRegistry.get(world.getBlockAt(selectedBlockPos));

                if (clickedBlock instanceof IInteractable interactable) {
                    boolean handled = interactable.onInteract(world, selectedBlockPos, player);
                    if (handled)
                        return;
                }
            }

            // --- 2. BLOCK ODER ITEM VERWENDEN ---
            ItemStack heldStack = player.getInventory().getSelectedHotbarStack();
            if (heldStack == null || heldStack.type == null)
                return;

            // HIER DIE ÄNDERUNG: Wir speichern das Ergebnis
            boolean success = heldStack.type.onUseRightClick(world, player, selectedBlockPos, adjacentBlockPos, this);

            // NEU: Item im Survival-Modus verbrauchen ODER Eimer tauschen
            if (success && player.getGameMode() == GameMode.SURVIVAL) {

                if (heldStack.type == Registries.ITEMS.get(Constants.NAMESPACE + ":" + "water_bucket")) {
                    // Voller Eimer wird ausgeleert -> Wir legen einen leeren Eimer in den Slot
                    player.getInventory().setStack(player.getInventory().getSelectedSlot(),
                            new ItemStack(Registries.ITEMS.get(Constants.NAMESPACE + ":" + "bucket"), 1));
                    eventBus.publish(new InventoryChangeEvent());

                } else if (heldStack.type == Registries.ITEMS.get(Constants.NAMESPACE + ":" + "bucket")) {
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
        }
    }

    public float getMiningProgressPercent() {
        if (currentlyMiningPos == null || selectedBlockPos == null || !currentlyMiningPos.equals(selectedBlockPos)) {
            return 0.0f;
        }

        byte blockId = world.getBlockAt(currentlyMiningPos);
        Block targetBlock = BlockRegistry.get(blockId);

        if (targetBlock == Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air") || targetBlock.getHardness() < 0) {
            return 0.0f;
        }

        float requiredTime = targetBlock.getHardness() * 1.5f;
        return Math.min(1.0f, miningProgress / requiredTime);
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

    public void resetCooldown() {
        this.clickCooldown = 0.5f;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
