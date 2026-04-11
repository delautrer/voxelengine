package de.delautrer.game.player;

import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int INV_SIZE = 27;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + INV_SIZE;

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];
    private ItemStack mouseStack = null; // Das Item, das an der Maus "klebt"
    private int selectedHotbarSlot = 0;
    private boolean isOpen = false;

    public Inventory() {
        ItemRegistry.init();

        // Wir füllen das Inventar zum Testen mit allen existierenden Items
        int i = 0;
        for (de.delautrer.game.items.ItemType item : ItemRegistry.getAll().values()) {
            slots[i++] = new ItemStack(item, 64);
            if (i >= TOTAL_SIZE) break;
        }
    }

    // Die geniale Drag'n'Drop Logik
    public void handleSlotClick(int slotIndex) {
        ItemStack clicked = slots[slotIndex];

        if (mouseStack == null) {
            // Nichts an der Maus -> Item aus dem Slot aufheben
            slots[slotIndex] = null;
            mouseStack = clicked;
        } else {
            if (clicked == null) {
                // Slot ist leer -> Maus-Item ablegen
                slots[slotIndex] = mouseStack;
                mouseStack = null;
            } else if (clicked.type == mouseStack.type) {
                // Gleiches Item -> Stacken
                clicked.amount += mouseStack.amount;
                mouseStack = null;
            } else {
                // Unterschiedliche Items -> Plätze tauschen
                slots[slotIndex] = mouseStack;
                mouseStack = clicked;
            }
        }
    }

    // Wandelt Pixel-Mauskoordinaten in eine Slot-ID (0-35) um
    public int getClickedSlot(float mouseX, float mouseY, int screenW, int screenH) {
        float pixelScale = 2.0f;
        if (screenH >= 1080) pixelScale = 3.0f;
        if (screenH >= 1440) pixelScale = 4.0f;

        // WICHTIG: Maus umdrehen! 0 ist jetzt Unten, screenH ist Oben!
        float invertedMouseY = screenH - mouseY;

        float hotbarWidth = 207.0f * pixelScale;
        float hotbarHeight = 23.0f * pixelScale;
        float hx = (float) Math.floor((screenW - hotbarWidth) / 2.0f);

        // Exakt wie im UIRenderer: 10 Pixel vom Boden entfernt
        float hotbarY = (float) Math.floor(10.0f * pixelScale);

        float slotHitboxSize = 22.0f * pixelScale;

        // 1. Hotbar (Slots 0-8)
        if (invertedMouseY >= hotbarY && invertedMouseY <= hotbarY + hotbarHeight) {
            for (int col = 0; col < 9; col++) {
                float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) return col;
            }
        }

        // 2. Inventar (Slots 9-35)
        if (isOpen) {
            float invY = (float) Math.floor((screenH - (3 * hotbarHeight)) / 2.0f);

            for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
                // Da 0=Unten ist, wächst Y nach Oben.
                // logicalRow 0 ist unten (invY), logicalRow 2 ist oben (invY + 2*höhe)
                float rowY = invY + (logicalRow * hotbarHeight);

                if (invertedMouseY >= rowY && invertedMouseY <= rowY + hotbarHeight) {
                    for (int col = 0; col < 9; col++) {
                        float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                        if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) {
                            return 9 + (logicalRow * 9) + col;
                        }
                    }
                }
            }
        }
        return -1; // Kein Slot getroffen
    }

    public void toggle() { isOpen = !isOpen; }
    public boolean isOpen() { return isOpen; }
    public ItemStack getStack(int i) { return slots[i]; }
    public ItemStack getMouseStack() { return mouseStack; }
    public ItemStack getSelectedHotbarStack() { return slots[selectedHotbarSlot]; }
    public void setSelectedSlot(int s) { this.selectedHotbarSlot = s; }
    public int getSelectedSlot() { return selectedHotbarSlot; }
}