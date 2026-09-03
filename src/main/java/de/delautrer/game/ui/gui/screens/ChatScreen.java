package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.commands.CommandManager;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.ChatMessageEvent;
import de.delautrer.game.events.CommandExecutedEvent;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.chat.TextRun;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.world.World;
import org.lwjgl.glfw.GLFW;

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

    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    private List<String> currentCompletions = new ArrayList<>();
    private int completionIndex = -1;
    private boolean isTabCycling = false;

    private long lastClickTime = 0;

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
        historyIndex = history.size();
        chatOverlay.resetScroll();
    }

    public void setFont(IFont font) {
        this.font = font;
    }

    @Override
    protected void onInit() {
        float fieldWidth = width - 8.0f;
        float fieldHeight = 28.0f;
        inputField = new UIInputField(4.0f, 4.0f, fieldWidth, fieldHeight, "Befehl oder Nachricht eingeben...", 256);
        inputField.setFocused(true);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        if (font != null) {
            inputField.render(builder, font, mouseX, mouseY);
        }
    }

    @Override
    public void handleInput(InputManager input) {
        super.handleInput(input);
        inputField.handleInput(input);

        double scroll = input.consumeScroll();
        if (scroll != 0) {
            chatOverlay.scroll((int) Math.signum(scroll) * 3, font, width * 0.6f);
        }
    }

    public void handleMenuInput(InputManager input, float mouseX, float uiMouseY) {
        TextRun hoveredRun = chatOverlay.hitTest(mouseX, uiMouseY, font, width, height, true);
        boolean isHoveringClick = hoveredRun != null && hoveredRun.getStyle().getClick() != null;
        input.setUICursorState(true, isHoveringClick);

        if (input.isActionJustPressed("INTERACT_BREAK")) {
            TextRun clickedRun = chatOverlay.hitTest(mouseX, uiMouseY, font, width, height, true);
            if (clickedRun != null && clickedRun.getStyle().getClick() != null) {
                ChatOverlay.executeClickAction(clickedRun.getStyle().getClick());
            } else if (inputField != null) {
                inputField.setFocused(true);
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    inputField.onDoubleClick(mouseX, uiMouseY, font);
                } else {
                    inputField.onMouseDown(mouseX, uiMouseY, font, false);
                }
                lastClickTime = now;
            }
        }

        handleInput(input);
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        // Handled in handleMenuInput with correct uiMouseY coordinate
    }

    @Override
    protected void onCharTyped(char c) {
        inputField.typeChar(c);
        isTabCycling = false;
    }

    @Override
    protected void onKeyPressed(InputManager input) {
        if (input.isActionJustPressed("UI_TAB")) {
            String currentText = inputField.getText();
            if (!isTabCycling) {
                currentCompletions = commandManager.getTabCompletions(player, world, currentText);
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

        if (input.isActionJustPressed("CHAT_SEND")) {
            String inputText = inputField.getText();
            if (!inputText.isEmpty()) {
                history.add(inputText);
                if (inputText.startsWith("/")) {
                    String[] parts = inputText.substring(1).split(" ");
                    String command = parts[0];
                    String[] args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                    eventBus.publish(new CommandExecutedEvent(command, args, player, world));
                } else {
                    String pName = de.delautrer.game.settings.SettingsManager.get().playerName;
                    if (pName == null || pName.trim().isEmpty()) pName = "Player";
                    eventBus.publish(new ChatMessageEvent(pName + ": " + inputText));
                }
            }
            closeCallback.run();
            isTabCycling = false;
        }
    }
}
