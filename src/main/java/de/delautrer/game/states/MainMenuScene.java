package de.delautrer.game.states;

import de.delautrer.Constants;
import de.delautrer.engine.Engine;
import de.delautrer.engine.MenuRenderer;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.states.Scene;
import de.delautrer.game.ui.UIMeshBuilder;
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
        menuRenderer = new MenuRenderer(engine.getGraphicsContext(), engine.getWindow());

        mainScreen = new MenuScreen() {
            @Override
            protected void onInit() {
                elements.clear();

                float btnWidth = 280;
                float btnHeight = 44;
                float spacing = 14;

                UIVBox menuBox = new UIVBox(0, 0, spacing);

                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Play Singleplayer", () -> {
                    WorldSelectScreen worldScreen = new WorldSelectScreen(engine, () -> activeScreen = mainScreen);
                    worldScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
                    worldScreen.setFont(menuRenderer.getFont());
                    activeScreen = worldScreen;
                }));

                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Options", () -> {
                    OptionsScreen optionsScreen = new OptionsScreen(() -> {
                        engine.getInputManager().reloadBindings();
                        activeScreen = mainScreen;
                    }, null);
                    optionsScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
                    optionsScreen.setFont(menuRenderer.getFont());
                    activeScreen = optionsScreen;
                }));

                menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Quit Game", () -> {
                    GLFW.glfwSetWindowShouldClose(engine.getWindow().getHandle(), true);
                }));

                // Center buttons in lower half of screen
                float centerX = (width - menuBox.getWidth()) / 2.0f;
                float centerY = height * 0.52f - menuBox.getHeight() / 2.0f;
                menuBox.setPosition(centerX, centerY);
                elements.add(menuBox);
            }

            @Override
            public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
                // Responsive centering
                if (!elements.isEmpty() && elements.get(0) instanceof UIVBox) {
                    UIVBox menuBox = (UIVBox) elements.get(0);
                    float centerX = (width - menuBox.getWidth()) / 2.0f;
                    float centerY = height * 0.45f - menuBox.getHeight() / 2.0f;
                    menuBox.setPosition(centerX, centerY);
                }

                // ── Background ──────────────────────────────────────────
                // Minimalist dark slate
                builder.addRect(0, 0, 0.0f, width, height, 0.08f, 0.08f, 0.09f, 1.0f);

                // Subtle modern accent lines (synth-wave minimalist grid)
                float gridSpacing = 40.0f;
                int lines = (int)(height / gridSpacing);
                for (int i = 0; i < lines; i++) {
                    float yPos = i * gridSpacing;
                    float intensity = 0.1f + 0.05f * (yPos / height);
                    builder.addRect(0, yPos, 0.01f, width, 1.0f, intensity, intensity, intensity + 0.02f, 1.0f);
                }

                // Horizontal accent strip
                float lineY = height * 0.65f;
                builder.addRect(0, lineY, 0.02f, width, 2.0f, 0.4f, 0.2f, 0.8f, 1.0f);
                builder.addRect(0, lineY + 2.0f, 0.02f, width, 1.0f, 0.2f, 0.1f, 0.4f, 1.0f);

                // ── Title ────────────────────────────────────────────────
                if (font != null) {
                    String title = "VEINSTRIDE";
                    float titleW = builder.getTextWidth(title, font);
                    float titleX = (width - titleW) / 2.0f;
                    float titleY = height * 0.75f;
                    
                    // Single, clean title text
                    builder.drawText(title, titleX, titleY, 0.06f, font);
                }

                // ── Buttons ──────────────────────────────────────────────
                super.render(builder, mouseX, mouseY);

                // ── Footer ───────────────────────────────────────────────
                if (font != null) {
                    String version = "v" + Constants.VERSION;
                    String credits = "made with <3 by delautrer";

                    float versionW = builder.getTextWidth(version, font);
                    float creditsW = builder.getTextWidth(credits, font);

                    float margin = 16f;
                    float footerY = 28f;

                    // Version — bottom left
                    builder.drawText(version, margin, footerY, 0.08f, font);
                    // Credits — bottom left below version
                    builder.drawText(credits, margin, footerY + 22f, 0.08f, font);
                }
            }
        };

        mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        mainScreen.setFont(menuRenderer.getFont());
        activeScreen = mainScreen;
    }

    @Override
    public void update(float deltaTime) {
        if (activeScreen == null)
            return;
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        activeScreen.handleMenuInput(input, uiMouseX, uiMouseY);
    }

    @Override
    public void render() {
        if (activeScreen == null)
            return;
        InputManager input = engine.getInputManager();
        float uiMouseX = input.getMouseX();
        float uiMouseY = engine.getWindow().getHeight() - input.getMouseY();
        menuRenderer.draw(activeScreen, uiMouseX, uiMouseY);
    }

    @Override
    public void onResize() {
        menuRenderer.recreate();
        if (mainScreen != null)
            mainScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        if (activeScreen != null && activeScreen != mainScreen) {
            activeScreen.init(engine.getWindow().getWidth(), engine.getWindow().getHeight());
        }
    }

    @Override
    public void cleanup() {
        if (menuRenderer != null)
            menuRenderer.cleanup();
    }
}
