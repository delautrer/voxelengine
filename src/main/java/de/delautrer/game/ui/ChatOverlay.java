package de.delautrer.game.ui;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.game.events.ChatMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatOverlay implements EventListener<ChatMessageEvent> {
    private final List<String> messages = new ArrayList<>();
    private static final int MAX_MESSAGES = 10;

    public ChatOverlay(EventBus eventBus) {
        eventBus.subscribe(ChatMessageEvent.class, this);
    }

    @Override
    public void onEvent(ChatMessageEvent event) {
        messages.add(event.message);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    public List<String> getMessages() {
        return messages;
    }
}