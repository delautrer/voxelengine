package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.UIUtils;
import de.delautrer.game.ui.gui.BaseContainer;
import de.delautrer.game.ui.gui.ClickType;
import de.delautrer.game.ui.gui.Slot;
import de.delautrer.game.ui.gui.UIMeshBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class ContainerScreen extends MenuScreen {
    protected final BaseContainer container;
    protected VulkanFont font;

    protected float guiX, guiY;
    protected float slotSize;

    private boolean isDragging = false;
    private final Set<Slot> dragSlots = new LinkedHashSet<>();
    private long lastClickTime = 0;
    private Slot lastClickedSlot = null;

    public ContainerScreen(BaseContainer container) {
        this.container = container;
    }

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    protected abstract void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY);

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // 1. Hintergrund zeichnen
        drawBackground(builder, mouseX, mouseY);

        Slot hoveredSlot = getHoveredSlotObj(mouseX, mouseY);

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

            /*
            // Drag-Highlight
            if (isDragging && dragSlots.contains(slot)) {
                builder.addRect(slotX, slotY, 0.25f, slotSize, slotSize, 0.8f, 0.8f, 1.0f, 0.4f);
            }
            */

            // Item im Slot
            ItemStack stack = slot.getStack();
            builder.drawItem(stack, slotX + 3 * pixelScale, slotY + 3 * pixelScale, 0.5f, slotSize - 6 * pixelScale);

            // Anzahl
            if (stack != null && stack.amount > 1 && font != null) {
                builder.drawText(String.valueOf(stack.amount), slotX + slotSize - (12.0f * pixelScale), slotY + (2.0f * pixelScale), 0.25f, font);
            }
        }

        // 3. Item an der Maus
        ItemStack mouseStack = container.getMouseStack();
        if (mouseStack != null) {
            float itemSize = slotSize - 4 * pixelScale;
            float invertedMouseY = height - mouseY;
            builder.drawItem(mouseStack, mouseX - itemSize / 2.0f + 3 * pixelScale, invertedMouseY - itemSize / 2.0f + 3 * pixelScale, 0.6f, itemSize);

            if (mouseStack.amount > 1 && font != null) {
                builder.drawText(String.valueOf(mouseStack.amount), mouseX + (itemSize / 2.0f) - (8.0f * pixelScale), invertedMouseY - (itemSize / 2.0f), 0.25f, font);
            }
        }

        // 4. Automatischer Tooltip ganz oben auf allem
        if (hoveredSlot != null && mouseStack == null && hoveredSlot.hasItem() && font != null) {
            String name = UIUtils.formatItemName(de.delautrer.game.items.ItemRegistry.getId(hoveredSlot.getStack().type));
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

        builder.addTooltipBackground(tipX, tipY - (textHeight / 2.0f), 1.0f, textWidth + (12.0f*pixelScale), textHeight + (8.0f*pixelScale), 4, 0, 4.0f * pixelScale);
        builder.drawText(text, tipX + (6.0f*pixelScale), tipY, textScale, font);
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
    public void handleInput(de.delautrer.engine.input.InputManager input) {
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
                if (hovered != null) container.hotbarSwap(hovered, i);
                break;
            }
        }

        // --- Inventar sortieren (Mittlere Maustaste) ---
        if (input.isActionJustPressed("PICK_BLOCK")) {
            Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
            if (hovered != null) container.sortRegion(hovered);
        }

        // --- Maus-States ---
        boolean shiftPressed = false;
        try { shiftPressed = input.isActionActive("SNEAK"); } catch (Exception e) {}

        boolean leftDown = input.isActionActive("INTERACT_BREAK");
        boolean leftJustPressed = input.isActionJustPressed("INTERACT_BREAK");
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
            if (leftJustPressed) {
                // Wir fangen an zu ziehen
                isDragging = true;
                dragSlots.clear();
            }

            if (leftDown && isDragging) {
                // Solange gedrückt, Slots sammeln
                Slot hovered = getHoveredSlotObj(mouseX, rawMouseY);
                if (hovered != null) dragSlots.add(hovered);
            } else if (!leftDown && isDragging) {
                // Beim Loslassen verteilen
                container.applyDrag(dragSlots);
                isDragging = false;
                dragSlots.clear();
            }
        } else {
            // Normales Klicken (wenn Hand leer ODER wenn wir Shift gedrückt halten)
            if (leftJustPressed) {
                handleContainerClick(mouseX, rawMouseY, 0, shiftPressed ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
        }

        // --- Rechtsklick (Splitten) geht immer normal ---
        if (rightJustPressed) {
            handleContainerClick(mouseX, rawMouseY, 1, ClickType.SPLIT);
        }
    }

    private void handleContainerClick(float mouseX, float rawMouseY, int button, ClickType type) {
        Slot slot = getHoveredSlotObj(mouseX, rawMouseY);
        if (slot != null) {
            container.clickSlot(slot, button, type);
        }
    }

    // Muss wegen der abstrakten Basisklasse leer bleiben (wird nicht mehr benutzt)
    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {}
}