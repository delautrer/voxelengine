package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.commands.CommandManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.ChatMessageEvent;
import de.delautrer.game.events.CommandExecutedEvent;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.world.World;
import java.util.ArrayList;
import java.util.List;

public class ChatScreen extends MenuScreen {

    private UIInputField inputField;
    private final EventBus eventBus;
    private final LocalPlayer player;
    private final World world;
    private final CommandManager commandManager;
    private final ChatOverlay chatOverlay;
    private final Runnable closeCallback;
    private IFont font;

    // History & Autocomplete Features
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    private List<String> currentCompletions = new ArrayList<>();
    private int completionIndex = -1;
    private boolean isTabCycling = false;

    public ChatScreen(EventBus eventBus, LocalPlayer player, World world, CommandManager commandManager,
            ChatOverlay chatOverlay, Runnable closeCallback) {
        this.eventBus = eventBus;
        this.player = player;
        this.world = world;
        this.commandManager = commandManager;
        this.chatOverlay = chatOverlay;
        this.closeCallback = closeCallback;
    }

    public void open(boolean startWithSlash) {
        inputField.setText(startWithSlash ? "/" : "");
        inputField.setFocused(true);
        historyIndex = history.size(); // Reset an ans Ende der History
        chatOverlay.resetScroll(); // Springt nach unten, wenn der Chat neu geöffnet wird
    }

    public void setFont(IFont font) {
        this.font = font;
    }

    @Override
    protected void onInit() {
        float fieldWidth = width - 20.0f;
        float fieldHeight = 30.0f;
        inputField = new UIInputField(10.0f, 10.0f, fieldWidth, fieldHeight, "Befehl oder Nachricht eingeben...", 100);
        inputField.setFocused(true);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        if (font != null) {
            inputField.render(builder, font, mouseX, mouseY);
        }
    }

    // --- NEU: Input für das Mausrad UND für Tastatur/Tippen ---
    @Override
    public void handleInput(InputManager input) {
        super.handleInput(input);

        inputField.handleInput(input); // Pfeiltasten, Backspace, Entf etc.

        double scroll = input.consumeScroll();
        if (scroll != 0) {
            chatOverlay.scroll((int) Math.signum(scroll) * 3);
        }
    }

    public void handleMenuInput(InputManager input, float mouseX, float mouseY) {
        handleInput(input);
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
    }

    @Override
    protected void onCharTyped(char c) {
        inputField.typeChar(c);
        isTabCycling = false; // Bricht Autocomplete ab, wenn man selbst weitertippt
    }

    @Override
    protected void onKeyPressed(InputManager input) {
        // HINWEIS: UI_BACKSPACE, Pfeiltasten etc. werden jetzt direkt im inputField.handleInput(input)
        // innerhalb von handleInput(input) verarbeitet. Hier nur noch Logik, die den Screen-State ändert.

        // HINWEIS: Die PAUSE (ESC) Abfrage ist hier raus, weil deine PlayScene das
        // jetzt global und viel sauberer managt!

        // --- AUTOCOMPLETE (TAB) ---
        if (input.isActionJustPressed("UI_TAB")) {
            String currentText = inputField.getText();

            if (!isTabCycling) {
                currentCompletions = commandManager.getTabCompletions(player, currentText);
                if (!currentCompletions.isEmpty()) {
                    isTabCycling = true;
                    completionIndex = 0;
                    inputField.setText(currentCompletions.get(completionIndex));
                }
            } else {
                if (!currentCompletions.isEmpty()) {
                    completionIndex = (completionIndex + 1) % currentCompletions.size();
                    inputField.setText(currentCompletions.get(completionIndex));
                }
            }
        }

        // --- HISTORY (UP/DOWN) ---
        if (input.isActionJustPressed("UI_UP")) {
            isTabCycling = false;
            if (historyIndex > 0) {
                historyIndex--;
                inputField.setText(history.get(historyIndex));
            }
        }
        if (input.isActionJustPressed("UI_DOWN")) {
            isTabCycling = false;
            if (historyIndex < history.size() - 1) {
                historyIndex++;
                inputField.setText(history.get(historyIndex));
            } else {
                historyIndex = history.size();
                inputField.setText("");
            }
        }

        // --- ABSENDEN (ENTER) ---
        if (input.isActionJustPressed("CHAT_SEND")) {
            String inputText = inputField.getText();

            if (!inputText.isEmpty()) {
                history.add(inputText); // Zur History hinzufügen

                if (inputText.startsWith("/")) {
                    String[] parts = inputText.substring(1).split(" ");
                    String command = parts[0];
                    String[] args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);

                    eventBus.publish(new CommandExecutedEvent(command, args, player, world));
                } else {
                    eventBus.publish(new ChatMessageEvent("[Player] " + inputText));
                }
            }
            closeCallback.run(); // Chat schließen
            isTabCycling = false;
        }
    }
}
