package de.delautrer.game.ui.chat;

public class Style {
    public static final Style EMPTY = new Style(0xFFFFFF, false, false, false, null);

    private final int rgb;
    private final boolean bold;
    private final boolean underline;
    private final boolean strikethrough;
    private final ClickEvent click;

    public Style(int rgb, boolean bold, boolean underline, boolean strikethrough, ClickEvent click) {
        this.rgb = rgb;
        this.bold = bold;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.click = click;
    }

    public int getRgb() {
        return rgb;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isUnderline() {
        return underline;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public ClickEvent getClick() {
        return click;
    }

    public Style withColor(int rgb) {
        return new Style(rgb, bold, underline, strikethrough, click);
    }

    public Style withBold(boolean bold) {
        return new Style(rgb, bold, underline, strikethrough, click);
    }

    public Style withUnderline(boolean underline) {
        return new Style(rgb, bold, underline, strikethrough, click);
    }

    public Style withStrikethrough(boolean strikethrough) {
        return new Style(rgb, bold, underline, strikethrough, click);
    }

    public Style withClick(ClickEvent.Action action, String value) {
        return new Style(rgb, bold, underline, strikethrough, new ClickEvent(action, value));
    }

    public Style withClick(ClickEvent click) {
        return new Style(rgb, bold, underline, strikethrough, click);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Style style = (Style) o;
        return rgb == style.rgb &&
                bold == style.bold &&
                underline == style.underline &&
                strikethrough == style.strikethrough &&
                java.util.Objects.equals(click, style.click);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(rgb, bold, underline, strikethrough, click);
    }
}
