package de.delautrer.game.ui.chat;

public class TextRun {
    private final String text;
    private final Style style;

    public TextRun(String text, Style style) {
        this.text = text != null ? text : "";
        this.style = style != null ? style : Style.EMPTY;
    }

    public String getText() {
        return text;
    }

    public Style getStyle() {
        return style;
    }
}
