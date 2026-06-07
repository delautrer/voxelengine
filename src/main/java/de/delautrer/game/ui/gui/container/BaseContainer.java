package de.delautrer.game.ui.gui.container;

import de.delautrer.game.items.ItemStack;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.ui.gui.ClickType;
import java.util.ArrayList;
import java.util.List;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;

public abstract class BaseContainer {
    public final List<Slot> slots = new ArrayList<>();
    private ItemStack mouseStack = null;

    protected Slot addSlot(Slot slot) {
        slots.add(slot);
        return slot;
    }

    public void onContainerClosed() {
        ItemStack mouse = getMouseStack();
        if (mouse != null) {
            giveItemOrDrop(mouse);
            setMouseStack(null);
        }
    }

    protected void giveItemOrDrop(ItemStack stack) {
        pushToPlayerInventory(stack);

        if (stack.amount > 0) {
            System.out.println("Inventar voll! Droppe Item in die Welt: " + stack.amount + "x " + stack.type);
        }
    }

    public ItemStack getMouseStack() { return mouseStack; }
    public void setMouseStack(ItemStack stack) { this.mouseStack = stack; }

    public void clickSlot(Slot slot, int button, ClickType clickType) {
        if (slot == null) return;

        if (clickType == ClickType.QUICK_MOVE) {
            if (slot instanceof CraftingResultSlot) {
                shiftClickCrafting((CraftingResultSlot) slot);
                return;
            }
            ItemStack clicked = slot.getStack();
            if (clicked != null) quickMove(slot);
            return;
        }

        ItemStack clicked = slot.getStack();

        // --- SHIFT-KLICK (Quick Move) ---
        if (clickType == ClickType.QUICK_MOVE) {
            if (clicked != null) quickMove(slot);
            return;
        }

        // --- RECHTSKLICK (Split / Einzeln platzieren) ---
        if (clickType == ClickType.SPLIT) {
            if (mouseStack == null) {
                if (clicked != null) {
                    // Nimm die Hälfte (bei ungeraden Zahlen wird aufgerundet)
                    int take = (int) Math.ceil(clicked.amount / 2.0f);
                    int kept = clicked.amount - take;
                    mouseStack = new ItemStack(clicked.type, take);

                    if (kept == 0) slot.putStack(null);
                    else clicked.amount = kept;
                }
            } else {
                if (clicked == null) {
                    // Platziere exakt 1 Item
                    slot.putStack(new ItemStack(mouseStack.type, 1));
                    mouseStack.amount--;
                    if (mouseStack.amount <= 0) mouseStack = null;
                } else if (clicked.type == mouseStack.type && clicked.amount < clicked.type.getMaxStackSize()) {
                    // Füge exakt 1 Item zum Stack hinzu
                    clicked.amount++;
                    mouseStack.amount--;
                    if (mouseStack.amount <= 0) mouseStack = null;
                } else if (clicked.type != mouseStack.type) {
                    // Typen unterschiedlich -> Tauschen
                    slot.putStack(mouseStack);
                    mouseStack = clicked;
                }
            }
            return;
        }

        // --- LINKSKLICK (Normal Pick/Swap) ---
        if (mouseStack == null) {
            if (clicked != null) {
                slot.putStack(null);
                slot.onTake();
                mouseStack = clicked;
            }
        } else {
            if (clicked == null) {
                if (slot.isItemValid(mouseStack)) {
                    slot.putStack(mouseStack);
                    mouseStack = null;
                }
            } else if (clicked.type == mouseStack.type) {
                if (slot instanceof CraftingResultSlot) {
                    // NEU: Mehrmaliges Klicken summiert das Item auf der Hand!
                    int space = mouseStack.type.getMaxStackSize() - mouseStack.amount;
                    if (space >= clicked.amount) {
                        mouseStack.amount += clicked.amount;
                        slot.onTake();
                    }
                } else if (slot.isItemValid(mouseStack)) {
                    int space = clicked.type.getMaxStackSize() - clicked.amount;
                    int toAdd = Math.min(space, mouseStack.amount);
                    clicked.amount += toAdd;
                    mouseStack.amount -= toAdd;
                    if (mouseStack.amount == 0) mouseStack = null;
                    slot.onSlotChanged();
                }
            } else {
                if (slot.isItemValid(mouseStack)) {
                    slot.putStack(mouseStack);
                    slot.onTake();
                    mouseStack = clicked;
                }
            }
        }
    }

    // Automatische Logik, wohin Items per Shift-Klick fliegen sollen
    protected void quickMove(Slot clickedSlot) {
        ItemStack stack = clickedSlot.getStack();
        if (stack == null) return;

        // 1. Durchlauf: Versuche, existierende Stacks desselben Typs aufzufüllen
        for (Slot target : slots) {
            if (isTargetRegion(clickedSlot, target)) {
                ItemStack targetStack = target.getStack();
                if (targetStack != null && targetStack.type == stack.type) {
                    int space = targetStack.type.getMaxStackSize() - targetStack.amount;
                    if (space > 0) {
                        int toAdd = Math.min(space, stack.amount);
                        targetStack.amount += toAdd;
                        stack.amount -= toAdd;
                        target.onSlotChanged();

                        if (stack.amount <= 0) {
                            clickedSlot.putStack(null);
                            return;
                        }
                    }
                }
            }
        }

        // 2. Durchlauf: Wenn noch was übrig ist, suche komplett leere Slots
        for (Slot target : slots) {
            if (isTargetRegion(clickedSlot, target) && target.isItemValid(stack)) {
                if (target.getStack() == null) {
                    target.putStack(new ItemStack(stack.type, stack.amount));
                    clickedSlot.putStack(null);
                    return;
                }
            }
        }
    }

    private boolean isTargetRegion(Slot clicked, Slot target) {
        if (clicked == target) return false;
        if (clicked.inventory != target.inventory) return true;
        if (clicked.inventory instanceof PlayerInventory) {
            boolean clickedInHotbar = clicked.slotIndex < PlayerInventory.HOTBAR_SIZE;
            boolean targetInHotbar = target.slotIndex < PlayerInventory.HOTBAR_SIZE;
            return clickedInHotbar != targetInHotbar;
        }
        return false;
    }

    public void hotbarSwap(Slot hoveredSlot, int hotbarIndex) {
        if (hoveredSlot == null) return;
        
        // Prevent hotkey interaction with CraftingResultSlot completely to avoid bypassing ingredient consumption
        if (hoveredSlot instanceof CraftingResultSlot) return;

        Slot hotbarSlot = null;
        for (Slot s : slots) {
            if (s.inventory instanceof PlayerInventory && s.slotIndex == hotbarIndex) {
                hotbarSlot = s;
                break;
            }
        }

        if (hotbarSlot != null && hotbarSlot != hoveredSlot) {
            ItemStack temp = hoveredSlot.getStack();
            ItemStack hotbarItem = hotbarSlot.getStack();
            
            // Check if the items are allowed in their new slots
            if (hotbarItem != null && !hoveredSlot.isItemValid(hotbarItem)) return;
            if (temp != null && !hotbarSlot.isItemValid(temp)) return;

            hoveredSlot.putStack(hotbarItem);
            hotbarSlot.putStack(temp);
        }
    }

    public void sortRegion(Slot hoveredSlot) {
        if (hoveredSlot == null || hoveredSlot.inventory == null || !hoveredSlot.inventory.isSortable()) return;

        List<Slot> regionSlots = new ArrayList<>();
        for (Slot s : slots) {
            if (s.inventory == hoveredSlot.inventory) {
                if (hoveredSlot.inventory instanceof PlayerInventory) {
                    boolean hoveredInHotbar = hoveredSlot.slotIndex < PlayerInventory.HOTBAR_SIZE;
                    boolean sInHotbar = s.slotIndex < PlayerInventory.HOTBAR_SIZE;
                    if (hoveredInHotbar == sInHotbar) regionSlots.add(s);
                } else {
                    regionSlots.add(s);
                }
            }
        }

        java.util.Map<Item, Integer> counts = new java.util.TreeMap<>(
                java.util.Comparator.comparing(item -> {
                    String id = ItemRegistry.getId(item);
                    return id != null ? id : "";
                })
        );

        for (Slot s : regionSlots) {
            ItemStack stack = s.getStack();
            if (stack != null && stack.amount > 0) {
                counts.put(stack.type, counts.getOrDefault(stack.type, 0) + stack.amount);
                s.putStack(null);
            }
        }

        int index = 0;
        for (java.util.Map.Entry<Item, Integer> entry : counts.entrySet()) {
            int amount = entry.getValue();
            while (amount > 0 && index < regionSlots.size()) {
                int stackSize = Math.min(amount, entry.getKey().getMaxStackSize());
                regionSlots.get(index++).putStack(new ItemStack(entry.getKey(), stackSize));
                amount -= stackSize;
            }
        }
    }

    public void gatherItems(Slot targetSlot) {
        if( targetSlot instanceof CraftingResultSlot) {
            craftMaxToCursor((CraftingResultSlot) targetSlot);
            return;
        }
        if (mouseStack == null || mouseStack.amount >= mouseStack.type.getMaxStackSize()) return;

        for (Slot s : slots) {
            if (s.inventory == null || s instanceof CraftingResultSlot) continue;
            ItemStack stack = s.getStack();

            if (stack != null && stack.type == mouseStack.type && s != targetSlot) {
                int space = mouseStack.type.getMaxStackSize() - mouseStack.amount;
                if (space <= 0) break;

                int take = Math.min(space, stack.amount);
                mouseStack.amount += take;
                stack.amount -= take;

                if (stack.amount <= 0) s.putStack(null);
            }
        }
    }

    public void applyDrag(java.util.Set<Slot> draggedSlots, int dragButton) {
        if (mouseStack == null || draggedSlots.isEmpty()) return;

        if (draggedSlots.size() == 1) {
            Slot slot = draggedSlots.iterator().next();
            clickSlot(slot, dragButton, dragButton == 1 ? ClickType.SPLIT : ClickType.PICKUP);
            return;
        }

        java.util.List<Slot> validSlots = new java.util.ArrayList<>();
        for (Slot s : draggedSlots) {
            if (s.inventory == null) continue;
            if (!s.isItemValid(mouseStack)) continue; // NEU: Check ob Item erlaubt ist

            ItemStack st = s.getStack();
            if (st == null || (st.type == mouseStack.type && st.amount < st.type.getMaxStackSize())) {
                validSlots.add(s);
            }
        }

        if (validSlots.isEmpty()) return;

        int amountPerSlot;
        if (dragButton == 1) {
            amountPerSlot = 1;
            while (validSlots.size() > mouseStack.amount) {
                validSlots.remove(validSlots.size() - 1);
            }
        } else {
            amountPerSlot = mouseStack.amount / validSlots.size();
        }
        
        if (amountPerSlot == 0) return;

        for (Slot s : validSlots) {
            ItemStack st = s.getStack();
            if (st == null) {
                s.putStack(new ItemStack(mouseStack.type, amountPerSlot));
                mouseStack.amount -= amountPerSlot;
            } else {
                int space = st.type.getMaxStackSize() - st.amount;
                int toAdd = Math.min(space, amountPerSlot);
                st.amount += toAdd;
                mouseStack.amount -= toAdd;
            }
        }
        if (mouseStack.amount <= 0) mouseStack = null;
    }

    protected void shiftClickCrafting(CraftingResultSlot slot) {
        ItemStack result = slot.getStack();
        while (result != null) {
            ItemStack copy = new ItemStack(result.type, result.amount);
            int initialAmount = copy.amount;

            pushToPlayerInventory(copy); // Versucht, die Items ins Inventar zu schieben

            if (copy.amount < initialAmount) {
                // Wir haben erfolgreich gecraftet (ganz oder teilweise)
                slot.onTake();

                if (copy.amount > 0) {
                    // Rest, der nicht mehr ins Inventar gepasst hat -> wandert an die Maus
                    if (mouseStack == null) {
                        mouseStack = copy;
                    }
                    break; // Inventar offensichtlich voll, stoppe Shift-Crafting
                }
                result = slot.getStack(); // Neues Item für nächsten Durchlauf laden
            } else {
                break; // Absolut kein Platz mehr im Inventar
            }
        }
    }

    protected void craftMaxToCursor(CraftingResultSlot slot) {
        if (mouseStack != null && mouseStack.amount >= mouseStack.type.getMaxStackSize()) return;

        ItemStack result = slot.getStack();
        while (result != null) {
            if (mouseStack == null) {
                mouseStack = new ItemStack(result.type, 0);
            } else if (mouseStack.type != result.type) {
                break; // Falsches Item auf der Hand
            }

            int space = mouseStack.type.getMaxStackSize() - mouseStack.amount;
            if (space >= result.amount) {
                mouseStack.amount += result.amount;
                slot.onTake();
                result = slot.getStack(); // Update für nächsten Loop
            } else {
                break; // Nicht genug Platz auf der Hand für ein weiteres ganzes Resultat
            }
        }
    }

    protected boolean pushToPlayerInventory(ItemStack stack) {
        boolean success = false;
        // 1. In existierende Stacks füllen
        for (Slot s : slots) {
            if (s.inventory instanceof PlayerInventory) {
                ItemStack target = s.getStack();
                if (target != null && target.type == stack.type) {
                    int space = target.type.getMaxStackSize() - target.amount;
                    if (space > 0) {
                        int toAdd = Math.min(space, stack.amount);
                        target.amount += toAdd;
                        stack.amount -= toAdd;
                        s.onSlotChanged();
                        success = true;
                        if (stack.amount == 0) return true;
                    }
                }
            }
        }
        // 2. In leere Slots füllen
        for (Slot s : slots) {
            if (s.inventory instanceof PlayerInventory) {
                if (s.getStack() == null) {
                    s.putStack(new ItemStack(stack.type, stack.amount));
                    stack.amount = 0;
                    s.onSlotChanged();
                    return true;
                }
            }
        }
        return success;
    }
}
