package de.delautrer.game.ui.chat;

import de.delautrer.engine.graphics.IFont;
import org.lwjgl.stb.STBTTBakedChar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatComponent {
    private final List<TextRun> runs;

    public ChatComponent() {
        this.runs = new ArrayList<>();
    }

    public ChatComponent(List<TextRun> runs) {
        this.runs = new ArrayList<>(runs != null ? runs : Collections.emptyList());
    }

    public static ChatComponent plain(String text) {
        ChatComponent comp = new ChatComponent();
        if (text != null && !text.isEmpty()) {
            comp.runs.add(new TextRun(text, Style.EMPTY));
        }
        return comp;
    }

    public static ChatComponent parseLegacy(String text) {
        ChatComponent comp = new ChatComponent();
        if (text == null || text.isEmpty()) return comp;

        int currentColor = 0xFFFFFF;
        boolean bold = false;
        boolean underline = false;
        boolean strikethrough = false;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++; // Skip code char

                if (isColorOrFormatCode(code)) {
                    if (sb.length() > 0) {
                        Style style = new Style(currentColor, bold, underline, strikethrough, null);
                        comp.runs.add(new TextRun(sb.toString(), style));
                        sb.setLength(0);
                    }

                    int color = getColorFromCode(code);
                    if (color != -1) {
                        currentColor = color;
                        bold = false;
                        underline = false;
                        strikethrough = false;
                    } else if (code == 'l') {
                        bold = true;
                    } else if (code == 'n') {
                        underline = true;
                    } else if (code == 'm') {
                        strikethrough = true;
                    } else if (code == 'r') {
                        currentColor = 0xFFFFFF;
                        bold = false;
                        underline = false;
                        strikethrough = false;
                    }
                    // 'o' (italic) and 'k' (obfuscated) are ignored without modifying current state
                }
            } else {
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            Style style = new Style(currentColor, bold, underline, strikethrough, null);
            comp.runs.add(new TextRun(sb.toString(), style));
        }

        return comp;
    }

    private static boolean isColorOrFormatCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'l' || c == 'n' || c == 'm' || c == 'r' || c == 'o' || c == 'k';
    }

    private static int getColorFromCode(char c) {
        return switch (c) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> -1;
        };
    }

    public ChatComponent append(ChatComponent other) {
        if (other != null) {
            this.runs.addAll(other.runs);
        }
        return this;
    }

    public ChatComponent append(TextRun run) {
        if (run != null) {
            this.runs.add(run);
        }
        return this;
    }

    public List<TextRun> getRuns() {
        return Collections.unmodifiableList(runs);
    }

    public String raw() {
        StringBuilder sb = new StringBuilder();
        for (TextRun run : runs) {
            sb.append(run.getText());
        }
        return sb.toString();
    }

    public List<List<TextRun>> wrap(IFont font, float maxWidth) {
        List<List<TextRun>> lines = new ArrayList<>();
        if (runs.isEmpty()) return lines;

        List<TextRun> currentLine = new ArrayList<>();
        float currentLineWidth = 0.0f;

        STBTTBakedChar.Buffer charData = (font != null) ? font.getCharData() : null;

        for (TextRun run : runs) {
            String text = run.getText();
            Style style = run.getStyle();
            boolean isBold = style.isBold();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\n') {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                    currentLineWidth = 0.0f;
                    continue;
                }

                float glyphW = 0.0f;
                if (charData != null && c >= 32 && c < 256) {
                    glyphW = charData.get(c - 32).xadvance() + (isBold ? 1.0f : 0.0f);
                }

                // Omit leading spaces after a line flush
                if (c == ' ' && currentLine.isEmpty()) {
                    continue;
                }

                if (currentLineWidth + glyphW > maxWidth) {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine);
                        currentLine = new ArrayList<>();
                        currentLineWidth = 0.0f;
                    }

                    if (c == ' ') {
                        continue;
                    }

                    if (glyphW > maxWidth) {
                        appendCharToLine(currentLine, c, style);
                        lines.add(currentLine);
                        currentLine = new ArrayList<>();
                        currentLineWidth = 0.0f;
                        continue;
                    }
                }

                appendCharToLine(currentLine, c, style);
                currentLineWidth += glyphW;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private void appendCharToLine(List<TextRun> line, char c, Style style) {
        if (!line.isEmpty()) {
            TextRun lastRun = line.get(line.size() - 1);
            if (lastRun.getStyle().equals(style)) {
                line.set(line.size() - 1, new TextRun(lastRun.getText() + c, style));
                return;
            }
        }
        line.add(new TextRun(String.valueOf(c), style));
    }
}
