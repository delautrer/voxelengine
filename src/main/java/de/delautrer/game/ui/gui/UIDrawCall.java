package de.delautrer.game.ui.gui;

public class UIDrawCall {
    public final UITexture texture;
    public final int indexOffset;
    public final int indexCount;

    public UIDrawCall(UITexture texture, int indexOffset, int indexCount) {
        this.texture = texture;
        this.indexOffset = indexOffset;
        this.indexCount = indexCount;
    }
}