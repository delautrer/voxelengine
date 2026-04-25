package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIElement;
import de.delautrer.game.ui.UIMeshBuilder;

public class PauseScreen extends MenuScreen {
    private final PlayScene playScene;
    public boolean isSaving = false;

    public PauseScreen(PlayScene playScene) {
        this.playScene = playScene;
    }

    @Override
    protected void onInit() {
        elements.clear();
        float btnWidth = 320;
        float btnHeight = 40;
        float centerX = (width - btnWidth) / 2.0f;
        float centerY = height / 2.0f;

        // Button 1: Zurück zum Spiel
        elements.add(new UIButton(centerX, centerY + 60, btnWidth, btnHeight, "Back to the game", () -> {
            playScene.resumeGame();
        }));

        // Button 2: Optionen (Platzhalter)
        elements.add(new UIButton(centerX, centerY, btnWidth, btnHeight, "Options", () -> {
            System.out.println("Optionen Menü kommt später!");
        }));

        // Button 3: Speichern & Beenden
        elements.add(new UIButton(centerX, centerY - 60, btnWidth, btnHeight, "Save & back to main menu", () -> {
            isSaving = true;
            elements.clear(); // Buttons ausblenden
            playScene.saveAndQuit();
        }));
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.addAtlasQuad(0, 0, 0, width, height, 15, 15, 1, 1, false);

        if (isSaving) {

            String saveText = "Saving world...";
            float textWidth = builder.getTextWidth(saveText, font);
            float textX = (width / 2.0f) - (textWidth / 2.0f);
            builder.drawText(saveText, textX, height / 2.0f, 0, font);
        } else {
            for (UIElement element : elements) {
                element.render(builder, font, mouseX, mouseY);
            }
        }
    }
}