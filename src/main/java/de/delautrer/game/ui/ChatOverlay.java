package de.delautrer.game.ui;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.game.events.ChatMessageEvent;
import java.util.ArrayList;
import java.util.List;

public class ChatOverlay implements EventListener<ChatMessageEvent> {

    // Neues Objekt speichert Nachricht + Timer
    public static class ChatMessage {
        public final String text;
        public float timeRemaining = 10.0f; // 10 Sekunden sichtbar
        public ChatMessage(String text) {
            this.text = text;
        }
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private static final int MAX_MESSAGES = 100; // Mehr Nachrichten für die Historie speichern
    private int scrollOffset = 0;

    @SuppressWarnings("this-escape")
    public ChatOverlay(EventBus eventBus) {
        eventBus.subscribe(ChatMessageEvent.class, this);
    }

    @Override
    public void onEvent(ChatMessageEvent event) {
        messages.add(new ChatMessage(event.message));
        if (messages.size() > MAX_MESSAGES) messages.remove(0);
        scrollOffset = 0; // Bei neuer Nachricht automatisch nach unten springen
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

    public void scroll(int amount) {
        scrollOffset += amount;
        int maxScroll = Math.max(0, messages.size() - 10);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    public int getScrollOffset() { return scrollOffset; }
    public void resetScroll() { scrollOffset = 0; }
    public List<ChatMessage> getMessages() { return messages; }
}