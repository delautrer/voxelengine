package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.graphics.ITexture;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.world.persistence.WorldData;

public class UIWorldRow extends UIElement {
    private final String displayName;
    private final String safeFolderName;
    private final WorldData data;
    private final ITexture thumbnail;
    private boolean isSelected;
    private final Runnable onSelect;
    
    public UIWorldRow(float x, float y, float width, float height, String displayName, String safeFolderName, WorldData data, ITexture thumbnail, Runnable onSelect) {
        super(x, y, width, height);
        this.displayName = displayName;
        this.safeFolderName = safeFolderName;
        this.data = data;
        this.thumbnail = thumbnail;
        this.onSelect = onSelect;
        this.isSelected = false;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public String getSafeFolderName() {
        return safeFolderName;
    }
    
    public WorldData getData() {
        return data;
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;
        
        boolean hovered = isHovered(mouseX, mouseY);
        
        // Background
        float r = 0.1f, g = 0.1f, b = 0.1f, a = 0.8f;
        if (isSelected) {
            r = 0.2f; g = 0.4f; b = 0.2f; a = 0.9f;
        } else if (hovered) {
            r = 0.15f; g = 0.15f; b = 0.15f; a = 0.9f;
        }
        
        builder.addRect(x, y, 0.11f, width, height, r, g, b, a);
        
        // Outline
        if (isSelected || hovered) {
            builder.addRect(x, y, 0.12f, width, 2.0f, 0.5f, 0.8f, 0.5f, 1.0f); // Bottom
            builder.addRect(x, y + height - 2.0f, 0.12f, width, 2.0f, 0.5f, 0.8f, 0.5f, 1.0f); // Top
            builder.addRect(x, y, 0.12f, 2.0f, height, 0.5f, 0.8f, 0.5f, 1.0f); // Left
            builder.addRect(x + width - 2.0f, y, 0.12f, 2.0f, height, 0.5f, 0.8f, 0.5f, 1.0f); // Right
        }

        // Thumbnail
        float thumbSize = height - 10.0f;
        float thumbX = x + 5.0f;
        float thumbY = y + 5.0f;
        
        if (thumbnail != null) {
            builder.addExternalTextureQuad(thumbnail, thumbX, thumbY, 0.13f, thumbSize, thumbSize, true);
        } else {
            builder.addRect(thumbX, thumbY, 0.13f, thumbSize, thumbSize, 0.3f, 0.3f, 0.3f, 1.0f);
        }

        boolean thumbHovered = mouseX >= thumbX && mouseX <= thumbX + thumbSize &&
                               mouseY >= thumbY && mouseY <= thumbY + thumbSize;
        if (thumbHovered) {
            builder.addRect(thumbX, thumbY, 0.135f, thumbSize, thumbSize, 0.0f, 0.0f, 0.0f, 0.5f);
            if (font != null) {
                float pw = builder.getTextWidth("PLAY", font);
                builder.drawText("PLAY", thumbX + (thumbSize - pw) / 2.0f, thumbY + thumbSize / 2.0f - 10.0f, 0.14f, font, 0.2f, 0.9f, 0.2f, 1.0f, 1.0f);
            }
        }

        // Text
        if (font != null) {
            float textX = thumbX + thumbSize + 15.0f;
            float textY = y + height - 30.0f;
            
            builder.drawText(displayName, textX, textY, 0.14f, font);
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            String dateStr = "Never played";
            if (data != null && data.lastOpenedDate > 0) {
                dateStr = "Last played: " + sdf.format(new java.util.Date(data.lastOpenedDate));
            }
            builder.drawText(dateStr, textX, textY - 20.0f, 0.14f, font, 0.5f);
            
            String pathText = "Saves: /" + safeFolderName;
            builder.drawText(pathText, textX, textY - 40.0f, 0.14f, font, 0.5f);
            
            if (data != null && data.lastOpenedVersion != null && !data.lastOpenedVersion.equals(de.delautrer.Constants.VERSION)) {
                builder.drawText("Version: " + data.lastOpenedVersion + " (Current: " + de.delautrer.Constants.VERSION + ")", textX, textY - 60.0f, 0.14f, font, 0.8f, 0.3f, 0.3f, 1.0f, 0.5f);
            }
        }
    }
}
