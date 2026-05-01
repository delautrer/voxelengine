package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIProgressBar;
import de.delautrer.game.ui.elements.UIVBox;

public class LoadingScreen extends MenuScreen {
    private UIProgressBar progressBar;

    @Override
    protected void onInit() {
        elements.clear();
        float barWidth = 400.0f;
        float barHeight = 40.0f;

        UIVBox menuBox = new UIVBox(0, 0, 0.0f);
        progressBar = new UIProgressBar(0, 0, barWidth, barHeight);

        menuBox.addChild(progressBar);
        menuBox.setPosition((width - menuBox.getWidth()) / 2.0f, (height - menuBox.getHeight()) / 2.0f);

        elements.add(menuBox);
    }

    public void setProgress(float progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.addAtlasQuad(0, 0, 0.0f, width, height, 15, 15, 1, 1, false);
        super.render(builder, mouseX, mouseY);

        if (progressBar != null && font != null) {
            int percent = Math.min(100, (int)(progressBar.getProgress() * 100));
            String loadText = "World is loading... " + percent + "%";
            float textWidth = builder.getTextWidth(loadText, font);
            float textX = (width / 2.0f) - (textWidth / 2.0f);

            // Text leicht über der VBox zeichnen
            builder.drawText(loadText, textX, progressBar.getY() + progressBar.getHeight() + 10.0f, 0.4f, font);
        }
    }
}