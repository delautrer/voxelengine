package de.delautrer.game.ui.gui.container;

import de.delautrer.game.items.ItemStack;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.ui.gui.ClickType;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseContainer {
    public final List<Slot> slots = new ArrayList<>();
    private ItemStack mouseStack = null;

    protected Slot addSlot(Slot slot) {
        slots.add(slot);
        return slot;
    }

    public ItemStack getMouseStack() { return mouseStack; }
    public void setMouseStack(ItemStack stack) { this.mouseStack = stack; }

    public void clickSlot(Slot slot, int button, ClickType clickType) {
        if (slot == null) return;
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
                } else if (clicked.type == mouseStack.type && clicked.amount < 64) {
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
                mouseStack = clicked;
            }
        } else {
            if (clicked == null) {
                slot.putStack(mouseStack);
                mouseStack = null;
            } else if (clicked.type == mouseStack.type) {
                int space = 64 - clicked.amount;
                int toAdd = Math.min(space, mouseStack.amount);
                clicked.amount += toAdd;
                mouseStack.amount -= toAdd;
                if (mouseStack.amount == 0) mouseStack = null;
            } else {
                slot.putStack(mouseStack);
                mouseStack = clicked;
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
                    int space = 64 - targetStack.amount;
                    if (space > 0) {
                        int toAdd = Math.min(space, stack.amount);
                        targetStack.amount += toAdd;
                        stack.amount -= toAdd;
                        if (stack.amount == 0) return;
                    }
                }
            }
        }

        // 2. Durchlauf: Wenn noch was übrig ist, suche komplett leere Slots
        for (Slot target : slots) {
            if (isTargetRegion(clickedSlot, target)) {
                if (target.getStack() == null) {
                    target.putStack(new ItemStack(stack.type, stack.amount));
                    clickedSlot.putStack(null);
                    return;
                }
            }
        }
    }

    // Hilfsmethode, um herauszufinden, ob ein Slot auf der "anderen Seite" liegt
    private boolean isTargetRegion(Slot clicked, Slot target) {
        if (clicked == target) return false;

        // Regel 1: Unterschiedliche Inventare (z.B. Kiste -> Spieler)
        if (clicked.inventory != target.inventory) return true;

        // Regel 2: Gleiches Inventar, aber wir wollen zwischen Hotbar und Main-Inv wechseln
        if (clicked.inventory instanceof PlayerInventory) {
            boolean clickedInHotbar = clicked.slotIndex < PlayerInventory.HOTBAR_SIZE;
            boolean targetInHotbar = target.slotIndex < PlayerInventory.HOTBAR_SIZE;
            return clickedInHotbar != targetInHotbar;
        }

        return false;
    }

    public void hotbarSwap(Slot hoveredSlot, int hotbarIndex) {
        if (hoveredSlot == null) return;

        Slot hotbarSlot = null;
        for (Slot s : slots) {
            if (s.inventory instanceof PlayerInventory && s.slotIndex == hotbarIndex) {
                hotbarSlot = s;
                break;
            }
        }

        if (hotbarSlot != null && hotbarSlot != hoveredSlot) {
            ItemStack temp = hoveredSlot.getStack();
            hoveredSlot.putStack(hotbarSlot.getStack());
            hotbarSlot.putStack(temp);
        }
    }

    public void sortRegion(Slot hoveredSlot) {
        if (hoveredSlot == null || hoveredSlot.inventory == null || !hoveredSlot.inventory.isSortable()) return;

        // 1. Finde alle Slots in diesem Container, die zur gleichen "Region" gehören
        List<Slot> regionSlots = new ArrayList<>();
        for (Slot s : slots) {
            if (s.inventory == hoveredSlot.inventory) {
                // Spezialfall Spieler-Inventar: Hotbar (0-8) und Main-Grid (9-35) trennen!
                if (hoveredSlot.inventory instanceof de.delautrer.game.inventory.PlayerInventory) {
                    boolean hoveredInHotbar = hoveredSlot.slotIndex < de.delautrer.game.inventory.PlayerInventory.HOTBAR_SIZE;
                    boolean sInHotbar = s.slotIndex < de.delautrer.game.inventory.PlayerInventory.HOTBAR_SIZE;
                    if (hoveredInHotbar == sInHotbar) {
                        regionSlots.add(s);
                    }
                } else {
                    // Normale Kisten/Inventare komplett nehmen
                    regionSlots.add(s);
                }
            }
        }

        // 2. Items sammeln und alphabetisch sortieren
        java.util.Map<de.delautrer.game.items.Item, Integer> counts = new java.util.TreeMap<>(
                java.util.Comparator.comparing(item -> {
                    String id = de.delautrer.game.items.ItemRegistry.getId(item);
                    return id != null ? id : "";
                })
        );

        for (Slot s : regionSlots) {
            ItemStack stack = s.getStack();
            if (stack != null && stack.amount > 0) {
                counts.put(stack.type, counts.getOrDefault(stack.type, 0) + stack.amount);
                s.putStack(null); // Slot leeren
            }
        }

        // 3. Sortierte Items wieder in die identifizierte Region einfügen
        int index = 0;
        for (java.util.Map.Entry<de.delautrer.game.items.Item, Integer> entry : counts.entrySet()) {
            int amount = entry.getValue();
            while (amount > 0 && index < regionSlots.size()) {
                int stackSize = Math.min(amount, 64);
                regionSlots.get(index++).putStack(new ItemStack(entry.getKey(), stackSize));
                amount -= stackSize;
            }
        }
    }

    public void gatherItems(Slot targetSlot) {
        if (mouseStack == null || mouseStack.amount >= 64) return;

        // Durchsuche alle Slots nach dem gleichen Item-Typ und sauge sie auf
        for (Slot s : slots) {
            if (s.inventory == null) continue; // Creative-Grid Slots ignorieren
            ItemStack stack = s.getStack();

            if (stack != null && stack.type == mouseStack.type && s != targetSlot) {
                int space = 64 - mouseStack.amount;
                if (space <= 0) break;

                int take = Math.min(space, stack.amount);
                mouseStack.amount += take;
                stack.amount -= take;

                if (stack.amount <= 0) s.putStack(null);
            }
        }
    }

    public void applyDrag(java.util.Set<Slot> draggedSlots) {
        if (mouseStack == null || draggedSlots.isEmpty()) return;

        // Wenn man nur geklickt hat ohne zu ziehen -> normaler Linksklick
        if (draggedSlots.size() == 1) {
            Slot slot = draggedSlots.iterator().next();
            clickSlot(slot, 0, ClickType.PICKUP);
            return;
        }

        // Filtere ungültige Slots (nur leere oder auffüllbare Slots mit demselben Typ)
        java.util.List<Slot> validSlots = new java.util.ArrayList<>();
        for (Slot s : draggedSlots) {
            if (s.inventory == null) continue; // Creative-Grid Slots ignorieren
            ItemStack st = s.getStack();
            if (st == null || (st.type == mouseStack.type && st.amount < 64)) {
                validSlots.add(s);
            }
        }

        if (validSlots.isEmpty()) return;

        // Gleichmäßig aufteilen
        int amountPerSlot = mouseStack.amount / validSlots.size();
        if (amountPerSlot == 0) return; // Zu wenig Items zum Verteilen

        for (Slot s : validSlots) {
            ItemStack st = s.getStack();
            if (st == null) {
                s.putStack(new ItemStack(mouseStack.type, amountPerSlot));
            } else {
                st.amount += amountPerSlot;
            }
            mouseStack.amount -= amountPerSlot;
        }

        if (mouseStack.amount <= 0) mouseStack = null;
    }
}