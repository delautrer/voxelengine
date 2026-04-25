package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.*;

import java.util.ArrayList;
import java.util.List;

public abstract class MenuScreen extends Screen {

    protected List<UIElement> elements = new ArrayList<>();
    protected VulkanFont font;

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        for (UIElement element : elements) {
            element.render(builder, font, mouseX, mouseY);
        }
    }

    public void handleMenuInput(InputManager input, float uiMouseX, float uiMouseY) {
        List<UIElement> elementsCopy = new ArrayList<>(elements);

        for (UIElement element : elementsCopy) {
            if (element instanceof UIScrollableList) {
                ((UIScrollableList) element).handleInput(input, uiMouseX, uiMouseY);
            }
        }

        // --- 1. MAUS-KLICKS (Fokus setzen & Buttons klicken) ---
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            boolean clickHandled = false;

            for (UIElement element : elementsCopy) {
                if (element instanceof UIInputField) {
                    ((UIInputField) element).setFocused(element.isHovered(uiMouseX, uiMouseY));
                }

                if (element instanceof UIScrollableList) {
                    ((UIScrollableList) element).handleInput(input, uiMouseX, uiMouseY);
                }

                if (!clickHandled && element.isHovered(uiMouseX, uiMouseY)) {
                    if (element instanceof UIButton) {
                        ((UIButton) element).click();
                        clickHandled = true;
                    } else if (element instanceof UIToggleButton) {
                        ((UIToggleButton) element).click();
                        clickHandled = true;
                    } else if (element instanceof UIConfirmButton) {
                        ((UIConfirmButton) element).click();
                        clickHandled = true;
                    }
                }
            }
        }

        // --- 2. TASTATUR-EINGABEN AN FOKUSSIERTE FELDER SENDEN ---
        UIInputField focusedField = null;
        for (UIElement element : elements) {
            if (element instanceof UIInputField && ((UIInputField) element).isFocused()) {
                focusedField = (UIInputField) element;
                break;
            }
        }

        if (focusedField != null) {
            for (char c : input.consumeTypedChars()) {
                focusedField.typeChar(c);
            }
            if (input.isActionJustPressed("UI_BACKSPACE")) {
                focusedField.backspace();
            }
        } else {
            input.consumeTypedChars();
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) { return -1; }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {}

    @Override
    protected void onInit() {}
}