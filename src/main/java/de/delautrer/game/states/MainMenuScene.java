package de.delautrer.game.states;

import de.delautrer.engine.Engine;
import de.delautrer.engine.MenuRenderer;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.Scene;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.gui.screens.WorldSelectScreen;
import org.lwjgl.glfw.GLFW;

public class MainMenuScene extends Scene {

    private MenuRenderer menuRenderer;

    // Wir merken uns das "Root"-Menü (Das allererste Menü)
    private MenuScreen mainScreen;

    // Das ist das Menü, das gerade TATSÄCHLICH gezeichnet und geklickt wird
    private MenuScreen activeScreen;

    public MainMenuScene(Engine engine) {
        super(engine);
    }

    @Override
    public void init() {
        engine.getWindow().enableCursor();
        menuRenderer = new MenuRenderer(engine.getVulkanContext(), engine.getWindow());

        mainScreen = new MenuScreen() {
            @Override
            protected void onInit() {
                elements.clear();

                float btnWidth = 256;
                float btnHeight = 40;
                float centerX = width / 2.0f - btnWidth / 2.0f;
                float centerY = height / 2.0f;

                // --- SPIELEN BUTTON ---
                elements.add(new UIButton(centerX, centerY + 30, btnWidth, btnHeight, "Play Singleplayer", () -> {

                    // 1. Wir erstellen den Welten-Screen und geben ihm mit, was bei "Zurück" passieren soll:
                    WorldSelectScreen worldScreen = new WorldSelectScreen(engine, () -> {
                        activeScreen = mainScreen; // Beim Klick auf Zurück: Wieder das Hauptmenü anzeigen!
                    });

                    // 2. Wir initialisieren ihn auf die Fenstergröße und geben ihm die Schriftart
                    worldScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
                    worldScreen.setFont(menuRenderer.getFont());

                    // 3. Wir machen ihn zum aktiven Screen! (Der Wechsel passiert in einem Frame)
                    activeScreen = worldScreen;
                }));

                // --- BEENDEN BUTTON ---
                elements.add(new UIButton(centerX, centerY - 30, btnWidth, btnHeight, "Quit Game", () -> {
                    GLFW.glfwSetWindowShouldClose(engine.getWindow().getHandle(), true);
                }));
            }
        };

        // Das Root-Menü einmalig aufsetzen
        mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        mainScreen.setFont(menuRenderer.getFont());

        // Zum Start ist das Hauptmenü natürlich der aktive Screen
        activeScreen = mainScreen;
    }

    @Override
    public void update(float deltaTime) {
        if (activeScreen == null) return;

        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();

        // Wir leiten den Input IMMER an den aktuell aktiven Screen weiter
        activeScreen.handleMenuInput(input, uiMouseX, uiMouseY);
    }

    @Override
    public void render() {
        if (activeScreen == null) return;

        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();

        // Wir zeichnen IMMER den aktuell aktiven Screen
        menuRenderer.draw(activeScreen, uiMouseX, uiMouseY);
    }

    @Override
    public void onResize() {
        menuRenderer.recreate();
        if (mainScreen != null) mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        // Falls wir gerade im World-Screen sind, müssen wir den auch resizen!
        if (activeScreen != null && activeScreen != mainScreen) {
            activeScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        }
    }

    @Override
    public void cleanup() {
        if (menuRenderer != null) menuRenderer.cleanup();
    }
}