package de.delautrer.game.states;

import de.delautrer.engine.Engine;
import de.delautrer.engine.MenuRenderer;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.Scene;
import de.delautrer.game.ui.gui.MenuScreen;
import de.delautrer.game.ui.gui.UIButton;
import de.delautrer.game.ui.gui.UIElement;
import org.lwjgl.glfw.GLFW;

public class MainMenuScene extends Scene {

    private MenuRenderer menuRenderer;
    private MenuScreen menuScreen;

    public MainMenuScene(Engine engine) {
        super(engine);
    }

    @Override
    public void init() {
        engine.getWindow().enableCursor();
        menuRenderer = new MenuRenderer(engine.getVulkanContext(), engine.getWindow());

        menuScreen = new MenuScreen() {
            @Override
            protected void onInit() {
                elements.clear();

                float btnWidth = 256;
                float btnHeight = 40;
                float centerX = width / 2.0f - btnWidth / 2.0f;
                float centerY = height / 2.0f;

                // OBEN: Größeres Y (+30) -> Welt betreten
                elements.add(new UIButton(centerX, centerY + 30, btnWidth, btnHeight, "Welt betreten", () -> {
                    engine.getSceneManager().changeScene(new PlayScene(engine));
                }));

                // UNTEN: Kleineres Y (-30) -> Spiel beenden
                elements.add(new UIButton(centerX, centerY - 30, btnWidth, btnHeight, "Spiel beenden", () -> {
                    GLFW.glfwSetWindowShouldClose(engine.getWindow().getHandle(), true);
                }));
            }
        };

        menuScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        menuScreen.setFont(menuRenderer.getFont());
    }

    @Override
    public void update(float deltaTime) {
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        menuScreen.handleMenuInput(input, uiMouseX, uiMouseY);
    }

    @Override
    public void render() {
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        menuRenderer.draw(menuScreen, uiMouseX, uiMouseY);
    }

    @Override
    public void onResize() {
        menuRenderer.recreate();
        menuScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
    }

    @Override
    public void cleanup() {
        if (menuRenderer != null) menuRenderer.cleanup();
    }
}