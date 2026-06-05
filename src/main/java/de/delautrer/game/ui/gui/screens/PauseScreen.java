package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIElement;
import de.delautrer.game.ui.elements.UIVBox;
import de.delautrer.game.ui.UIMeshBuilder;

public class PauseScreen extends MenuScreen {
    private final PlayScene playScene;
    public boolean isSaving = false;
    private MenuScreen subScreen = null;

    public PauseScreen(PlayScene playScene) {
        this.playScene = playScene;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        if (subScreen != null) {
            subScreen.init(width, height);
        }
    }

    @Override
    protected void onInit() {
        elements.clear();

        // 1. Box erstellen (X/Y vorerst egal, Spacing 20)
        UIVBox menuBox = new UIVBox(0, 0, 20.0f);

        float btnWidth = 320;
        float btnHeight = 40;

        // 2. Elemente einfach nacheinander einwerfen! X/Y innerhalb der Buttons sind völlig egal (0,0), die Box regelt das.
        menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Back to the game", () -> {
            playScene.resumeGame();
        }));

        menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Options", () -> {
            OptionsScreen optionsScreen = new OptionsScreen(() -> {
                playScene.getEngine().getInputManager().reloadBindings();
                subScreen = null;
            }, playScene::requestUIRebuild);
            optionsScreen.init(width, height);
            optionsScreen.setFont(font);
            subScreen = optionsScreen;
        }));

        menuBox.addChild(new UIButton(0, 0, btnWidth, btnHeight, "Save & back to main menu", () -> {
            isSaving = true;
            elements.clear();
            playScene.saveAndQuit();
        }));

        // 3. Magie: Die Box hat ihre Größe jetzt selbst berechnet. Wir zentrieren sie exakt in die Mitte!
        menuBox.setPosition(
                (width - menuBox.getWidth()) / 2.0f,
                (height - menuBox.getHeight()) / 2.0f
        );

        elements.add(menuBox);
    }

    @Override
    public void handleMenuInput(InputManager input, float uiMouseX, float uiMouseY) {
        if (subScreen != null) {
            subScreen.handleMenuInput(input, uiMouseX, uiMouseY);
        } else {
            super.handleMenuInput(input, uiMouseX, uiMouseY);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        if (subScreen != null) {
            subScreen.render(builder, mouseX, mouseY);
        } else {
            builder.addAtlasQuad(0, 0, 0, width, height, 15, 15, 1, 1, false);

            if (isSaving) {
                String saveText = "Saving world...";
                float textWidth = builder.getTextWidth(saveText, font);
                builder.drawText(saveText, (width - textWidth) / 2.0f, height / 2.0f, 0, font);
            } else {
                for (UIElement element : elements) {
                    element.render(builder, font, mouseX, mouseY);
                }
            }
        }
    }

    public boolean hasSubScreen() {
        return subScreen != null;
    }

    public void closeSubScreen() {
        if (subScreen instanceof OptionsScreen) {
            de.delautrer.game.settings.SettingsManager.save();
            playScene.getEngine().getInputManager().reloadBindings();
        }
        subScreen = null;
    }
}