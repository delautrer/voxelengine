package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import java.util.ArrayList;
import java.util.List;

public abstract class MenuScreen extends Screen {

    protected List<UIElement> elements = new ArrayList<>();
    protected VulkanFont font; // Wir brauchen die Font-Referenz hier

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Für Menüs wollen wir oft einen dunklen Hintergrund, den könnten wir hier zeichnen

        for (UIElement element : elements) {
            element.render(builder, font, mouseX, mouseY);
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        return -1; // Menüs haben keine Inventar-Slots
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        if (button != 0) return; // Nur Linksklick

        for (UIElement element : elements) {
            if (element instanceof UIButton && element.isHovered(mouseX, mouseY)) {
                ((UIButton) element).click();
                return; // Nur einen Button pro Klick auslösen
            }
        }
    }
}