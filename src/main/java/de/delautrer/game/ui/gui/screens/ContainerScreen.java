package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.UIUtils;
import de.delautrer.game.ui.gui.InventoryConstants;
import de.delautrer.game.ui.gui.container.BaseContainer;
import de.delautrer.game.ui.gui.ClickType;
import de.delautrer.game.ui.gui.container.Slot;
import de.delautrer.game.ui.UIMeshBuilder;
import java.util.LinkedHashSet;
import java.util.Set;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.items.ItemRegistry;

public abstract class ContainerScreen extends MenuScreen {
    protected final BaseContainer container;
    protected IFont font;
    protected PlayerInteraction interaction;

    protected float guiX, guiY;
    protected float slotSize;

    private boolean isDragging = false;
    private int dragButton = -1;
    private final Set<Slot> dragSlots = new LinkedHashSet<>();
    private long lastClickTime = 0;
    private Slot lastClickedSlot = null;

    public ContainerScreen(BaseContainer container) {
        this.container = container;
    }

    public void setFont(IFont font) {
        this.font = font;
    }

    public void setInteraction(PlayerInteraction interaction) {
        this.interaction = interaction;
    }

    protected abstract void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY);

    @Override
    public void onClose() {
        super.onClose();
        if (container != null) {
            container.onContainerClosed();
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // 1. Hintergrund zeichnen
        drawBackground(builder, mouseX, mouseY);

        Slot hoveredSlot = getHoveredSlotObj(mouseX, mouseY);
        ItemStack mouseStack = container.getMouseStack();

        // --- Drag-Vorschau Logik ---
        int amountPerSlot = 0;
        int remainingMouseAmount = (mouseStack != null) ? mouseStack.amount : 0;
        java.util.List<Slot> validDragSlots = new java.util.ArrayList<>();

        if (isDragging && mouseStack != null) {
            for (Slot s : dragSlots) {
                if (s.inventory == null) continue;
                if (!s.isItemValid(mouseStack)) continue;

                ItemStack st = s.getStack();
                if (st == null || (st.type == mouseStack.type && st.amount < st.type.getMaxStackSize())) {
                    validDragSlots.add(s);
                }
            }
            if (!validDragSlots.isEmpty()) {
                if (dragButton == 1) { // Right drag
                    amountPerSlot = 1;
                    while (validDragSlots.size() > mouseStack.amount) {
                        validDragSlots.remove(validDragSlots.size() - 1);
                    }
                    remainingMouseAmount = mouseStack.amount - validDragSlots.size();
                } else {
                    amountPerSlot = mouseStack.amount / validDragSlots.size();
                    remainingMouseAmount = mouseStack.amount - (amountPerSlot * validDragSlots.size());
                }
            }
        }

        // 2. Slots & Items rendern
        for (Slot slot : container.slots) {
            float slotX = guiX + (slot.x * pixelScale);
            float slotY = guiY + (slot.y * pixelScale);

            // Slot Hintergrund
            builder.addAtlasQuad(slotX, slotY, 0.1f, slotSize, slotSize, 5, 0, 1, 1, false);

            // Hover-Highlight
            if (slot == hoveredSlot) {
                builder.addAtlasQuad(slotX, slotY, 0.2f, slotSize, slotSize, 10, 1, 1, 1, false);
            }

            // Item im Slot (oder Drag-Preview)
            ItemStack stack = slot.getStack();
            int displayAmount = (stack != null) ? stack.amount : 0;
            boolean isPreview = false;

            if (isDragging && validDragSlots.contains(slot) && amountPerSlot > 0) {
                if (stack == null) {
                    stack = new ItemStack(mouseStack.type, amountPerSlot);
                    displayAmount = amountPerSlot;
                    isPreview = true;
                } else if (stack.type == mouseStack.type) {
                    displayAmount = stack.amount + amountPerSlot;
                    isPreview = true;
                }
            }

            if (stack != null) {
                float itemSize = InventoryConstants.ITEM_SIZE * pixelScale;
                builder.drawItem(stack, slotX + (slotSize - itemSize) * 0.5f,
                        slotY + (slotSize - itemSize) * 0.5f, 0.25f, itemSize);

                // Anzahl
                if (displayAmount > 1 && font != null) {
                    String amountStr = String.valueOf(displayAmount);
                    float textX = slotX + slotSize - ((displayAmount > 9 ? 12.0f : 8.0f) * pixelScale);
                    builder.drawText(amountStr, textX, slotY + (2.0f * pixelScale), 0.3f, font);
                }
            }
        }

        // 3. Item an der Maus
        if (mouseStack != null) {
            float itemSize = InventoryConstants.ITEM_SIZE * pixelScale;
            float invertedMouseY = height - mouseY;
            builder.drawItem(mouseStack, mouseX - itemSize / 2.0f,
                    invertedMouseY - itemSize / 2.0f, 0.5f, itemSize);

            int mouseDisplayAmount = isDragging ? remainingMouseAmount : mouseStack.amount;

            if (mouseDisplayAmount > 1 && font != null) {
                String amountStr = String.valueOf(mouseDisplayAmount);
                float textX = mouseX + (itemSize / 2.0f) - ((mouseDisplayAmount > 9 ? 14.0f : 10.0f) * pixelScale);
                builder.drawText(amountStr, textX, invertedMouseY - (itemSize / 2.0f) + (3.0f * pixelScale), 0.55f, font);
            }
        }

        // 4. Automatischer Tooltip ganz oben auf allem
        if (hoveredSlot != null && mouseStack == null && hoveredSlot.hasItem() && font != null) {
            String name = UIUtils.formatItemName(ItemRegistry.getId(hoveredSlot.getStack().type));
            drawTooltip(builder, name, mouseX, mouseY);
        }
    }

    protected void drawTooltip(UIMeshBuilder builder, String text, float mouseX, float mouseY) {
        float textScale = 0.2f;
        float textWidth = builder.getTextWidth(text, font);
        float textHeight = 28.0f * pixelScale * textScale;

        float invertedMouseY = height - mouseY;
        float tipX = mouseX + (12.0f * pixelScale);
        float tipY = invertedMouseY - (12.0f * pixelScale);

        if (tipX + textWidth + (10.0f * pixelScale) > width) {
            tipX = mouseX - textWidth - (16.0f * pixelScale);
        }
        if (tipY < textHeight) {
            tipY = textHeight;
        }

        builder.addTooltipBackground(tipX, tipY - (textHeight / 2.0f), 1.0f, textWidth + (12.0f * pixelScale),
                textHeight + (8.0f * pixelScale), 4, 0, 4.0f * pixelScale);
        builder.drawText(text, tipX + (6.0f * pixelScale), tipY, 1.1f, font);
    }

    public Slot getHoveredSlotObj(float mouseX, float mouseY) {
        float invertedMouseY = height - mouseY;
        for (Slot slot : container.slots) {
            float slotX = guiX + (slot.x * pixelScale);
            float slotY = guiY + (slot.y * pixelScale);

            if (mouseX >= slotX && mouseX <= slotX + slotSize &&
                    invertedMouseY >= slotY && invertedMouseY <= slotY + slotSize) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        Slot slot = getHoveredSlotObj(mouseX, mouseY);
        return slot != null ? slot.slotIndex : -1;
    }

    @Override
    public void handleInput(InputManager input) {
        onKeyPressed(input);
        for (char c : input.consumeTypedChars()) {
            onCharTyped(c);
        }

        float mouseX = input.getMouseX();
        float rawMouseY = input.getMouseY(); // FIX: Hier invertieren wir NICHT mehr!

        // --- Hotkey Swapping 1-9 ---
        for (int i = 0; i < 9; i++) {
            if (input.isActionJustPressed("SLOT_" + (i + 1))) {
                Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
                if (hovered != null)
                    container.hotbarSwap(hovered, i);
                break;
            }
        }

        // --- Inventar sortieren (Mittlere Maustaste) ---
        if (input.isActionJustPressed("PICK_BLOCK")) {
            Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
            if (hovered != null)
                container.sortRegion(hovered);
        }

        if (input.isActionJustPressed("DROP_ITEM") && interaction != null) {
            Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
            boolean fullStack = input.isControlDown();

            if (hovered != null && hovered.hasItem()) {
                ItemStack stack = hovered.getStack();
                int amount = fullStack ? stack.amount : 1;
                ItemStack dropStack = new ItemStack(stack.type, amount);
                dropStack.durability = stack.durability;
                stack.amount -= amount;
                if (stack.amount <= 0) hovered.putStack(null);
                else hovered.onSlotChanged();
                interaction.dropStack(dropStack);
            }
            return;
        }

        // --- Maus-States ---
        boolean shiftPressed = false;
        try {
            shiftPressed = input.isActionActive("SNEAK");
        } catch (Exception e) {
        }

        boolean leftDown = input.isActionActive("INTERACT_BREAK");
        boolean leftJustPressed = input.isActionJustPressed("INTERACT_BREAK");
        boolean rightDown = input.isActionActive("INTERACT_PLACE");
        boolean rightJustPressed = input.isActionJustPressed("INTERACT_PLACE");

        // --- UPDATE B: Doppelklick ---
        if (leftJustPressed) {
            long now = System.currentTimeMillis();
            Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);

            // Wenn man innerhalb von 300ms zweimal denselben Slot anklickt
            if (hovered != null && lastClickedSlot == hovered && (now - lastClickTime) < 300) {
                container.gatherItems(hovered);
                lastClickTime = 0;
                return; // Normalen Klick überspringen
            }
            lastClickTime = now;
            lastClickedSlot = hovered;
        }

        // --- UPDATE B: Drag & Drop ---
        if (container.getMouseStack() != null && !shiftPressed) {
            if (leftJustPressed || rightJustPressed) {
                if (getHoveredSlotObj(mouseX, rawMouseY) == null && interaction != null) {
                    interaction.dropStack(container.getMouseStack());
                    container.setMouseStack(null);
                    return;
                }
                // Wir fangen an zu ziehen
                isDragging = true;
                dragButton = leftJustPressed ? 0 : 1;
                dragSlots.clear();
            }

            boolean isCurrentButtonDown = (dragButton == 0) ? leftDown : rightDown;

            if (isCurrentButtonDown && isDragging) {
                // Solange gedrückt, Slots sammeln
                Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
                if (hovered != null) {
                    if (dragButton == 1 && dragSlots.size() >= container.getMouseStack().amount && !dragSlots.contains(hovered)) {
                        // Bei Rechtsklick maximal so viele Slots sammeln wie Items da sind
                    } else {
                        dragSlots.add(hovered);
                    }
                }
            } else if (!isCurrentButtonDown && isDragging) {
                // Beim Loslassen verteilen
                container.applyDrag(dragSlots, dragButton);
                isDragging = false;
                dragButton = -1;
                dragSlots.clear();
            }
        } else {
            // Normales Klicken (wenn Hand leer ODER wenn wir Shift gedrückt halten)
            if (leftJustPressed) {
                handleContainerClick(mouseX, rawMouseY, 0, shiftPressed ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
            if (rightJustPressed) {
                handleContainerClick(mouseX, rawMouseY, 1, ClickType.SPLIT);
            }
        }
    }

    private void handleContainerClick(float mouseX, float rawMouseY, int button, ClickType type) {
        Slot slot = getHoveredSlotObj(mouseX, rawMouseY);
        if (slot != null) {
            container.clickSlot(slot, button, type);
        } else if (button == 0 && type == ClickType.PICKUP && container.getMouseStack() != null && interaction != null) {
            interaction.dropStack(container.getMouseStack());
            container.setMouseStack(null);
        }
    }

    // Muss wegen der abstrakten Basisklasse leer bleiben (wird nicht mehr benutzt)
    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
    }

    public BaseContainer getContainer() {
        return container;
    }
}
