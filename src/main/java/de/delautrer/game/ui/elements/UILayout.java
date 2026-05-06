package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.vulkan.texture.VulkanFont;
import de.delautrer.game.ui.UIMeshBuilder;

import java.util.ArrayList;
import java.util.List;

public abstract class UILayout extends UIElement {
    protected final List<UIElement> children = new ArrayList<>();
    public float spacing;

    public UILayout(float x, float y, float spacing) {
        super(x, y, 0, 0); // Width und Height werden automatisch berechnet!
        this.spacing = spacing;
    }

    public void addChild(UIElement element) {
        children.add(element);
        pack(); // Layout nach jedem neuen Element neu berechnen
    }

    public List<UIElement> getChildren() {
        return children;
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        pack(); // Wenn das Layout verschoben wird, ziehen alle Kinder automatisch mit!
    }

    // Muss von VBox/HBox implementiert werden, um Kinder zu positionieren
    public abstract void pack();

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;
        for (UIElement child : children) {
            child.render(builder, font, mouseX, mouseY);
        }
    }
}
