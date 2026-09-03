package de.delautrer.game.ui.chat;

public class ClickEvent {
    public enum Action {
        OPEN_FILE,
        OPEN_URL
    }

    private final Action action;
    private final String value;

    public ClickEvent(Action action, String value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }
}
