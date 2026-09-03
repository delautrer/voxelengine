package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.ui.chat.ChatComponent;

public class ChatMessageEvent implements Event {
    public final String message;
    public final ChatComponent component;

    public ChatMessageEvent(String message) {
        this.message = message;
        this.component = null;
    }

    public ChatMessageEvent(ChatComponent component) {
        this.message = null;
        this.component = component;
    }
}