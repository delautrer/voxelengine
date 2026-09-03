package de.delautrer.game.ui;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.events.ChatMessageEvent;
import de.delautrer.game.ui.chat.ChatComponent;
import de.delautrer.game.ui.chat.TextRun;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatOverlay implements EventListener<ChatMessageEvent> {

    public static class ChatMessage {
        public final ChatComponent component;
        public float timeRemaining = 10.0f; // 10 seconds visible

        public ChatMessage(ChatComponent component) {
            this.component = component != null ? component : ChatComponent.plain("");
        }

        public ChatMessage(String legacyText) {
            this(ChatComponent.parseLegacy(legacyText));
        }
    }

    public static class WrappedLine {
        public final List<TextRun> runs;
        public final ChatMessage parentMessage;

        public WrappedLine(List<TextRun> runs, ChatMessage parentMessage) {
            this.runs = runs;
            this.parentMessage = parentMessage;
        }
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private static final int MAX_MESSAGES = 100;
    private int scrollOffset = 0; // line scroll offset

    @SuppressWarnings("this-escape")
    public ChatOverlay(EventBus eventBus) {
        if (eventBus != null) {
            eventBus.subscribe(ChatMessageEvent.class, this);
        }
    }

    @Override
    public void onEvent(ChatMessageEvent event) {
        if (event != null && event.message != null) {
            addComponent(ChatComponent.parseLegacy(event.message));
        }
    }

    public void addComponent(ChatComponent component) {
        messages.add(new ChatMessage(component));
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        scrollOffset = 0; // Reset scroll on new message
    }

    public void addMessage(String legacyText) {
        addComponent(ChatComponent.parseLegacy(legacyText));
    }

    public boolean update(float deltaTime) {
        boolean needsRebuild = false;
        for (ChatMessage msg : messages) {
            if (msg.timeRemaining > -0.5f) {
                msg.timeRemaining -= deltaTime;
                needsRebuild = true;
            }
        }
        return needsRebuild;
    }

    public List<WrappedLine> getAllWrappedLines(IFont font, float maxWidth) {
        List<WrappedLine> allLines = new ArrayList<>();
        for (ChatMessage msg : messages) {
            List<List<TextRun>> lines = msg.component.wrap(font, maxWidth);
            if (lines.isEmpty()) {
                allLines.add(new WrappedLine(Collections.emptyList(), msg));
            } else {
                for (List<TextRun> lineRuns : lines) {
                    allLines.add(new WrappedLine(lineRuns, msg));
                }
            }
        }
        return allLines;
    }

    public void scroll(int lines, IFont font, float maxWidth) {
        List<WrappedLine> allLines = getAllWrappedLines(font, maxWidth);
        int maxScroll = Math.max(0, allLines.size() - 10);
        scrollOffset += lines;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void resetScroll() {
        scrollOffset = 0;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public TextRun hitTest(float mouseX, float mouseY, IFont font, float windowWidth, float windowHeight, boolean isChatOpen) {
        if (!isChatOpen || font == null) return null;

        float maxWidth = windowWidth * 0.6f;
        List<WrappedLine> allLines = getAllWrappedLines(font, maxWidth);
        if (allLines.isEmpty()) return null;

        int totalLines = allLines.size();
        int maxVisible = 10;
        int startIndex = totalLines - 1 - scrollOffset;
        int endIndex = Math.max(0, startIndex - maxVisible + 1);

        float textY = 50.0f; // starts above input field at bottom
        float lineHeight = 22.0f;

        for (int i = startIndex; i >= endIndex; i--) {
            WrappedLine line = allLines.get(i);
            TextRun hitRun = font != null ? hitTestStyled(line.runs, 10.0f, textY, lineHeight, mouseX, mouseY, font) : null;
            if (hitRun != null) {
                return hitRun;
            }
            textY += lineHeight;
        }

        return null;
    }

    private TextRun hitTestStyled(List<TextRun> runs, float originX, float originY, float lineHeight, float mouseX, float mouseY, IFont font) {
        if (runs == null || font == null || font.getCharData() == null) return null;
        if (mouseY < originY - 2.0f || mouseY > originY + 18.0f) return null;

        float currentX = originX;
        var charData = font.getCharData();
        for (TextRun run : runs) {
            if (run == null) continue;
            float runStartX = currentX;
            String text = run.getText();
            boolean isBold = run.getStyle().isBold();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= 32 && c < 256) {
                    currentX += charData.get(c - 32).xadvance() + (isBold ? 1.0f : 0.0f);
                }
            }
            float runEndX = currentX;
            if (mouseX >= runStartX && mouseX <= runEndX) {
                return run;
            }
        }
        return null;
    }

    public static void executeClickAction(de.delautrer.game.ui.chat.ClickEvent click) {
        if (click == null || click.getValue() == null) return;

        if (click.getAction() == de.delautrer.game.ui.chat.ClickEvent.Action.OPEN_FILE) {
            try {
                File file = new File(click.getValue());
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                } else {
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start();
                    } else if (os.contains("mac")) {
                        new ProcessBuilder("open", file.getAbsolutePath()).start();
                    } else {
                        new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to open file: " + e.getMessage());
            }
        }
    }
}