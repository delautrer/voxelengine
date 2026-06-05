package de.delautrer.game.ui.elements;

public class UIDrawCall {
    public final Object texture;
    public final int indexOffset;
    public final int indexCount;

    public UIDrawCall(Object texture, int indexOffset, int indexCount) {
        this.texture = texture;
        this.indexOffset = indexOffset;
        this.indexCount = indexCount;
    }
}