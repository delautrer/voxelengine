package de.delautrer.game.states;

import de.delautrer.engine.Engine;
import de.delautrer.engine.MenuRenderer;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.Scene;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIVBox;
import de.delautrer.game.ui.gui.screens.OptionsScreen;
import de.delautrer.game.ui.gui.screens.WorldSelectScreen;
import org.lwjgl.glfw.GLFW;

public class MainMenuScene extends Scene {

    private MenuRenderer menuRenderer;
    private MenuScreen mainScreen;
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

                // 1. Box erstellen (Spacing: 20px)
                UIVBox menuBox = new UIVBox(0, 0, 20.0f);

                // 2. Buttons einwerfen (X und Y werden von der Box ignoriert und selbst gesetzt!)
                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Play Singleplayer", () -> {
                    WorldSelectScreen worldScreen = new WorldSelectScreen(engine, () -> activeScreen = mainScreen);
                    worldScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
                    worldScreen.setFont(menuRenderer.getFont());
                    activeScreen = worldScreen;
                }));

                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Options", () -> {
                    OptionsScreen optionsScreen = new OptionsScreen(() -> activeScreen = mainScreen);
                    optionsScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
                    optionsScreen.setFont(menuRenderer.getFont());
                    activeScreen = optionsScreen;
                }));

                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Quit Game", () -> {
                    GLFW.glfwSetWindowShouldClose(engine.getWindow().getHandle(), true);
                }));

                // 3. Gesamte Box zentrieren
                menuBox.setPosition((width - menuBox.getWidth()) / 2.0f, (height - menuBox.getHeight()) / 2.0f);
                elements.add(menuBox);
            }
        };

        mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        mainScreen.setFont(menuRenderer.getFont());
        activeScreen = mainScreen;
    }

    @Override
    public void update(float deltaTime) {
        if (activeScreen == null) return;
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        activeScreen.handleMenuInput(input, uiMouseX, uiMouseY);
    }

    @Override
    public void render() {
        if (activeScreen == null) return;
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        menuRenderer.draw(activeScreen, uiMouseX, uiMouseY);
    }

    @Override
    public void onResize() {
        menuRenderer.recreate();
        if (mainScreen != null) mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        if (activeScreen != null && activeScreen != mainScreen) {
            activeScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        }
    }

    @Override
    public void cleanup() {
        if (menuRenderer != null) menuRenderer.cleanup();
    }
}