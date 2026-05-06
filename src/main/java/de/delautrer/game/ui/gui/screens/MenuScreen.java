package de.delautrer.game.ui.gui.screens;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.*;
import java.util.ArrayList;
import java.util.List;



public abstract class MenuScreen extends Screen {

    protected List<UIElement> elements = new ArrayList<>();
    protected IFont font;

    public void setFont(IFont font) {
        this.font = font;
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        for (UIElement element : elements) {
            element.render(builder, font, mouseX, mouseY);
        }
    }

    public void handleMenuInput(InputManager input, float uiMouseX, float uiMouseY) {
        List<UIElement> elementsCopy = getFlattenedElements();

        for (UIElement element : elementsCopy) {
            if (element instanceof UIScrollableList) {
                ((UIScrollableList) element).handleInput(input, uiMouseX, uiMouseY);
            }
            if (element instanceof UISlider) {
                ((UISlider) element).handleInput(input, uiMouseX, uiMouseY);
            }
            if (element instanceof UIKeybindButton) {
                ((UIKeybindButton) element).handleInput(input, uiMouseX, uiMouseY);
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

    private List<UIElement> getFlattenedElements() {
        List<UIElement> flatList = new ArrayList<>();
        flattenRecursive(elements, flatList);
        return flatList;
    }

    private void flattenRecursive(List<UIElement> source, List<UIElement> target) {
        for (UIElement el : source) {
            if (el instanceof UILayout) {
                flattenRecursive(((UILayout) el).getChildren(), target);
            } else {
                target.add(el);
            }
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) { return -1; }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {}

    @Override
    protected void onInit() {}
}
