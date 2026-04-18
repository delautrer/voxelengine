package de.delautrer.game.events;

import de.delautrer.engine.events.Event;

public class ChatMessageEvent implements Event {
    public final String message;
    public ChatMessageEvent(String message) {
        this.message = message;
    }
}